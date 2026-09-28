package io.github.grebeshok105.codex.client.core.vfx.backend;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Veil-free {@link VfxBackend}: emitters and dynamic lights are no-ops
 * (the mod runs without Veil), {@link #flash} degrades to a fading HUD
 * overlay, distortion is dropped.
 */
public final class FallbackVfxBackend implements VfxBackend {
	private static final float FLASH_TICKS = 6f;

	private static float flashIntensity;
	private static int flashRgb;
	private static float flashTicksLeft;

	private static final LightHandle NOOP_LIGHT = new LightHandle() {
		@Override
		public void move(Vec3 pos) {
		}

		@Override
		public void set(int rgb, float radius, float brightness) {
		}

		@Override
		public void remove() {
		}
	};

	@Override
	public void emit(ResourceLocation emitter, Vec3 pos) {
	}

	@Override
	public LightHandle light(Vec3 pos, int rgb, float radius, float brightness) {
		return NOOP_LIGHT;
	}

	@Override
	public void flash(float intensity, int rgb) {
		if (intensity <= 0f) {
			return;
		}
		if (intensity >= flashIntensity || flashTicksLeft <= 0f) {
			flashIntensity = Math.min(intensity, 1f);
			flashRgb = rgb & 0xFFFFFF;
		}
		flashTicksLeft = FLASH_TICKS;
	}

	@Override
	public void distortion(Vec3 center, float radius, float strength) {
	}

	/** Draws the current flash as a fading full-screen HUD overlay (registered by {@code VfxRuntime.init}). */
	public static void renderFlashHud(GuiGraphics graphics, DeltaTracker delta) {
		if (flashTicksLeft <= 0f || flashIntensity <= 0f) {
			return;
		}
		int alpha = (int) (255f * flashIntensity * (flashTicksLeft / FLASH_TICKS));
		if (alpha > 0) {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | flashRgb);
		}
		flashTicksLeft -= delta.getGameTimeDeltaTicks();
	}
}
