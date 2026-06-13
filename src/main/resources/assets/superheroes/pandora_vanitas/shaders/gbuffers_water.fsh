#version 120
varying vec4 color;
void main() {
	// Geometry colour is irrelevant — final.fsh repaints every solid pixel pure black.
	gl_FragData[0] = color;
}