package com.chau.glasseffect

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class SecondActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.DKGRAY)
        }

        val label = TextView(this).apply {
            text = "Welcome to the Second Screen"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }
        root.addView(label)

        val backButton = GlassButton(this).apply {
            button.text = "Back to Main"
            layoutParams = FrameLayout.LayoutParams(500, 150).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 200
            }
            setOnClickListener { finish() }
        }
        root.addView(backButton)

        setContentView(root)
    }
}
