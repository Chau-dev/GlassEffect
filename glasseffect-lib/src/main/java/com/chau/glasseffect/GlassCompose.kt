package com.chau.glasseffect

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Jetpack Compose wrapper for GlassLayout.
 * 
 * @param modifier Compose modifier for sizing and positioning.
 * @param ior Index of Refraction.
 * @param dispersion Chromatic dispersion intensity.
 * @param autoEnvironment If true, automatically captures the background behind the glass.
 * @param content The UI content to be placed "inside" the glass lens.
 */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    ior: Float = 1.65f,
    dispersion: Float = 0.08f,
    autoEnvironment: Boolean = true,
    content: @Composable () -> Unit
) {
    AndroidView(
        factory = { context ->
            GlassLayout(context).apply {
                this.autoEnvironment = autoEnvironment
                this.renderer.material.ior = ior
                this.renderer.material.dispersion = dispersion
                
                // Add a ComposeView as a child to host the content
                val composeView = ComposeView(context).apply {
                    setContent { content() }
                }
                addView(composeView)
            }
        },
        modifier = modifier,
        update = { view ->
            view.autoEnvironment = autoEnvironment
            view.renderer.material.ior = ior
            view.renderer.material.dispersion = dispersion
            
            // Update the content if needed
            (view.getChildAt(1) as? ComposeView)?.setContent { content() }
        }
    )
}
