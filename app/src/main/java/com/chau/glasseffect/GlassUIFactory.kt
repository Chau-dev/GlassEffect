package com.chau.glasseffect

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * GlassUIFactory: A Mathematician's factory for bounded UI Manifolds.
 * 
 * As a physicist, we ensure these components have the correct "Material Properties"
 * (Alpha, Stroke, and Tint) to interact beautifully with the Screen-Space Refraction Engine.
 */
object GlassUIFactory {

    /**
     * Create a fully interactive 3D refractive glass component with specific lens parameters.
     */
    fun createInteractiveLens(
        context: Context,
        label: String,
        width: Float,
        height: Float,
        radius: Float,
        offX: Float,
        offY: Float,
        ior: Float = 1.65f,
        dispersion: Float = 0.08f,
        absorption: Float = 0.01f,
        onAction: () -> Unit
    ): GlassButton {
        return GlassButton(context).apply {
            button.text = label
            setGlassDimensions(width, height, radius * 2f, radius)
            setLensOffset(offX, offY)
            
            // Apply optical properties
            renderer.material.ior = ior
            renderer.material.dispersion = dispersion
            renderer.material.absorption = absorption
            
            setOnClickListener { onAction() }
        }
    }

    fun createGlassButton(context: Context, text: String): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = createGlassBackground()
            // Physical property: Surface tension (padding)
            setPadding(60, 30, 60, 30)
            elevation = 10f
        }
    }

    fun createGlassTextField(context: Context, hint: String): EditText {
        return EditText(context).apply {
            this.hint = hint
            setHintTextColor(Color.argb(180, 200, 200, 200))
            setTextColor(Color.WHITE)
            background = createGlassBackground(cornerRadius = 15f)
            setPadding(40, 30, 40, 30)
        }
    }

    fun createGlassCard(context: Context, title: String, content: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = createGlassBackground(alpha = 40, cornerRadius = 40f)
            setPadding(50, 50, 50, 50)
            
            val titleView = TextView(context).apply {
                text = title
                textSize = 20f
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, 20)
            }
            
            val contentView = TextView(context).apply {
                text = content
                setTextColor(Color.argb(200, 255, 255, 255))
            }
            
            addView(titleView)
            addView(contentView)
        }
    }

    /**
     * The "Refractive Hull" Generator.
     * Mathematically, this defines the boundary of the optical medium.
     */
    private fun createGlassBackground(
        alpha: Int = 25, 
        cornerRadius: Float = 30f,
        strokeAlpha: Int = 120
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            
            // Physicist's Absorption: The internal tint of the glass
            setColor(Color.argb(alpha, 255, 255, 255))
            
            // Mathematician's Boundary: The sharp edge of the manifold
            setStroke(3, Color.argb(strokeAlpha, 255, 255, 255))
        }
    }
}
