uniform sampler2D DiffuseSampler;

uniform float uIntensity;
uniform vec3 uColor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    fragColor = vec4(mix(scene.rgb, uColor, clamp(uIntensity, 0.0, 1.0)), scene.a);
}
