package com.chau.glasseffect.engine

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
        val vaos = IntArray(1); val vbos = IntArray(1); val ebos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        GLES30.glGenBuffers(1, ebos, 0)
        
        vao = vaos[0]; vbo = vbos[0]; ebo = ebos[0]
        
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0)
        if (hasNormals) {
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4)
        }
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4)
        GLES30.glBindVertexArray(0)
    }
    
    fun draw() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    fun destroy() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(ebo), 0)
    }
    
    companion object {
        /**
         * Core Manifold Generator: Creates a bevelled slab that acts as an optical lens.
         * Mathematically, this is the Minkowski sum of a rectangle and a sphere.
         */
        fun createBevelledSlab(
            width: Float, height: Float, depth: Float,
            radius: Float,
            segments: Int = 64
        ): Mesh {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Int>()

            val halfW = width / 2f
            val halfH = height / 2f
            val halfD = depth / 2f

            // Mathematically ensure radius fits within all three dimensions to prevent negative inner spans
            val r = radius.coerceAtMost(minOf(halfW, halfH, halfD)).coerceAtLeast(0f)
            val innerW = maxOf(0f, halfW - r)
            val innerH = maxOf(0f, halfH - r)
            val innerD = maxOf(0f, halfD - r)

            fun buildFace(nx: Float, ny: Float, nz: Float, ux: Float, uy: Float, uz: Float, vx: Float, vy: Float, vz: Float) {
                val startIdx = vertices.size / 8
                for (i in 0..segments) {
                    val vFrac = (i.toFloat() / segments) * 2f - 1f
                    for (j in 0..segments) {
                        val uFrac = (j.toFloat() / segments) * 2f - 1f
                        val px = (nx + ux * uFrac + vx * vFrac) * halfW
                        val py = (ny + uy * uFrac + vy * vFrac) * halfH
                        val pz = (nz + uz * uFrac + vz * vFrac) * halfD

                        val cx = px.coerceIn(-innerW, innerW)
                        val cy = py.coerceIn(-innerH, innerH)
                        val cz = pz.coerceIn(-innerD, innerD)
                        
                        val dx = px - cx; val dy = py - cy; val dz = pz - cz
                        val dLen = kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)

                        val nX: Float; val nY: Float; val nZ: Float
                        if (dLen > 0.0001f) { nX = dx / dLen; nY = dy / dLen; nZ = dz / dLen }
                        else { nX = nx; nY = ny; nZ = nz }
                        
                        vertices.add(cx + nX * r); vertices.add(cy + nY * r); vertices.add(cz + nZ * r)
                        vertices.add(nX); vertices.add(nY); vertices.add(nZ)
                        vertices.add(j.toFloat() / segments); vertices.add(i.toFloat() / segments)
                    }
                }
                for (i in 0 until segments) {
                    for (j in 0 until segments) {
                        val k = startIdx + i * (segments + 1) + j
                        indices.add(k); indices.add(k + segments + 1); indices.add(k + 1)
                        indices.add(k + 1); indices.add(k + segments + 1); indices.add(k + segments + 2)
                    }
                }
            }

            buildFace( 0f, 0f, 1f,  1f, 0f, 0f,  0f, 1f, 0f) // Front
            buildFace( 0f, 0f,-1f, -1f, 0f, 0f,  0f, 1f, 0f) // Back
            buildFace( 0f, 1f, 0f,  1f, 0f, 0f,  0f, 0f,-1f) // Top
            buildFace( 0f,-1f, 0f,  1f, 0f, 0f,  0f, 0f, 1f) // Bottom
            buildFace( 1f, 0f, 0f,  0f, 0f,-1f,  0f, 1f, 0f) // Right
            buildFace(-1f, 0f, 0f,  0f, 0f, 1f,  0f, 1f, 0f) // Left

            return Mesh(vertices.toFloatArray(), indices.toIntArray())
        }

        /** Professional factory function for a Liquid Glass Button */
        fun createLiquidButton(): Mesh {
            return createBevelledSlab(width = 2.4f, height = 0.9f, depth = 0.5f, radius = 0.44f)
        }

        /** Professional factory function for a Liquid Glass TextField */
        fun createLiquidTextField(): Mesh {
            return createBevelledSlab(width = 4.0f, height = 0.8f, depth = 0.4f, radius = 0.2f)
        }

        /** Professional factory function for a Liquid Glass Card */
        fun createLiquidCard(): Mesh {
            return createBevelledSlab(width = 3.5f, height = 5.0f, depth = 0.3f, radius = 0.4f)
        }

        /** The standard 'iOS Dock' Pill manifold */
        fun createCube(): Mesh {
            return createBevelledSlab(width = 4.2f, height = 1.0f, depth = 1.0f, radius = 0.5f)
        }

        fun createSphere(radius: Float = 1.0f, sectors: Int = 36, stacks: Int = 18): Mesh {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            val pi = Math.PI.toFloat()
            for (i in 0..stacks) {
                val phi = pi / 2 - (i.toFloat() / stacks) * pi
                val xy = radius * kotlin.math.cos(phi); val z = radius * kotlin.math.sin(phi)
                for (j in 0..sectors) {
                    val theta = j * 2 * pi / sectors
                    val x = xy * kotlin.math.cos(theta); val y = xy * kotlin.math.sin(theta)
                    vertices.add(x); vertices.add(y); vertices.add(z)
                    val len = radius.coerceAtLeast(0.0001f)
                    vertices.add(x/len); vertices.add(y/len); vertices.add(z/len)
                    vertices.add(j.toFloat()/sectors); vertices.add(i.toFloat()/stacks)
                }
            }
            for (i in 0 until stacks) {
                var k1 = i * (sectors + 1); var k2 = k1 + sectors + 1
                for (j in 0 until sectors) {
                    if (i != 0) { indices.add(k1); indices.add(k2); indices.add(k1 + 1) }
                    if (i != stacks - 1) { indices.add(k1 + 1); indices.add(k2); indices.add(k2 + 1) }
                    k1++; k2++
                }
            }
            return Mesh(vertices.toFloatArray(), indices.toIntArray())
        }
    }
}
