package com.example.superheroes.client.fx;

import java.util.Random;

public final class ScreenShakeManager {
	private static float intensity = 0f;
	private static int durationLeft = 0;
	private static int durationStart = 1;
	private static final Random RNG = new Random();
	private static final float[] OUT = new float[2];

	private ScreenShakeManager() {
	}

	public static void shake(float power, int durationTicks) {
		if (power > intensity) {
			intensity = power;
			durationLeft = durationTicks;
			durationStart = Math.max(1, durationTicks);
		} else {
			durationLeft = Math.max(durationLeft, durationTicks);
		}
	}

	public static boolean isActive() {
		return intensity > 0.01f && durationLeft > 0;
	}

	public static void tick() {
		if (durationLeft > 0) {
			durationLeft--;
			if (durationLeft <= 0) {
				intensity = 0f;
			}
		}
	}

	public static float[] sample() {
		float decay = (float) durationLeft / (float) durationStart;
		float amp = intensity * decay * 4f;
		OUT[0] = (RNG.nextFloat() * 2f - 1f) * amp;
		OUT[1] = (RNG.nextFloat() * 2f - 1f) * amp;
		return OUT;
	}
}
