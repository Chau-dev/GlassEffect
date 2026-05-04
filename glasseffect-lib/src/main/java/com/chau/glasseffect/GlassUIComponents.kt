package com.chau.glasseffect

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

/**
 * Pre-built Glass UI Components for the Library
 */

class GlassButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GlassLayout(context, attrs) {
    val button = Button(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(Color.WHITE)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    init { addView(button) }
}

class GlassSearchBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GlassLayout(context, attrs) {
    val editText = EditText(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.argb(150, 255, 255, 255))
        hint = "Search..."
        gravity = Gravity.CENTER_VERTICAL
        setPadding(40, 0, 40, 0)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    init { addView(editText) }
}
