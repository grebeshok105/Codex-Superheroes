package io.github.grebeshok105.codex.client.core.vfx.pattern;

import io.github.grebeshok105.codex.client.fx.ScreenShakeManager;

/** Camera feel seam for the pattern layer; delegates to {@link ScreenShakeManager}. */
public final class CameraImpulse {
	private CameraImpulse() {
	}

	public static void shake(float intensity, int ticks) {
		ScreenShakeManager.shake(intensity, ticks);
	}
}
