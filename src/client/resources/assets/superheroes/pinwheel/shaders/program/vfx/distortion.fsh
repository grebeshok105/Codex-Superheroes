#include veil:space_helper

uniform sampler2D DiffuseSampler;

uniform float uIntensity;
uniform vec3 uCenter;
uniform float uRadius;
uniform float uStrength;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // uCenter/uRadius are world space; project them through the VeilCamera UBO.
    // Call worldToScreenSpace directly: the *Position convenience macros lose
    // their argument under Veil's dynamic-shader recompile path (the driver
    // sees `pos` unbound and the program fails to compile).
    vec2 centerUv = worldToScreenSpace(vec4(uCenter, 1.0)).xy;
    vec3 worldRight = normalize((VeilCamera.IViewMat * vec4(1.0, 0.0, 0.0, 0.0)).xyz);
    float screenRadius = max(distance(
            worldToScreenSpace(vec4(uCenter + worldRight * uRadius, 1.0)).xy, centerUv), 1.0e-4);

    vec2 offset = texCoord - centerUv;
    float dist = length(offset) / screenRadius;
    vec2 uv = texCoord;
    if (dist < 1.0) {
        float edge = 1.0 - dist;
        float pull = uStrength * clamp(uIntensity, 0.0, 1.0) * edge * edge;
        uv = centerUv + offset * (1.0 - pull);
    }
    fragColor = texture(DiffuseSampler, uv);
}
