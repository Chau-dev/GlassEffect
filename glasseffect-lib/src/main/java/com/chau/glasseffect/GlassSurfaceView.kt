package com.chau.glasseffect

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.chau.glasseffect.engine.GlassRenderer

/**
 * A Mathematician's Perspective: This view is a coordinate transformer for light rays.
 * A Physicist's Perspective: This is a refractive boundary layer with dynamic Normals.
 * A Coder's Perspective: A high-performance GLSurfaceView with physics-mapped gestures.
 */
class GlassSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer: GlassRenderer = GlassRenderer(context)
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    // We introduce Angular state (rotation) because in Material Science, 
    // the "Angle of Incidence" is the key to realistic refraction and Fresnel effects.
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    
    var onMoveListener: ((Float, Float) -> Unit)? = null

    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            performClick()
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Adjust the Z-depth (refractive zoom) based on pinch
            // In a Ramanujan-like view, Z changes the focal manifold's intersection with the projection plane.
            renderer.offsetZ += (detector.scaleFactor - 1.0f) * 5.0f
            renderer.offsetZ = renderer.offsetZ.coerceIn(-15f, 2f)
            return true
        }
    })

    init {
        // Physicist's Requirement: We need GLES 3.0 for advanced refractive shaders
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val configInfo = activityManager.deviceConfigurationInfo
        if (configInfo.reqGlEsVersion < 0x30000) {
            android.util.Log.e("GlassSurfaceView", "GLES 3.0 not supported on this device. Glass engine will not start.")
        }

        setEGLContextClientVersion(3)
        // Ensure the config supports transparency (Alpha channel) for UI overlay
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
    }

    fun setMeshDimensions(width: Float, height: Float, depth: Float, radius: Float) {
        renderer.updateMeshDimensions(width, height, depth, radius)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress) return true

        val x: Float = event.x
        val y: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx: Float = x - previousX
                val dy: Float = y - previousY

                // 2D Translational Displacement: Moving the 'Lens' substrate
                // independently of the container in the XY plane.
                val translationSensitivity = 0.01f
                renderer.offsetX += dx * translationSensitivity
                renderer.offsetY -= dy * translationSensitivity
                
                onMoveListener?.invoke(renderer.offsetX, renderer.offsetY)
            }
        }

        previousX = x
        previousY = y
        return true
    }
}
