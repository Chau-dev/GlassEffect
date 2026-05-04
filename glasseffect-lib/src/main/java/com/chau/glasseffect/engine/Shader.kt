package com.chau.glasseffect.engine

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class Shader(context: Context, vertexResId: Int, fragmentResId: Int) {
    private var programId: Int = 0
    
    init {
        val vertexCode = readRawTextFile(context, vertexResId)
        val fragmentCode = readRawTextFile(context, fragmentResId)
        programId = createProgram(vertexCode, fragmentCode)
    }
    
    fun use() = GLES30.glUseProgram(programId)
    fun getId() = programId
    
    fun setMat4(name: String, value: FloatArray) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniformMatrix4fv(loc, 1, false, value, 0)
    }
    
    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniform3f(loc, x, y, z)
    }
    
    fun setVec3(name: String, value: FloatArray) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniform3fv(loc, 1, value, 0)
    }

    fun setVec2(name: String, x: Float, y: Float) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniform2f(loc, x, y)
    }

    fun setVec2(name: String, value: FloatArray) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniform2fv(loc, 1, value, 0)
    }
    
    fun setFloat(name: String, value: Float) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniform1f(loc, value)
    }
    
    fun setInt(name: String, value: Int) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        GLES30.glUniform1i(loc, value)
    }
    
    private fun readRawTextFile(context: Context, resId: Int): String {
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(resId)))
        return reader.use { it.readText() }
    }
    
    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)
        
        val program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }
        
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e("Shader", "Program link error: ${GLES30.glGetProgramInfoLog(program)}")
        }
        return program
    }
    
    private fun loadShader(type: Int, code: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, code)
            GLES30.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e("Shader", "Compile error: ${GLES30.glGetShaderInfoLog(shader)}")
            }
        }
    }
}
