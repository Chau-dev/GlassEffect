#version 300 es
precision highp float;

in vec3 FragPos;
in vec3 Normal;
in vec3 ViewDir;

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

void main() {
    vec3 N = normalize(Normal);
    vec3 V = normalize(-ViewDir);
    float cosTheta = max(dot(N, V), 0.0);

    // 1. Physically-based Fresnel (The Reflection Shell)
    // High power (6.0) keeps the center clear, and edges sharp
    float F0 = pow((1.0 - material.ior) / (1.0 + material.ior), 2.0);
    float fresnel = F0 + (1.0 - F0) * pow(1.0 - cosTheta, 6.0);

    // 2. High-Clarity Refraction (Dispersion)
    float refrR = 1.0 / (material.ior - material.dispersion);
    float refrG = 1.0 / material.ior;
    float refrB = 1.0 / (material.ior + material.dispersion);

    vec3 refrDirR = refract(ViewDir, N, refrR);
    vec3 refrDirG = refract(ViewDir, N, refrG);
    vec3 refrDirB = refract(ViewDir, N, refrB);

    // TIR Protection
    if (length(refrDirG) < 0.1) refrDirG = reflect(ViewDir, N);

    vec2 screenUV = gl_FragCoord.xy / uResolution;
    
    // 3. Ghost-Clear Sampling
    // Very subtle liquid shimmer
    float flow = sin(uTime * 0.8 + FragPos.x * 5.0) * 0.004;
    
    vec3 color;
    float refrScale = 0.18; 
    color.r = texture(uBackground, screenUV + refrDirR.xy * refrScale + flow).r;
    color.g = texture(uBackground, screenUV + refrDirG.xy * refrScale + flow).g;
    color.b = texture(uBackground, screenUV + refrDirB.xy * refrScale + flow).b;

    // 4. Invisible Transmission Model (Anti-Black)
    // Center is purely the background image. No absorption in the center.
    float pathLength = pow(1.0 - cosTheta, 4.0);
    vec3 transmission = exp(-material.absorption * (vec3(1.0) - material.tint) * pathLength);
    vec3 liquidMedium = color * transmission;

    // 5. Studio Environment Reflection
    vec3 R = reflect(ViewDir, N);
    float skyGlow = smoothstep(-0.1, 1.0, R.y);
    vec3 reflection = vec3(1.0, 1.0, 1.0) * skyGlow * 0.5;

    // 6. Brilliant Specular Sun-Glints
    vec3 L = normalize(vec3(1.5, 3.0, 2.0));
    vec3 H = normalize(V + L);
    float sharpGlint = pow(max(dot(N, H), 0.0), 1024.0) * 5.0; // Sharp star-like glint
    float surfaceGloss = pow(max(dot(N, H), 0.0), 64.0) * 0.4;
    
    // 7. Final Composition
    // Center is refracted background (alpha 0.1-0.2)
    // Edges are reflective shell (alpha 0.9)
    vec3 finalColor = mix(liquidMedium, reflection + vec3(0.5), fresnel * 0.6);
    finalColor += vec3(sharpGlint + surfaceGloss);

    // 8. Dynamic Transparency
    // alpha = 0.15 in center (very transparent), 0.98 at edges
    float alpha = mix(0.15, 0.98, fresnel);

    FragColor = vec4(clamp(finalColor, 0.0, 1.0), alpha);
}
