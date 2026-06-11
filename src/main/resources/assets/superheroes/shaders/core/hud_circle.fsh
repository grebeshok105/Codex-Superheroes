#version 150

uniform float Radius;
uniform float RingWidth;
uniform vec4 FillColor;
uniform vec4 RingColor;
uniform vec4 GlowColor;
uniform float GlowRadius;
uniform vec2 Arc;
uniform vec4 ColorModulator;

in vec2 vLocal;
out vec4 fragColor;

const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

void main() {
	float r = length(vLocal);
	float aa = max(fwidth(r), 0.0001);

	// angular mask (1 inside [Arc.x, Arc.y], smooth edges). Screen y is down,
	// so flip y to keep math angles CCW with 0 = right, pi/2 = up.
	float angMask = 1.0;
	float span = Arc.y - Arc.x;
	if (span < TWO_PI - 0.0005) {
		float ang = atan(-vLocal.y, vLocal.x);
		float rel = mod(ang - Arc.x, TWO_PI);
		float aaAng = 1.5 * aa / max(r, 0.75);
		angMask = smoothstep(0.0, aaAng, rel) * (1.0 - smoothstep(span - aaAng, span, rel));
	}

	float innerEdge = Radius - RingWidth;
	float ringT = smoothstep(innerEdge - aa, innerEdge + aa, r) * step(0.01, RingWidth);
	vec4 ring = vec4(RingColor.rgb, RingColor.a * angMask);
	vec4 inCol = mix(FillColor, ring, ringT);

	float d = r - Radius;
	float gr = max(GlowRadius, 0.0001);
	float gt = clamp(d / gr, 0.0, 1.0);
	vec4 glow = vec4(GlowColor.rgb, GlowColor.a * exp(-4.5 * gt) * (1.0 - gt) * angMask);

	float outT = smoothstep(-aa, aa, d);
	fragColor = mix(inCol, glow, outT) * ColorModulator;
}
