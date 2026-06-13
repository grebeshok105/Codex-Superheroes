package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientPandoraDeathState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Pandora — one-time death cinematic full-screen overlay (#7).
 *
 * <p>Repaints the whole screen blood-red and floods it dark so the world reads as black
 * silhouettes (à la a held F1 with the colour drained to crimson). Hides the normal HUD
 * by covering it, peaks at the "rise", then fades cleanly back. Pairs with the camera
 * turn ({@code CameraMixin}) and the child-giggle sound fired server-side.
 *
 * <p>Pure tuning numbers below — easy to dial in once tested in-game.
 */
public final class PandoraDeathOverlay {
	private PandoraDeathOverlay() {
	}

	public static void render(GuiGraphics g, DeltaTracker tracker) {
		if (!ClientPandoraDeathState.active()) {
			return;
		}
		float p = ClientPandoraDeathState.progress();
		if (p <= 0.001f) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		// Envelope: ramp up fast (0→0.2), hold full, fade out (0.85→1).
		float env;
		if (p < 0.20f) {
			env = p / 0.20f;
		} else if (p < 0.85f) {
			env = 1f;
		} else {
			env = 1f - (p - 0.85f) / 0.15f;
		}
		env = Mth.clamp(env, 0f, 1f);

		// 1) Deep darken so everything collapses into silhouettes.
		int darkA = (int) (0.82f * env * 255f);
		g.fill(0, 0, w, h, (Mth.clamp(darkA, 0, 255) << 24) | 0x000000);

		// 2) Blood-red wash on top of the dark.
		int redA = (int) (0.55f * env * 255f);
		g.fill(0, 0, w, h, (Mth.clamp(redA, 0, 255) << 24) | 0x4A0008);

		// 3) Heavy crimson vignette pulsing in from the edges.
		int vig = (int) (0.85f * env * 255f);
		int vignette = (Mth.clamp(vig, 0, 255) << 24) | 0x1A0004;
		int band = Math.max(8, h / 4);
		g.fillGradient(0, 0, w, band, vignette, 0x00000000);
		g.fillGradient(0, h - band, w, h, 0x00000000, vignette);
		int side = Math.max(8, w / 5);
		g.fillGradient(0, 0, side, h, vignette, 0x00000000);
		// right side: gradient flows the other way, so emulate with a second band.
		g.fillGradient(w - side, 0, w, h, 0x00000000, vignette);
	}
}
