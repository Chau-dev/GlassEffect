#version 300 es
precision highp float;

in vec3 FragPos;
in vec3 Normal;
in vec3 ViewDir;
in vec3 vLocalPos; // From vertex shader for accurate mathematical clipping

out vec4 FragColor;

struct Material {
    float ior;
    float dispersion;
    float absorption;
    vec3 tint;
};

uniform Material material;
uniform sampler2D uBackground;
uniform vec2 uResolution;
uniform float uTime;

// Helper to convert OpenGL screen UVs (0,0 at bottom) 
// to Android Texture UVs (0,0 at top).
vec2 getBgUV(vec2 uv) {
    return vec2(uv.x, 1.0 - uv.y);
}

void main() {
    // 0. Topological Truncation (The iOS "Cutoff")
    // We truncate the sphere to create a 'Liquid Pill' manifold.
    // Cutoff at 0.82 creates flat top/bottom interfaces that catch grazing light.
    float cutoff = 0.82; 
    if (abs(vLocalPos.y) > cutoff) discard;

    vec3 N = normalize(Normal);
    vec2 screenUV = gl_FragCoord.xy / uResolution;

    // 1. Liquid Surface Perturbation (Laminar Flow)
    // As physicists, we simulate surface tension waves by perturbing the normal field
    // with a low-frequency sinusoidal sum relative to time and space.
    float shimmer = sin(vLocalPos.y * 12.0 + uTime * 2.5) * 0.015;
    float ripple = cos(vLocalPos.x * 15.0 - uTime * 1.8) * 0.01;
    N = normalize(N + vec3(shimmer, ripple, shimmer));

    vec3 V = normalize(-ViewDir);
    float cosTheta = clamp(dot(N, V), 0.0, 1.0);

    // 2. Physically-based Fresnel (Schlick's Approximation)
    // Reflection intensity increases at grazing angles.
    float r0 = pow((1.0 - material.ior) / (1.0 + material.ior), 2.0);
    float fresnel = r0 + (1.0 - r0) * pow(1.0 - cosTheta, 5.0);

    // 2.5 Screen-Space UI Mirror Reflection (The "Dog Icon" logic)
    // Reflecting the view ray across the surface normal to sample the "environment" (Top UI).
    // Since the pill is at the bottom, its normals point up, reflecting rays toward the top.
    vec3 R_surf = reflect(normalize(ViewDir), N);
    vec2 reflectUV = clamp(screenUV + R_surf.xy * 0.75, 0.0, 1.0);
    vec3 uiReflection = texture(uBackground, getBgUV(reflectUV)).rgb;

    // 3. Cauchy Dispersion: Frequency-Dependent Speed of Light
    float refrR = 1.0 / (material.ior - material.dispersion);
    float refrG = 1.0 / material.ior;
    float refrB = 1.0 / (material.ior + material.dispersion);

    vec3 dirR = refract(normalize(ViewDir), N, refrR);
    vec3 dirG = refract(normalize(ViewDir), N, refrG);
    vec3 dirB = refract(normalize(ViewDir), N, refrB);

    // 4. Mathematician's Path Length Correction
    // Thickness is modulated by the cutoff boundary to simulate a heavy glass slab.
    float slabDepth = 0.45; 
    float pathLength = (1.0 - abs(vLocalPos.y / cutoff)) * slabDepth + (1.0 - cosTheta) * 0.35;

    // 5. Chromatic Displacement (Prismatic Splitting)
    vec3 refractedColor;
    refractedColor.r = texture(uBackground, getBgUV(screenUV + dirR.xy * pathLength * 0.45)).r;
    refractedColor.g = texture(uBackground, getBgUV(screenUV + dirG.xy * pathLength * 0.45)).g;
    refractedColor.b = texture(uBackground, getBgUV(screenUV + dirB.xy * pathLength * 0.45)).b;

    // 6. Volumetric Absorption (Beer-Lambert Law)
    vec3 absorptionCoeff = (vec3(1.0) - material.tint) * material.absorption;
    vec3 transmission = exp(-absorptionCoeff * pathLength * 15.0);
    vec3 internalLight = refractedColor * transmission;

    // 7. iOS Specular Highlights & Mirror Composition
    vec3 LightPos = normalize(vec3(0.5, 3.0, 1.2)); 
    vec3 H = normalize(V + LightPos);
    float sharpGlint = pow(max(dot(N, H), 0.0), 2048.0) * 5.0; // Extreme sharp highlight
    float rimLight = pow(1.0 - cosTheta, 4.0) * 0.7;
    
    // Boundary Glow (Interface Scatter)
    float edgeGlow = smoothstep(cutoff - 0.06, cutoff, abs(vLocalPos.y)) * 0.7;
    
    // Mix the UI reflection into the reflective shell based on Fresnel
    vec3 reflection = mix(uiReflection, vec3(1.0), 0.4) * (fresnel + rimLight);
    reflection += sharpGlint + material.tint * edgeGlow;

    // 8. Final Composition
    vec3 finalColor = mix(internalLight, reflection, fresnel);
    
    // Opacity increases at the edges and cutoff boundaries for a solid look.
    float alpha = mix(0.12, 0.99, fresnel + edgeGlow * 0.4);

    FragColor = vec4(clamp(finalColor, 0.0, 1.0), alpha);
}
