package com.chau.glasseffect

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/**
 * MainActivity: A Refractive Input Manifold.
 * 
 * As a mathematician and physicist, we don't just "show" a text field; we embed a 
 * standard Android EditText over a 3D refractive substrate (the Bevelled Slab).
 * Interaction is primarily touch-based, ensuring the refractive boundaries
 * respond directly to user gestures.
 */
class MainActivity : Activity() {
    private lateinit var glSurfaceView: GlassSurfaceView
    private lateinit var backgroundImageView: ImageView
    private lateinit var adjustableButtonLayout: FrameLayout
    private lateinit var adjustableButton: Button
    
    private var currentW = 4.2f
    private var currentH = 1.2f
    private var currentD = 1.2f
    private var currentR = 0.6f
    private var currentButtonWidthDp = 220f
    private var currentButtonHeightDp = 64f
    private var currentButtonRadiusDp = 28f
    private var currentButtonXOffsetDp = 0f
    private var currentButtonYOffsetDp = -150f
    private var currentButtonLabel = "Glass Button"
    
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Physicist's Setup: Full transparency and high-fidelity depth
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setFormat(PixelFormat.TRANSLUCENT)
        
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // 1. Background Image View (The Environment)
        backgroundImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        rootLayout.addView(backgroundImageView)
        
        // 2. 3D Engine Surface View (Rendering the Bevelled Slab substrate)
        glSurfaceView = GlassSurfaceView(this)
        rootLayout.addView(glSurfaceView)

        loadSettings()
        updateMesh()
        
