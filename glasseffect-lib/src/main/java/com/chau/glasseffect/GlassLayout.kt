package com.chau.glasseffect

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.FrameLayout
import com.chau.glasseffect.engine.GlassRenderer

/**
 * GlassLayout: The Primary UI/UX Library Component.
 * 
 * This container adds a 3D refractive glass substrate behind its children.
 * It encapsulates the OpenGL ES 3.0 engine, allowing any developer to 
 * implement high-fidelity glassmorphism with a single XML tag.
 */
class GlassLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val glassSurfaceView = GlassSurfaceView(context)
    
    init {
        // 1. Add the Glass Engine as the bottom-most layer
        addView(glassSurfaceView, 0, LayoutParams(
            LayoutParams.MATCH_PARENT, 
            LayoutParams.MATCH_PARENT
        ))

        // 2. Load XML Attributes for Design Customization
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GlassLayout)
            
            val ior = a.getFloat(R.styleable.GlassLayout_glass_ior, 1.65f)
            val dispersion = a.getFloat(R.styleable.GlassLayout_glass_dispersion, 0.08f)
            val thickness = a.getFloat(R.styleable.GlassLayout_glass_thickness, 1.2f)
            val radius = a.getFloat(R.styleable.GlassLayout_glass_radius, 0.6f)
            val tiltX = a.getFloat(R.styleable.GlassLayout_glass_tilt_x, 35f)
            val tiltY = a.getFloat(R.styleable.GlassLayout_glass_tilt_y, 0f)

            // Apply to renderer
            glassSurfaceView.renderer.apply {
                updateMeshDimensions(4.2f, 1.2f, thickness, radius) // Default width/height
                setRotation(tiltX - 35f, tiltY) // Offset from base 35
                material.ior = ior
                material.dispersion = dispersion
            }

            a.recycle()
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
     * Programmatically update dimensions
     */
    fun setGlassDimensions(width: Float, height: Float, thickness: Float, radius: Float) {
        glassSurfaceView.setMeshDimensions(width, height, thickness, radius)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // Mapping pixels to 3D units:
            // We want the mesh to fill the layout. At Camera Z=5 and FOV=45, 
            // the visible 3D height is approximately 4.0 units.
            val scaleFactor = 4.0f
            val aspectRatio = w.toFloat() / h.toFloat()
            
            val meshHeight = scaleFactor
            val meshWidth = meshHeight * aspectRatio
            
            // Adjust thickness and radius proportionally or keep fixed
            val thickness = 1.2f 
            val radius = (meshHeight * 0.4f).coerceAtMost(meshHeight / 2.2f)
            
            glassSurfaceView.setMeshDimensions(meshWidth, meshHeight, thickness, radius)
        }
    }
}
