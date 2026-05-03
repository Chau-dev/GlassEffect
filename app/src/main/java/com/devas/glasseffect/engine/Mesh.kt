package com.devas.glasseffect.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class Mesh(vertices: FloatArray, indices: IntArray, private val hasNormals: Boolean = true) {
    private var vao: Int = 0
    private var vbo: Int = 0
    private var ebo: Int = 0
    private val indexCount: Int = indices.size
    
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: IntBuffer
    
    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }
            
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .apply { put(indices); position(0) }
            
        setupMesh()
    }
    
    private fun setupMesh() {
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        val ebos = IntArray(1)
        
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        GLES30.glGenBuffers(1, ebos, 0)
        
        vao = vaos[0]
        vbo = vbos[0]
        ebo = ebos[0]
        
        GLES30.glBindVertexArray(vao)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        // Position attribute (location = 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0)
        
        // Normal attribute (location = 1)
        if (hasNormals) {
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4)
        }
        
        // TexCoord attribute (location = 2)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4)
        
        GLES30.glBindVertexArray(0)
    }
    
    fun draw() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }
    
    companion object {
        fun createSphere(radius: Float = 1.0f, sectors: Int = 36, stacks: Int = 18): Mesh {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            
            val pi = Math.PI.toFloat()
            
            for (i in 0..stacks) {
                val stackAngle = pi / 2 - i * pi / stacks
                val xy = radius * kotlin.math.cos(stackAngle)
                val z = radius * kotlin.math.sin(stackAngle)
                
                for (j in 0..sectors) {
                    val sectorAngle = j * 2 * pi / sectors
                    val x = xy * kotlin.math.cos(sectorAngle)
                    val y = xy * kotlin.math.sin(sectorAngle)
                    
                    // Position
                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z)
                    
                    // Normal (normalized)
                    val len = kotlin.math.sqrt(x*x + y*y + z*z)
                    vertices.add(x / len)
                    vertices.add(y / len)
                    vertices.add(z / len)
                    
                    // TexCoord
                    vertices.add(j.toFloat() / sectors)
                    vertices.add(i.toFloat() / stacks)
                }
            }
            
            for (i in 0 until stacks) {
                var k1 = i * (sectors + 1)
                var k2 = k1 + sectors + 1
                
                for (j in 0 until sectors) {
                    if (i != 0) {
                        indices.add(k1)
                        indices.add(k2)
                        indices.add(k1 + 1)
                    }
                    if (i != (stacks - 1)) {
                        indices.add(k1 + 1)
                        indices.add(k2)
                        indices.add(k2 + 1)
                    }
                    k1++
                    k2++
                }
            }
            
            return Mesh(vertices.toFloatArray(), indices.toIntArray())
        }
        
        fun createCube(): Mesh {
            val vertices = floatArrayOf(
                // Back face
                -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f,
                 0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f,
                 0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 1.0f, 0.0f,
                 0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 0.0f, 1.0f,
                // Front face
                -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f,
                 0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 1.0f, 0.0f,
                 0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f,
                 0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f,
                // Left face
                -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f, 1.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f, 0.0f, 0.0f,
                -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f,
                // Right face
                 0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f,
                 0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f,
                 0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f, 1.0f, 1.0f,
                 0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f,
                 0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f,
                 0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f, 0.0f, 0.0f,
                // Bottom face
                -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f,
                 0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f, 1.0f, 1.0f,
                 0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f,
                 0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f, 0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f,
                // Top face
                -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f,
                 0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f,
                 0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f, 1.0f, 1.0f,
                 0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f, 0.0f, 0.0f
            )
            
            // Full cube indices
            val indices = IntArray(36) { it }
            return Mesh(vertices, indices)
        }
    }
}
