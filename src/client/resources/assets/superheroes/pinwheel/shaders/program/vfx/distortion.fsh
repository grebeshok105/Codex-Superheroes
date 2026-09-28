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
    vec2 centerUv = worldToScreenSpacePosition(uCenter).xy;
    vec3 worldRight = normalize((VeilCamera.IViewMat * vec4(1.0, 0.0, 0.0, 0.0)).xyz);
    float screenRadius = max(distance(
            worldToScreenSpacePosition(uCenter + worldRight * uRadius).xy, centerUv), 1.0e-4);

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
