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
        scaleDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress) return true

        val x: Float = event.x
        val y: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx: Float = x - previousX
                val dy: Float = y - previousY

                // 1. Translational Displacement: Moving the 'Lens' across the UI plane
                val translationSensitivity = 0.01f
                renderer.offsetX += dx * translationSensitivity
                renderer.offsetY -= dy * translationSensitivity

                // 2. Angular Displacement (Tilt): 
                // In Material Science, tilting the shape shifts the surface Normals (N).
                // This activates the Fresnel effect and shifts the refraction vectors,
                // making the "light passing through" feel physically substantial.
                rotationY += dx * 0.15f
                rotationX += dy * 0.15f
                renderer.setRotation(rotationX, rotationY)
            }
        }

        previousX = x
        previousY = y
        return true
    }
}
