#version 150

uniform vec2 HalfSize;
uniform float Radius;
uniform vec4 FillTop;
uniform vec4 FillBottom;
uniform vec4 BorderColor;
uniform float BorderWidth;
uniform vec4 GlowColor;
uniform float GlowRadius;
uniform vec4 ColorModulator;

in vec2 vLocal;
out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
	vec2 q = abs(p) - b + vec2(r);
	return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
	float d = sdRoundBox(vLocal, HalfSize, Radius);
	float aa = max(fwidth(d), 0.0001);

	// vertical gradient + subtle light sheen near the top edge
	float t = clamp((vLocal.y + HalfSize.y) / max(2.0 * HalfSize.y, 0.0001), 0.0, 1.0);
	vec4 fill = mix(FillTop, FillBottom, t);
	float sheen = (1.0 - smoothstep(0.0, 5.0, vLocal.y + HalfSize.y)) * 0.055;
	fill.rgb = min(fill.rgb + vec3(sheen), vec3(1.0));

	// border band just inside the edge
	float borderT = smoothstep(-BorderWidth - aa, -BorderWidth + aa, d) * step(0.01, BorderWidth);
	vec4 inCol = mix(fill, BorderColor, borderT);

	// soft exponential outer glow (CSS box-shadow style)
	float gr = max(GlowRadius, 0.0001);
	float gt = clamp(d / gr, 0.0, 1.0);
	vec4 glow = vec4(GlowColor.rgb, GlowColor.a * exp(-4.5 * gt) * (1.0 - gt));

	float outT = smoothstep(-aa, aa, d);
	fragColor = mix(inCol, glow, outT) * ColorModulator;
}
