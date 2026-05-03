package com.devas.glasseffect

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.devas.glasseffect.engine.GlassRenderer

class GlassSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer: GlassRenderer = GlassRenderer(context)
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Adjust the Z-depth (refractive zoom) based on pinch
            renderer.offsetZ += (detector.scaleFactor - 1.0f) * 5.0f
            // Clamp Z to reasonable limits
            renderer.offsetZ = renderer.offsetZ.coerceIn(-10f, 2f)
            return true
        }
    })

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Pass event to scale detector
        scaleDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress) return true

        val x: Float = event.x
        val y: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx: Float = x - previousX
                val dy: Float = y - previousY

                // Update position with sensitivity adjustment
                val sensitivity = 0.01f
                renderer.offsetX += dx * sensitivity
                renderer.offsetY -= dy * sensitivity
            }
        }

        previousX = x
        previousY = y
        return true
    }
}
