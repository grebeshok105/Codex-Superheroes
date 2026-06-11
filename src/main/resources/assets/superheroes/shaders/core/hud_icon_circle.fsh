#version 150

uniform sampler2D Sampler0;
uniform float QuadSize;
uniform float MaskRadius;
uniform float Darken;
uniform vec4 ColorModulator;

in vec2 vLocal;
out vec4 fragColor;

void main() {
	vec4 tex = texture(Sampler0, vLocal);
	vec2 p = (vLocal - vec2(0.5)) * QuadSize;
	float r = length(p);
	float aa = max(fwidth(r), 0.0001);
	float mask = 1.0 - smoothstep(MaskRadius - aa, MaskRadius + aa, r);
	fragColor = vec4(tex.rgb * (1.0 - Darken), tex.a * mask) * ColorModulator;
}
