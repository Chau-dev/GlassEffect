package com.chau.glasseffect

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.FrameLayout
import android.view.ViewGroup
import com.chau.glasseffect.engine.GlassRenderer

/**
 * GlassLayout: The Primary UI/UX Library Component.
 * 
 * This container adds a 3D refractive glass substrate behind its children.
 * It encapsulates the OpenGL ES 3.0 engine, allowing any developer to 
 * implement high-fidelity glassmorphism with a single XML tag.
 */
open class GlassLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val glassSurfaceView = GlassSurfaceView(context)
    val renderer: GlassRenderer get() = glassSurfaceView.renderer
    
    private var isGles3Supported = true
    private var fallbackView: android.view.View? = null

    /**
     * Programmatic control for automatic background capture.
     * When true, the layout will automatically capture the background on the next layout pass.
     */
    var autoEnvironment: Boolean = false
        set(value) {
            field = value
            if (value && isAttachedToWindow) {
                post { captureEnvironment() }
            }
        }

    private var explicitWidth: Float? = null
    private var explicitHeight: Float? = null
    private var explicitRadius: Float? = null
    private var explicitThickness: Float? = null

    init {
        // 1. Check for GLES 3.0 Support
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        isGles3Supported = activityManager.deviceConfigurationInfo.reqGlEsVersion >= 0x30000

        if (isGles3Supported) {
            // 2. Add the Glass Engine as the bottom-most layer
            addView(glassSurfaceView, 0, LayoutParams(
                LayoutParams.MATCH_PARENT, 
                LayoutParams.MATCH_PARENT
            ))
        } else {
            // 3. Fallback: Simple Frosted Glass Aesthetic (Non-refractive)
            setupFallbackUI()
        }

        // 4. Load XML Attributes for Design Customization
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GlassLayout)
            
            val ior = a.getFloat(R.styleable.GlassLayout_glass_ior, 1.65f)
            val dispersion = a.getFloat(R.styleable.GlassLayout_glass_dispersion, 0.08f)
            val thickness = a.getFloat(R.styleable.GlassLayout_glass_thickness, 1.2f)
            val radius = a.getFloat(R.styleable.GlassLayout_glass_radius, 0.6f)
            val tiltX = a.getFloat(R.styleable.GlassLayout_glass_tilt_x, 0f)
            val tiltY = a.getFloat(R.styleable.GlassLayout_glass_tilt_y, 0f)
            
            if (a.hasValue(R.styleable.GlassLayout_glass_width)) 
                explicitWidth = a.getFloat(R.styleable.GlassLayout_glass_width, 4.2f)
            if (a.hasValue(R.styleable.GlassLayout_glass_height)) 
                explicitHeight = a.getFloat(R.styleable.GlassLayout_glass_height, 1.2f)
            if (a.hasValue(R.styleable.GlassLayout_glass_radius))
                explicitRadius = radius
            if (a.hasValue(R.styleable.GlassLayout_glass_thickness))
                explicitThickness = thickness

            val offX = a.getFloat(R.styleable.GlassLayout_glass_offset_x, 0f)
            val offY = a.getFloat(R.styleable.GlassLayout_glass_offset_y, 0f)

            // Apply to renderer
            glassSurfaceView.renderer.apply {
                offsetX = offX
                offsetY = offY
                updateMeshDimensions(
                    explicitWidth ?: 4.2f, 
                    explicitHeight ?: 1.2f, 
                    thickness, 
                    radius
                )
                setRotation(tiltX, tiltY) 
                material.ior = ior
                material.dispersion = dispersion
            }

            // Sync children (text, icons, etc) with lens movement
            glassSurfaceView.onMoveListener = { ox, oy ->
                syncChildrenTranslation(ox, oy)
            }
            
            // Forward clicks from the glass surface to the layout
            glassSurfaceView.setOnClickListener { this.performClick() }

            autoEnvironment = a.getBoolean(R.styleable.GlassLayout_glass_auto_environment, false)

            a.recycle()
        }
    }

    private fun setupFallbackUI() {
        val fallback = android.view.View(context).apply {
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(android.graphics.Color.argb(40, 255, 255, 255))
                setStroke(2, android.graphics.Color.argb(100, 255, 255, 255))
                cornerRadius = 30f
            }
            background = drawable
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        fallbackView = fallback
        addView(fallback, 0)
    }

    private fun syncChildrenTranslation(ox: Float, oy: Float) {
        // Mapping engine units back to pixels for UI alignment
        // The visible height in the engine is 4.0 units.
        val viewHeight = height.toFloat()
        if (viewHeight <= 0) return
        
        val pxX = (ox / 4.0f) * viewHeight
        val pxY = -(oy / 4.0f) * viewHeight

        // Translate all children except the GlassSurfaceView (index 0)
        for (i in 1 until childCount) {
            val child = getChildAt(i)
            child.translationX = pxX
            child.translationY = pxY
        }
    }

    /**
     * Set the image that will be refracted/reflected by the glass.
     * In a library context, this is usually a screenshot of the app's background.
     */
    fun setEnvironment(bitmap: Bitmap) {
        glassSurfaceView.renderer.setBackgroundImage(bitmap)
    }

    /**
     * Automatically capture the background of the parent view to use as the refractive environment.
     */
    fun captureEnvironment() {
        val parent = parent as? ViewGroup ?: return
        
        // Temporarily hide the glass to see what's behind it
        val wasVisible = isVisible()
        visibility = INVISIBLE
        
        parent.post {
            try {
                val bitmap = Bitmap.createBitmap(parent.width, parent.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                parent.draw(canvas)
                setEnvironment(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (wasVisible) visibility = VISIBLE
        }
    }

    private fun isVisible(): Boolean = visibility == VISIBLE

    /**
     * Programmatically update dimensions
     */
    fun setGlassDimensions(width: Float, height: Float, thickness: Float, radius: Float) {
        explicitWidth = width
        explicitHeight = height
        explicitThickness = thickness
        explicitRadius = radius
        glassSurfaceView.setMeshDimensions(width, height, thickness, radius)
    }

    /**
     * Programmatically update lens position
     */
    fun setLensOffset(x: Float, y: Float) {
        glassSurfaceView.renderer.offsetX = x
        glassSurfaceView.renderer.offsetY = y
        syncChildrenTranslation(x, y)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoEnvironment) {
            post { captureEnvironment() }
        }
        // Observe parent scroll for 'live' refraction updates
        parent?.let {
            if (it is android.view.View) {
                it.viewTreeObserver.addOnScrollChangedListener(scrollListener)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        parent?.let {
            if (it is android.view.View) {
                it.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
            }
        }
    }

    private val scrollListener = android.view.ViewTreeObserver.OnScrollChangedListener {
        if (autoEnvironment) {
            captureEnvironment()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // Mapping pixels to 3D units:
            // We want the mesh to fill the layout by default.
            val scaleFactor = 4.0f
            val aspectRatio = w.toFloat() / h.toFloat()
            
            val autoHeight = scaleFactor
            val autoWidth = autoHeight * aspectRatio
            
            val meshHeight = explicitHeight ?: autoHeight
            val meshWidth = explicitWidth ?: autoWidth
            
            val thickness = explicitThickness ?: 1.2f 
            val radius = explicitRadius ?: (meshHeight * 0.4f).coerceAtMost(meshHeight / 2.2f)
            
            glassSurfaceView.setMeshDimensions(meshWidth, meshHeight, thickness, radius)
            
            // Apply initial or current translation to children
            syncChildrenTranslation(glassSurfaceView.renderer.offsetX, glassSurfaceView.renderer.offsetY)
        }
    }
}
