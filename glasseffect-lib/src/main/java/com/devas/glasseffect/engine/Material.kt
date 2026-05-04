package com.devas.glasseffect.engine

/**
 * Physically-based Glass Material
 * @param ior Index of Refraction (1.5 for glass, 2.4 for diamond)
 * @param dispersion Cauchy's constant (0.01 - 0.05 for spectral splitting)
 * @param absorption Beer-Lambert coefficient (higher = darker/denser glass)
 * @param tint Normalized RGB color of the medium
 */
data class GlassMaterial(
    var ior: Float = 1.52f,
    var dispersion: Float = 0.02f,
    var absorption: Float = 0.5f,
    var tint: FloatArray = floatArrayOf(0.8f, 0.9f, 1.0f)
) {
    fun applyToShader(shader: Shader) {
        shader.setFloat("material.ior", ior)
        shader.setFloat("material.dispersion", dispersion)
        shader.setFloat("material.absorption", absorption)
        shader.setVec3("material.tint", tint)
    }
}
