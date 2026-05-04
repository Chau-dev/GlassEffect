package com.chau.glasseffect.engine

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.chau.glasseffect.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlassRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private lateinit var glassShader: Shader
    private lateinit var pillMesh: Mesh
    private lateinit var camera: Camera
    
    private var backgroundTextureId: Int = -1
    private var pendingBitmap: Bitmap? = null
    
    var offsetX = 0f
    var offsetY = 0f
    var offsetZ = -5.5f 
    
    // Aesthetic perspective tilt: 35 degrees to see the hemispherical ends clearly
    private var rotationX = 35f 
    private var rotationY = 0f
    
    private val modelMatrix = FloatArray(16)
    private var width: Int = 1
    private var height: Int = 1
    private var time: Float = 0f

    // Physics parameters for dynamic manifold reconstruction
    private var pendingMeshUpdate = false
    private var nextWidth = 4.2f
    private var nextHeight = 1.2f
    private var nextDepth = 1.2f
    private var nextRadius = 0.6f

    // Material Science: Extremely high dispersion to visualize spectral splitting at rounded ends
    val material = GlassMaterial(
        ior = 1.65f,        // Heavy Flint Glass for high refraction
        dispersion = 0.08f, // Ultra-high dispersion for vivid rainbow edges
        absorption = 0.01f, 
        tint = floatArrayOf(1.0f, 1.0f, 1.0f) // Pure clear glass
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        glassShader = Shader(context, R.raw.glass_vertex, R.raw.glass_fragment)
        
        // Creating the Hemispherical 'Pill' Slab
        // width=4.0, height=1.2, depth=1.2. 
        // radius=0.6 (Exactly height/2 and depth/2) makes it perfectly round at the ends.
        pillMesh = Mesh.createBevelledSlab(
            width = 4.2f, 
            height = 1.2f, 
            depth = 1.2f, 
            radius = 0.6f, 
            segments = 64 // Dense mesh for liquid-smooth spherical refraction
        )
        
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

        // Handle dynamic manifold reconstruction (Resizing UI components)
        if (pendingMeshUpdate) {
            pillMesh.destroy()
            pillMesh = Mesh.createBevelledSlab(
                width = nextWidth,
                height = nextHeight,
                depth = nextDepth,
                radius = nextRadius,
                segments = 64
            )
            pendingMeshUpdate = false
        }

        if (pendingBitmap != null) {
            if (backgroundTextureId != -1) GLES30.glDeleteTextures(1, intArrayOf(backgroundTextureId), 0)
            backgroundTextureId = loadTexture(pendingBitmap!!)
            pendingBitmap = null
        }

        glassShader.use()
        glassShader.setMat4("view", camera.getViewMatrix())
        glassShader.setMat4("projection", camera.getProjectionMatrix())
        glassShader.setVec2("uResolution", width.toFloat(), height.toFloat())
        glassShader.setFloat("uTime", time)
        glassShader.setVec3("cameraPos", 0f, 0f, 5f)
        
        if (backgroundTextureId != -1) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, backgroundTextureId)
            glassShader.setInt("uBackground", 0)
        }

        material.applyToShader(glassShader)

        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        android.opengl.Matrix.translateM(modelMatrix, 0, offsetX, offsetY, offsetZ)
        
        // Apply fixed rotation to reveal hemispherical depth
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)
        
        glassShader.setMat4("model", modelMatrix)
        pillMesh.draw()
    }

    fun setRotation(rx: Float, ry: Float) {
        rotationX = 35f + rx
        rotationY = ry
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        pendingBitmap = bitmap
    }

    /**
     * Professional UI/UX Resizing Function:
     * Adjusts the 'length' (width) and 'padding' (height/depth) of the refractive mesh.
     */
    fun updateMeshDimensions(width: Float, height: Float, depth: Float, radius: Float) {
        nextWidth = width
        nextHeight = height
        nextDepth = depth
        nextRadius = radius
        pendingMeshUpdate = true
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
