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

	private static final FlashEnvelope flash = new FlashEnvelope(FLASH_TICKS);

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
		flash.feed(intensity, rgb);
	}

	@Override
	public void distortion(Vec3 center, float radius, float strength) {
	}

	/** Draws the current flash as a fading full-screen HUD overlay (registered by {@code VfxRuntime.init}). */
	public static void renderFlashHud(GuiGraphics graphics, DeltaTracker delta) {
		float intensity = flash.shown();
		if (intensity > 0f) {
			int alpha = (int) (255f * intensity);
			if (alpha > 0) {
				graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(),
						alpha << 24 | flash.argb());
			}
		}
		flash.tick(delta.getGameTimeDeltaTicks());
	}
}
