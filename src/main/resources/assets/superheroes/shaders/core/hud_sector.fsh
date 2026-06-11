#version 150

uniform float InnerRadius;
uniform float OuterRadius;
uniform vec2 Arc;
uniform vec4 FillColor;
uniform vec4 BorderColor;
uniform float BorderWidth;
uniform vec4 GlowColor;
uniform float GlowRadius;
uniform vec4 ColorModulator;

in vec2 vLocal;
out vec4 fragColor;

const float TWO_PI = 6.28318530718;

void main() {
	float r = length(vLocal);
	float ang = atan(-vLocal.y, vLocal.x);

	// signed distance: radial band
	float dRad = max(InnerRadius - r, r - OuterRadius);

	// signed distance to angular bounds, converted to arc length (px)
	float center = (Arc.x + Arc.y) * 0.5;
	float halfSpan = (Arc.y - Arc.x) * 0.5;
	float rel = mod(ang - center + TWO_PI + 3.14159265, TWO_PI) - 3.14159265;
	float dAng = (abs(rel) - halfSpan) * max(r, 0.75);

	float d = max(dRad, dAng);
	float aa = max(fwidth(d), 0.0001);

	float borderT = smoothstep(-BorderWidth - aa, -BorderWidth + aa, d) * step(0.01, BorderWidth);
	vec4 inCol = mix(FillColor, BorderColor, borderT);

	float gr = max(GlowRadius, 0.0001);
	float gt = clamp(d / gr, 0.0, 1.0);
	vec4 glow = vec4(GlowColor.rgb, GlowColor.a * exp(-4.5 * gt) * (1.0 - gt));

	float outT = smoothstep(-aa, aa, d);
	fragColor = mix(inCol, glow, outT) * ColorModulator;
}