        // 3. The Refractive TextField Overlay
        // We overlay a transparent EditText. The 3D Bevelled Slab Mesh provides the "Material" depth.
        val glassTextField = EditText(this).apply {
            hint = "Tap to type in glass..."
            setHintTextColor(Color.argb(150, 255, 255, 255))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT) // Transparent so we see the 3D Slab underneath
            textSize = 20f
            gravity = Gravity.CENTER
            // The Slab in GlassRenderer is ~3.2 units wide, which maps to the center visual area
            layoutParams = FrameLayout.LayoutParams(850, 180).apply {
                gravity = Gravity.CENTER
            }
        }
        rootLayout.addView(glassTextField)

        // 4. Adjustable button layout. The button text, shape, and location can be tuned live.
        adjustableButton = Button(this).apply {
            text = currentButtonLabel
            setTextColor(Color.WHITE)
            textSize = 16f
            isAllCaps = false
            minWidth = 0
            minHeight = 0
            minimumWidth = 0
            minimumHeight = 0
            includeFontPadding = false
            gravity = Gravity.CENTER
            setPadding(dp(16f), 0, dp(16f), 0)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        adjustableButtonLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp(currentButtonWidthDp),
                dp(currentButtonHeightDp)
            ).apply {
                gravity = Gravity.CENTER
            }
            elevation = 16f
            addView(adjustableButton)
        }
        installButtonDrag()
        updateButtonLayout()
        rootLayout.addView(adjustableButtonLayout)

        // 5. Navigation Demo: Open Second Activity
        val navButton = GlassButton(this).apply {
            button.text = "Open Second Screen"
            layoutParams = FrameLayout.LayoutParams(600, 150).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 100
            }
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SecondActivity::class.java))
            }
        }
        rootLayout.addView(navButton)

        val controlPanelHeight = dp(360f).coerceAtMost(
            (resources.displayMetrics.heightPixels * 0.55f).toInt()
        )
        
        // 5. UI Button to pick background
        val pickImageButton = Button(this).apply {
            text = "Update Environment"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(0, 0, 0, controlPanelHeight + dp(12f))
            }
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                startActivityForResult(intent, PICK_IMAGE_REQUEST)
            }
        }
        rootLayout.addView(pickImageButton)

        // 6. Real-time Control Panel for UI/UX Prototyping
        val controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(180, 0, 0, 0))
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            layoutParams = ViewGroup.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        fun createSlider(
            label: String,
            min: Float,
            max: Float,
            initial: Float,
            afterUpdate: () -> Unit = { updateMesh() },
            onUpdate: (Float) -> Unit
        ) {
            val container = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val labelView = TextView(this).apply {
                text = "$label: "
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dp(132f), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val seekBar = SeekBar(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val steps = 100
                this.max = steps
                progress = (((initial - min) / (max - min)) * steps).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val value = min + (p.toFloat() / steps) * (max - min)
                        onUpdate(value)
                        afterUpdate()
                        saveSettings()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            container.addView(labelView)
            container.addView(seekBar)
            controlPanel.addView(container)
        }

        fun createTextInput(label: String, initial: String, onUpdate: (String) -> Unit) {
            val container = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val labelView = TextView(this).apply {
                text = "$label: "
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dp(132f), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val editText = EditText(this).apply {
                setText(initial)
                setSingleLine(true)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.argb(150, 255, 255, 255))
                setBackgroundColor(Color.argb(60, 255, 255, 255))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        onUpdate(s?.toString().orEmpty())
                        updateButtonLayout()
                        saveSettings()
                    }
                })
            }
            container.addView(labelView)
            container.addView(editText)
            controlPanel.addView(container)
        }

        createSlider("Lens Width", 0.05f, 8.0f, currentW) { currentW = it }
        createSlider("Lens Height", 0.05f, 4.0f, currentH) { currentH = it }
        createSlider("Lens Radius", 0.0f, 1.5f, currentR) { 
            currentR = it 
            currentD = it * 2f // Maintain perfect hemispherical depth (Thickness = 2 * Radius)
        }
        createSlider("Lens Thickness", 0.1f, 3.0f, currentD) { 
            currentD = it 
            currentR = it / 2f // Sync back to radius
        }
        createTextInput("Button Label", currentButtonLabel) { currentButtonLabel = it }
        createSlider("Button Width", 120f, 360f, currentButtonWidthDp, afterUpdate = ::updateButtonLayout) {
            currentButtonWidthDp = it
        }
        createSlider("Button Height", 44f, 120f, currentButtonHeightDp, afterUpdate = ::updateButtonLayout) {
            currentButtonHeightDp = it
        }
        createSlider("Button Radius", 0f, 60f, currentButtonRadiusDp, afterUpdate = ::updateButtonLayout) {
            currentButtonRadiusDp = it
        }
        createSlider("Button X", -180f, 180f, currentButtonXOffsetDp, afterUpdate = ::updateButtonLayout) {
            currentButtonXOffsetDp = it
        }
        createSlider("Button Y", -260f, 180f, currentButtonYOffsetDp, afterUpdate = ::updateButtonLayout) {
            currentButtonYOffsetDp = it
        }
        createSlider("Lens X", -5.0f, 5.0f, glSurfaceView.renderer.offsetX) {
            glSurfaceView.renderer.offsetX = it
        }
        createSlider("Lens Y", -5.0f, 5.0f, glSurfaceView.renderer.offsetY) {
            glSurfaceView.renderer.offsetY = it
        }

        val controlPanelScroller = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                controlPanelHeight
            ).apply {
                gravity = Gravity.BOTTOM
            }
            addView(controlPanel)
        }
        rootLayout.addView(controlPanelScroller)
        
        setContentView(rootLayout)
        
        // Restore environment image if saved
        restoreSavedImage()
    }

    private fun updateMesh() {
        glSurfaceView.setMeshDimensions(currentW, currentH, currentD, currentR)
    }

    private fun updateButtonLayout() {
        val layoutParams = adjustableButtonLayout.layoutParams as FrameLayout.LayoutParams
        layoutParams.width = dp(currentButtonWidthDp)
        layoutParams.height = dp(currentButtonHeightDp)
        layoutParams.gravity = Gravity.CENTER
        adjustableButtonLayout.layoutParams = layoutParams
        adjustableButtonLayout.translationX = dpFloat(currentButtonXOffsetDp)
        adjustableButtonLayout.translationY = dpFloat(currentButtonYOffsetDp)

        val safeRadius = currentButtonRadiusDp.coerceAtMost(currentButtonHeightDp / 2f)
        adjustableButton.text = currentButtonLabel.ifBlank { "Button" }
        adjustableButton.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpFloat(safeRadius)
            setColor(Color.argb(46, 255, 255, 255))
            setStroke(dp(1f), Color.argb(150, 255, 255, 255))
        }
    }

    private fun installButtonDrag() {
        var lastRawX = 0f
        var lastRawY = 0f
        var isDragging = false

        val dragListener = android.view.View.OnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - lastRawX
                    val deltaY = event.rawY - lastRawY
                    if (kotlin.math.abs(deltaX) > 1f || kotlin.math.abs(deltaY) > 1f) {
                        isDragging = true
                    }
                    currentButtonXOffsetDp += pxToDp(deltaX)
                    currentButtonYOffsetDp += pxToDp(deltaY)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    updateButtonLayout()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        view.performClick()
                    }
                    saveSettings()
                    true
                }
                else -> false
            }
        }

        adjustableButtonLayout.setOnTouchListener(dragListener)
        adjustableButton.setOnTouchListener(dragListener)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("glass_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("width", currentW)
            putFloat("height", currentH)
            putFloat("depth", currentD)
            putFloat("radius", currentR)
            putFloat("button_width_dp", currentButtonWidthDp)
            putFloat("button_height_dp", currentButtonHeightDp)
            putFloat("button_radius_dp", currentButtonRadiusDp)
            putFloat("button_x_offset_dp", currentButtonXOffsetDp)
            putFloat("button_y_offset_dp", currentButtonYOffsetDp)
            putString("button_label", currentButtonLabel)
            apply()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("glass_prefs", MODE_PRIVATE)
        currentW = prefs.getFloat("width", 4.2f)
        currentH = prefs.getFloat("height", 1.2f)
        currentD = prefs.getFloat("depth", 1.2f)
        currentR = prefs.getFloat("radius", 0.6f)
        currentButtonWidthDp = prefs.getFloat("button_width_dp", 220f)
        currentButtonHeightDp = prefs.getFloat("button_height_dp", 64f)
        currentButtonRadiusDp = prefs.getFloat("button_radius_dp", 28f)
        currentButtonXOffsetDp = prefs.getFloat("button_x_offset_dp", 0f)
        currentButtonYOffsetDp = prefs.getFloat("button_y_offset_dp", -150f)
        currentButtonLabel = prefs.getString("button_label", "Glass Button") ?: "Glass Button"
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun dpFloat(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun pxToDp(value: Float): Float {
        return value / resources.displayMetrics.density
    }

    private fun restoreSavedImage() {
        val prefs = getSharedPreferences("glass_prefs", MODE_PRIVATE)
        val uriString = prefs.getString("bg_uri", null)
        if (uriString != null) {
            val uri = android.net.Uri.parse(uriString)
            loadBackgroundFromUri(uri)
        }
    }

    private fun loadBackgroundFromUri(uri: android.net.Uri) {
        backgroundImageView.setImageURI(uri)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let {
                    glSurfaceView.renderer.setBackgroundImage(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                // Take persistent permission so it works after reboot
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { /* Might already have it or not supported */ }

                getSharedPreferences("glass_prefs", MODE_PRIVATE).edit()
                    .putString("bg_uri", uri.toString())
                    .apply()

                loadBackgroundFromUri(uri)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}
