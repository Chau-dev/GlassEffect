package com.chau.glasseffect

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
    
    private var currentW = 4.2f
    private var currentH = 1.2f
    private var currentD = 1.2f
    private var currentR = 0.6f
    
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
        
        // 4. UI Button to pick background
        val pickImageButton = Button(this).apply {
            text = "Update Environment"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(0, 0, 0, 100)
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

        // 5. Real-time Control Panel for UI/UX Prototyping
        val controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(180, 0, 0, 0))
            setPadding(40, 40, 40, 40)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
            }
        }

        loadSettings()
        updateMesh()

        fun createSlider(label: String, min: Float, max: Float, initial: Float, onUpdate: (Float) -> Unit) {
            val container = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val labelView = TextView(this).apply {
                text = "$label: "
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(250, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val seekBar = SeekBar(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val steps = 100
                progress = (((initial - min) / (max - min)) * steps).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val value = min + (p.toFloat() / steps) * (max - min)
                        onUpdate(value)
                        updateMesh()
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

        createSlider("Length", 1.0f, 8.0f, currentW) { currentW = it }
        createSlider("Padding", 0.2f, 4.0f, currentH) { currentH = it }
        createSlider("Thickness", 0.1f, 3.0f, currentD) { currentD = it }
        createSlider("Rounding", 0.0f, 1.5f, currentR) { currentR = it }

        rootLayout.addView(controlPanel)
        
        setContentView(rootLayout)
        
        // Restore environment image if saved
        restoreSavedImage()
    }

    private fun updateMesh() {
        glSurfaceView.setMeshDimensions(currentW, currentH, currentD, currentR)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("glass_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("width", currentW)
            putFloat("height", currentH)
            putFloat("depth", currentD)
            putFloat("radius", currentR)
            apply()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("glass_prefs", MODE_PRIVATE)
        currentW = prefs.getFloat("width", 4.2f)
        currentH = prefs.getFloat("height", 1.2f)
        currentD = prefs.getFloat("depth", 1.2f)
        currentR = prefs.getFloat("radius", 0.6f)
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
