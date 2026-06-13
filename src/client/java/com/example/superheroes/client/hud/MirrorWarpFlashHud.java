package com.example.superheroes.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Black "into the Mirror Dimension" flash that hides the unavoidable Iris
 * pipeline-reload freeze.
 *
 * The Acid warp options (MODE/J) are compile-time {@code #define}s, so changing
 * them needs a full {@code Iris.reload()} which freezes the client for ~0.5-3s.
 * We can't make that smooth, so we hide it: fade the screen to solid black,
 * run the reload only once a fully-black frame has actually been presented (so
 * the freeze shows black, not the old frame), then fade back in already warped.
 *
 * Correctness must not depend on rendering: if rendering is stalled (a GUI
 * screen is open) {@link #fallbackTick()} runs the pending reload from the
 * client tick instead, just without the nice cover.
 */
public final class MirrorWarpFlashHud {

	private enum Phase {
		IDLE,
		COVERING,
		/** A full-black frame was drawn this frame; run the reload next frame. */
		BLACK_PRESENTED,
		REVEALING
	}

	private static final long COVER_MS = 200L;
	private static final long REVEAL_MS = 480L;
	/** Ticks to wait for rendering before forcing the reload without a cover. */
	private static final int FALLBACK_TICKS = 5;

	private static Phase phase = Phase.IDLE;
	private static long phaseStartMs;
	private static Runnable pendingAction;
	private static int ticksWaitingToRender;

	private MirrorWarpFlashHud() {
	}

	/** Fade to black, run {@code action} (the Iris reload) under cover, fade back. */
	public static void flashAndRun(Runnable action) {
		pendingAction = action;
		ticksWaitingToRender = 0;
		phase = Phase.COVERING;
		phaseStartMs = System.currentTimeMillis();
	}

	public static boolean isCovering() {
		return phase != Phase.IDLE;
	}

	/**
	 * Safety net from the client tick: if rendering hasn't advanced the cover
	 * (e.g. a screen is open) within a few ticks, run the pending reload anyway
	 * so the shader still applies.
	 */
	public static void fallbackTick() {
		if (pendingAction == null) {
			return;
		}
		ticksWaitingToRender++;
		if (ticksWaitingToRender > FALLBACK_TICKS) {
			runPending();
			phase = Phase.IDLE;
		}
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (phase == Phase.IDLE) {
			return;
		}
		ticksWaitingToRender = 0;
		long now = System.currentTimeMillis();
		float alpha;
		switch (phase) {
			case COVERING -> {
				float t = (now - phaseStartMs) / (float) COVER_MS;
				if (t >= 1f) {
					alpha = 1f;
					// This frame is fully black; once it is presented we run the
					// reload on the next frame so the freeze is hidden behind black.
					phase = Phase.BLACK_PRESENTED;
				} else {
					alpha = t;
				}
			}
			case BLACK_PRESENTED -> {
				// Previous (fully black) frame is now on screen -> safe to freeze.
				runPending();
				alpha = 1f;
				phase = Phase.REVEALING;
				phaseStartMs = System.currentTimeMillis();
			}
			case REVEALING -> {
				float t = (now - phaseStartMs) / (float) REVEAL_MS;
				if (t >= 1f) {
					phase = Phase.IDLE;
					return;
				}
				alpha = 1f - t;
			}
			default -> {
				return;
			}
		}
		drawBlack(graphics, alpha);
	}

	private static void runPending() {
		Runnable action = pendingAction;
		pendingAction = null;
		if (action != null) {
			try {
				action.run();
			} catch (Throwable ignored) {
				// The Iris bridge already guards itself; never let a render-thread
				// hiccup kill the HUD.
			}
		}
	}

	private static void drawBlack(GuiGraphics graphics, float alpha) {
		int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
		if (a <= 0) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		graphics.fill(0, 0, w, h, a << 24);
	}
}
