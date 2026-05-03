package com.devas.glasseffect.engine

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.devas.glasseffect.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlassRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private lateinit var glassShader: Shader
    private lateinit var sphere: Mesh
    private lateinit var camera: Camera
    
    private var backgroundTextureId: Int = -1
    private var pendingBitmap: Bitmap? = null
    
    // User-controlled offsets
    var offsetX = 0f
    var offsetY = 0f
    var offsetZ = 0f
    
    // Gravity vector for liquid sloshing
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    
    private val modelMatrix = FloatArray(16)
    private var width: Int = 1
    private var height: Int = 1
    private var time: Float = 0f

    private val material = GlassMaterial(
        ior = 1.65f,       
        dispersion = 0.04f, 
        absorption = 0.15f, 
        tint = floatArrayOf(0.9f, 0.95f, 1.0f)
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        glassShader = Shader(context, R.raw.glass_vertex, R.raw.glass_fragment)
        sphere = Mesh.createSphere(1.2f, 96, 92) 
        camera = Camera(position = floatArrayOf(0f, 0f, 5f))
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        camera.aspect = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        time += 0.016f

        if (pendingBitmap != null) {
            if (backgroundTextureId != -1) {
                GLES30.glDeleteTextures(1, intArrayOf(backgroundTextureId), 0)
            }
            backgroundTextureId = loadTexture(pendingBitmap!!)
            pendingBitmap = null
        }

        glassShader.use()
        glassShader.setMat4("view", camera.getViewMatrix())
        glassShader.setMat4("projection", camera.getProjectionMatrix())
        glassShader.setVec2("uResolution", width.toFloat(), height.toFloat())
        glassShader.setFloat("uTime", time)
        glassShader.setVec3("uGravity", -gravityX * 0.1f, -gravityY * 0.1f, gravityZ * 0.1f)
        
        if (backgroundTextureId != -1) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, backgroundTextureId)
            glassShader.setInt("uBackground", 0)
        }

        material.applyToShader(glassShader)

        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        android.opengl.Matrix.translateM(modelMatrix, 0, offsetX, offsetY, offsetZ)
        // Gentle rotation + inertia
        android.opengl.Matrix.rotateM(modelMatrix, 0, time * 10f, gravityY, -gravityX, 1f)
        glassShader.setMat4("model", modelMatrix)

        sphere.draw()
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        pendingBitmap = bitmap
    }

    fun setGravity(x: Float, y: Float, z: Float) {
        gravityX = x
        gravityY = y
        gravityZ = z
    }

    private fun loadTexture(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        return textures[0]
    }
}
