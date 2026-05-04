package com.chau.glasseffect.engine

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

class Camera(
    var position: FloatArray = floatArrayOf(0f, 0f, 3f),
    var target: FloatArray = floatArrayOf(0f, 0f, 0f),
    var up: FloatArray = floatArrayOf(0f, 1f, 0f)
) {
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    
    var fov = 45f
    var aspect = 1f
    var near = 0.1f
    var far = 100f
    
    fun getViewMatrix(): FloatArray {
        Matrix.setLookAtM(viewMatrix, 0,
            position[0], position[1], position[2],
            target[0], target[1], target[2],
            up[0], up[1], up[2])
        return viewMatrix
    }
    
    fun getProjectionMatrix(): FloatArray {
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, near, far)
        return projectionMatrix
    }
    
    fun getVPMatrix(): FloatArray {
        Matrix.multiplyMM(vpMatrix, 0, getProjectionMatrix(), 0, getViewMatrix(), 0)
        return vpMatrix
    }
    
    fun orbit(time: Float, radius: Float = 3f, height: Float = 1f) {
        position[0] = sin(time) * radius
        position[2] = cos(time) * radius
        position[1] = height
    }
}
