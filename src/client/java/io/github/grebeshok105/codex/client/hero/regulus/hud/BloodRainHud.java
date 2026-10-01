package io.github.grebeshok105.codex.client.hero.regulus.hud;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientMadnessState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Blood-on-screen overlay for the madness window: static sprite layers
 * (edge drips hanging from the top, corner vignettes, a one-shot central
 * splatter on entry) whose alpha ramps in over ~3.5 s and pulses with the
 * shared heartbeat driver ({@link MadnessHudOverlay#heartbeatPulse()}).
 * All timing is synced game-tick — a client that observes the attachment
 * late sees the same phase as everyone else.
 */
public final class BloodRainHud {
	private static final ResourceLocation[] EDGE_SPRITES = {
			ModId.of("textures/gui/regulus/blood_edge_1.png"),
			ModId.of("textures/gui/regulus/blood_edge_2.png"),
			ModId.of("textures/gui/regulus/blood_edge_3.png"),
			ModId.of("textures/gui/regulus/blood_edge_4.png"),
	};
	private static final ResourceLocation[] CORNER_SPRITES = {
			ModId.of("textures/gui/regulus/blood_corner_1.png"),
			ModId.of("textures/gui/regulus/blood_corner_2.png"),
	};
	private static final ResourceLocation HIT_SPRITE =
			ModId.of("textures/gui/regulus/blood_hit.png");

	private static final int EDGE_TEX_W = 72;
	private static final int EDGE_TEX_H = 150;
	private static final int CORNER_TEX = 110;
	private static final int HIT_TEX = 180;
	/** Alpha ramp-in after the madness edge (the old darken fade was 3.5 s). */
	private static final long RAMP_TICKS = 70L;
	/** Central splatter visibility window on madness entry. */
	private static final long HIT_TICKS = 28L;
	/** Base alpha of the persistent layers; heartbeat adds on top. */
	private static final float LAYER_ALPHA = 0.62f;
	private static final float PULSE_ALPHA = 0.38f;
	private static final float EDGE_SCALE = 1.6f;
	private static final float CORNER_SCALE = 1.35f;

	/** X fractions of the four edge drips along the top of the screen. */
	private static final float[] EDGE_X = {0.12f, 0.36f, 0.63f, 0.87f};

	private static volatile boolean active = false;
	private static volatile long startedTick = -1L;

	static {
		ClientSessionState.register(BloodRainHud::clear);
	}

	private BloodRainHud() {
	}

	public static void trigger() {
		active = true;
		startedTick = gameTime();
	}

	public static void clear() {
		active = false;
		startedTick = -1L;
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		boolean madness = ClientMadnessState.isMadness();
		if (!madness) {
			if (active) {
				clear();
			}
			return;
		}
		if (!active) {
			trigger();
		}
		long now = gameTime();
		long age = startedTick < 0L ? 0L : now - startedTick;
		float ramp = Math.min(1f, age / (float) RAMP_TICKS);
		if (ramp <= 0f) {
			return;
		}
		float pulse = MadnessHudOverlay.heartbeatPulse();
		float layerAlpha = ramp * (LAYER_ALPHA + PULSE_ALPHA * pulse);
		int sw = graphics.guiWidth();
		int sh = graphics.guiHeight();

		// Central splatter on entry — decays over HIT_TICKS.
		if (age < HIT_TICKS) {
			float hitA = (1f - age / (float) HIT_TICKS) * 0.9f;
			float hitScale = 0.4f * sh / HIT_TEX * (1f + age / (float) HIT_TICKS * 0.15f);
			int hw = (int) (HIT_TEX * hitScale);
			blit(graphics, HIT_SPRITE, (sw - hw) / 2f, (sh - hw) / 2f, HIT_TEX,
					hitScale, hitA, false, false);
		}

		// Edge drips hanging from the top edge; odd indices are mirrored.
		for (int i = 0; i < EDGE_SPRITES.length; i++) {
			float x = EDGE_X[i] * sw - EDGE_TEX_W * EDGE_SCALE / 2f;
			blit(graphics, EDGE_SPRITES[i], x, -8f, EDGE_TEX_W, EDGE_TEX_H,
					EDGE_SCALE, layerAlpha, i % 2 == 1, false);
		}

		// Corner vignettes: top-right mirrors horizontally, bottom flips
		// vertically — two sprites cover all four corners.
		blit(graphics, CORNER_SPRITES[0], 0f, -4f, CORNER_TEX, CORNER_TEX,
				CORNER_SCALE, layerAlpha * 0.9f, false, false);
		blit(graphics, CORNER_SPRITES[0], sw, -4f, CORNER_TEX, CORNER_TEX,
				CORNER_SCALE, layerAlpha * 0.9f, true, false);
		blit(graphics, CORNER_SPRITES[1], 0f, sh + 4f, CORNER_TEX, CORNER_TEX,
				CORNER_SCALE, layerAlpha * 0.75f, false, true);
		blit(graphics, CORNER_SPRITES[1], sw, sh + 4f, CORNER_TEX, CORNER_TEX,
				CORNER_SCALE, layerAlpha * 0.75f, true, true);
	}

	private static void blit(GuiGraphics graphics, ResourceLocation sprite,
			float x, float y, int w, float scale, float alpha, boolean flipX, boolean flipY) {
		blit(graphics, sprite, x, y, w, w, scale, alpha, flipX, flipY);
	}

	/**
	 * Blits a full gui overlay sprite scaled by {@code scale} with an alpha
	 * tint. {@code flipX}/{@code flipY} mirror around the passed anchor —
	 * for the right/bottom sides callers pass the screen edge and the sprite
	 * lands mirrored inward.
	 */
	private static void blit(GuiGraphics graphics, ResourceLocation sprite,
			float x, float y, int texW, int texH, float scale,
			float alpha, boolean flipX, boolean flipY) {
		if (alpha <= 0.004f) {
			return;
		}
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0f);
		graphics.pose().scale(scale * (flipX ? -1f : 1f), scale * (flipY ? -1f : 1f), 1f);
		graphics.setColor(1f, 1f, 1f, Math.min(1f, alpha));
		graphics.blit(sprite, 0, 0, 0f, 0f, texW, texH, texW, texH);
		graphics.setColor(1f, 1f, 1f, 1f);
		graphics.pose().popPose();
	}

	private static long gameTime() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0L : mc.level.getGameTime();
	}
}
