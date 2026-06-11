package com.example.superheroes.client.hud;

public final class HudAnimator {
	private HudAnimator() {
	}

	public static float smoothBar(float current, float target, float speed) {
		if (Math.abs(current - target) < 0.01f) {
			return target;
		}
		return current + (target - current) * speed;
	}

	public static float pulse(float frequencyHz) {
		return 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.001 * frequencyHz * 2.0 * Math.PI);
	}

	public static float cooldownArc(int remaining, int total) {
		if (total <= 0) {
			return 1.0f;
		}
		return 1.0f - Math.max(0f, Math.min(1f, remaining / (float) total));
	}

	public static float smoothstep(float x) {
		float c = Math.max(0f, Math.min(1f, x));
		return c * c * (3f - 2f * c);
	}

	public static int lerpColor(int c1, int c2, float t) {
		int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
		int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
		int a = (int) (a1 + (a2 - a1) * t);
		int r = (int) (r1 + (r2 - r1) * t);
		int g = (int) (g1 + (g2 - g1) * t);
		int b = (int) (b1 + (b2 - b1) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
