#version 300 es
precision highp float;

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;

out vec3 FragPos;
out vec3 Normal;
out vec3 ViewDir;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform vec3 cameraPos;
uniform float uTime;
uniform vec3 uGravity; // Accelerometer data

void main() {
    // Liquid sloshing: Shift the "center of mass" of ripples based on gravity
    float slosh = dot(aPos, uGravity) * 0.15;
    
    // Dynamic fluid ripples influenced by tilt
    float wave = sin(aPos.x * 3.0 + uTime + uGravity.x * 2.0) * 0.04 + 
                 cos(aPos.y * 3.0 + uTime * 0.8 + uGravity.y * 2.0) * 0.04 +
                 sin(aPos.z * 3.0 + uTime * 1.2) * 0.03;
                 
    vec3 displacedPos = aPos + aNormal * (wave + slosh);
    
    FragPos = vec3(model * vec4(displacedPos, 1.0));
    
    // Perturb normal based on fluid slosh and ripples
    vec3 n = aNormal;
    n += uGravity * 0.1; 
    n.x += cos(uTime + aPos.y * 10.0) * 0.05;
    n.y += sin(uTime + aPos.z * 10.0) * 0.05;
    
    Normal = normalize(mat3(transpose(inverse(model))) * n);
    ViewDir = normalize(FragPos - cameraPos);
    
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
