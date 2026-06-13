#version 120
// Pandora "Vanitas" death cinematic shader.
// Every solid pixel (depth < 1.0 => terrain, entities, lightning, hand) is
// repainted ABSOLUTELY black — flat silhouettes, no detail. The background
// (sky / void, depth == 1.0) becomes a red-to-white vertical gradient like the
// reference art: lighter pink-red at the top, deep red toward the bottom.

uniform sampler2D depthtex0;
uniform float viewHeight;

varying vec2 texcoord;

void main() {
	float depth = texture2D(depthtex0, texcoord).r;

	if (depth >= 1.0) {
		// texcoord.y: 0 at the bottom of the screen, 1 at the top (GL convention).
		float t = clamp(texcoord.y, 0.0, 1.0);
		// Smooth the ramp a touch so the centre band reads like the artwork.
		t = pow(t, 0.85);
		vec3 bottom = vec3(0.62, 0.03, 0.07);   // deep blood red
		vec3 topCol = vec3(1.00, 0.62, 0.66);   // washed pink-white highlight
		vec3 col = mix(bottom, topCol, t);
		gl_FragColor = vec4(col, 1.0);
	} else {
		gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
	}
}
