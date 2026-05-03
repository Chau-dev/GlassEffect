package com.devas.glasseffect.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils

class CubeMap(context: Context, faces: List<Int>) {
    var id: Int = 0
        private set
    
    init {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        id = textures[0]
        
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, id)
        
        faces.forEachIndexed { index, resId ->
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            if (bitmap != null) {
                GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + index, 0, bitmap, 0)
                bitmap.recycle()
            }
        }
        
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_CUBE_MAP)
    }
    
    fun bind(unit: Int = 0) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, id)
    }
}
