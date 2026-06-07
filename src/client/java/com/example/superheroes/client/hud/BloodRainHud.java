package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMadnessState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BloodRainHud {
	private static final int INITIAL_BURST = 15;
	private static final long DARKEN_FADE_MS = 3500L;

	private static final long SPAWN_INTERVAL_MIN_MS = 150L;
	private static final long SPAWN_INTERVAL_MAX_MS = 450L;
	private static final int SPAWN_BATCH_MIN = 1;
	private static final int SPAWN_BATCH_MAX = 3;

	private static volatile long startedMs = 0L;
	private static volatile boolean active = false;
	private static volatile long nextSpawnMs = 0L;
	private static final List<Drop> DROPS = new CopyOnWriteArrayList<>();
	private static final List<Splat> SPLATS = new CopyOnWriteArrayList<>();
	private static final Random RNG = new Random();

	private BloodRainHud() {
	}

	public static void trigger() {
		startedMs = System.currentTimeMillis();
		active = true;
		nextSpawnMs = 0L;
		DROPS.clear();
		SPLATS.clear();
		List<Drop> initial = new ArrayList<>(INITIAL_BURST);
		for (int i = 0; i < INITIAL_BURST; i++) {
			initial.add(spawnDrop(true));
		}
		DROPS.addAll(initial);
	}

	public static void clear() {
		active = false;
		startedMs = 0L;
		DROPS.clear();
		SPLATS.clear();
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		boolean madness = ClientMadnessState.isMadness();
		if (!madness && !active) return;
		if (!madness) {
			active = false;
			DROPS.clear();
			SPLATS.clear();
			return;
		}
		if (!active) {
			trigger();
		}
		long now = System.currentTimeMillis();
		int sw = graphics.guiWidth();
		int sh = graphics.guiHeight();

		long elapsed = startedMs == 0L ? 0L : now - startedMs;
		float darkenAlpha = 0f;
		if (elapsed < DARKEN_FADE_MS) {
			darkenAlpha = 0.45f * (1f - elapsed / (float) DARKEN_FADE_MS);
		}
		if (darkenAlpha > 0f) {
			int a = (int) (darkenAlpha * 255f);
			int topColor = (a << 24) | 0x00000000;
			int midColor = ((a * 2 / 3) << 24) | 0x00000000;
			int bottomColor = (a << 24) | 0x00000000;
			graphics.fillGradient(0, 0, sw, sh / 2, topColor, midColor);
			graphics.fillGradient(0, sh / 2, sw, sh, midColor, bottomColor);
		}

		if (nextSpawnMs == 0L) {
			nextSpawnMs = now + SPAWN_INTERVAL_MIN_MS
					+ RNG.nextInt((int) (SPAWN_INTERVAL_MAX_MS - SPAWN_INTERVAL_MIN_MS));
		}
		while (now >= nextSpawnMs) {
			int batch = SPAWN_BATCH_MIN + RNG.nextInt(SPAWN_BATCH_MAX - SPAWN_BATCH_MIN + 1);
			for (int i = 0; i < batch; i++) {
				DROPS.add(spawnDrop(false));
			}
			nextSpawnMs += SPAWN_INTERVAL_MIN_MS
					+ RNG.nextInt((int) (SPAWN_INTERVAL_MAX_MS - SPAWN_INTERVAL_MIN_MS));
		}

		// render splats (static, fading) behind drops
		Iterator<Splat> sit = SPLATS.iterator();
		List<Splat> splatRemove = null;
		while (sit.hasNext()) {
			Splat s = sit.next();
			long sAge = now - s.spawnedMs;
			if (sAge > s.lifetimeMs) {
				if (splatRemove == null) splatRemove = new ArrayList<>();
				splatRemove.add(s);
				continue;
			}
			float sFade = 1f - (sAge / (float) s.lifetimeMs);
			int sa = (int) (s.baseAlpha * sFade);
			if (sa < 4) continue;
			int core = (sa << 24) | 0x00900000;
			int edge = ((sa / 3) << 24) | 0x00600000;
			int cx = s.xPx;
			int cy = s.yPx;
			int r = s.radius;
			// rough splat: central blob + random pixels around
			graphics.fill(cx - r / 2, cy - 1, cx + r / 2, cy + 2, core);
			graphics.fill(cx - r, cy, cx + r, cy + 1, edge);
			for (int j = 0; j < s.splats.length; j++) {
				int[] p = s.splats[j];
				graphics.fill(cx + p[0], cy + p[1], cx + p[0] + p[2], cy + p[1] + p[3], edge);
			}
		}
		if (splatRemove != null) {
			SPLATS.removeAll(splatRemove);
		}

		Iterator<Drop> it = DROPS.iterator();
		List<Drop> toRemove = null;
		while (it.hasNext()) {
			Drop d = it.next();
			float age = (now - d.spawnedMs) / 1000f;
			// variable speed: accel slightly over time (gravity-like)
			float progress = age * d.speedPerSec + 0.002f * age * age;
			float yNorm = d.yStart + progress;
			// lateral oscillation for "running on glass" feel
			float xOsc = (float) Math.sin(age * d.wobbleFreq + d.wobblePhase) * d.wobbleAmp;
			float xNorm = d.xNorm + xOsc;
			if (yNorm > 1.15f) {
				if (toRemove == null) toRemove = new ArrayList<>();
				toRemove.add(d);
				// splat on leave
				int splatX = (int) (xNorm * sw);
				int splatY = sh - 2;
				SPLATS.add(makeSplat(splatX, splatY, d.baseAlpha));
				continue;
			}
			int xPx = (int) (xNorm * sw);
			int yPx = (int) (yNorm * sh);
			int alpha = Math.min(235, d.baseAlpha);
			if (alpha < 6) continue;
			renderDrop(graphics, d, xPx, yPx, alpha);
		}
		if (toRemove != null) {
			DROPS.removeAll(toRemove);
		}
	}

	private static void renderDrop(GuiGraphics graphics, Drop d, int xPx, int yPx, int alpha) {
		int head = (alpha << 24) | 0x00B30000;
		int mid = ((alpha * 3 / 4) << 24) | 0x00800000;
		int tail = ((alpha / 5) << 24) | 0x00400000;
		int w = d.width;
		int h = d.length;
		int trailH = Math.min(110, h * 3);

		switch (d.shape) {
			case STREAM -> {
				// tapering trail via fillGradient (opaque head -> transparent top)
				graphics.fillGradient(xPx - 1, yPx - trailH, xPx + 1, yPx, tail, mid);
				// thick head (droplet bulb)
				graphics.fill(xPx - w / 2, yPx, xPx + w - w / 2, yPx + h / 2, head);
				// rounded bottom (narrower)
				graphics.fill(xPx - (w - 1) / 2, yPx + h / 2, xPx + (w - 1) - (w - 1) / 2, yPx + h, head);
				// left/right trail shadow for glass-smear look
				graphics.fillGradient(xPx - w / 2 - 1, yPx - trailH / 2, xPx - w / 2, yPx, tail, mid);
				graphics.fillGradient(xPx + w - w / 2, yPx - trailH / 2, xPx + w - w / 2 + 1, yPx, tail, mid);
			}
			case BLOB -> {
				// short fat drip with soft halo
				int bw = Math.max(3, w + 1);
				int bh = Math.max(4, h / 2);
				graphics.fillGradient(xPx - bw / 2 - 1, yPx - trailH / 2, xPx + bw - bw / 2 + 1, yPx, tail, mid);
				graphics.fill(xPx - bw / 2, yPx, xPx + bw - bw / 2, yPx + bh, head);
				graphics.fill(xPx - bw / 2 - 1, yPx + 1, xPx - bw / 2, yPx + bh - 1, mid);
				graphics.fill(xPx + bw - bw / 2, yPx + 1, xPx + bw - bw / 2 + 1, yPx + bh - 1, mid);
			}
			case ZIGZAG -> {
				// segmented trail: alternating offsets giving a zig-zag smear
				int segments = Math.max(3, trailH / 6);
				int segH = trailH / segments;
				for (int i = 0; i < segments; i++) {
					int segY0 = yPx - trailH + i * segH;
					int segY1 = segY0 + segH;
					int segAlpha = (int) (tail >>> 24) + (int) (((mid >>> 24) - (tail >>> 24)) * i / (float) segments);
					int c = (segAlpha << 24) | 0x00700000;
					int offset = ((i % 2) == 0) ? 0 : 1;
					graphics.fill(xPx - 1 + offset, segY0, xPx + 1 + offset, segY1, c);
				}
				graphics.fill(xPx - w / 2, yPx, xPx + w - w / 2, yPx + h, head);
			}
			case SMEAR -> {
				// wide horizontal smear, like fresh blood on a glass window
				int sw2 = Math.max(3, w + 2);
				graphics.fillGradient(xPx - sw2, yPx - trailH / 3, xPx + sw2, yPx, tail, mid);
				graphics.fill(xPx - sw2 / 2, yPx, xPx + sw2 - sw2 / 2, yPx + h / 3, head);
				graphics.fill(xPx - sw2, yPx + 1, xPx + sw2, yPx + 2, mid);
			}
		}
	}

	private static Drop spawnDrop(boolean initialBurst) {
		float xNorm = RNG.nextFloat();
		float yStart = initialBurst ? -0.05f - RNG.nextFloat() * 0.25f : -0.08f - RNG.nextFloat() * 0.05f;
		float speed = 0.008f + RNG.nextFloat() * 0.022f;
		int width = 2 + RNG.nextInt(4);
		int length = 14 + RNG.nextInt(34);
		int alpha = 140 + RNG.nextInt(90);
		Shape shape = pickShape();
		float wobbleFreq = 1.5f + RNG.nextFloat() * 3.0f;
		float wobblePhase = RNG.nextFloat() * (float) Math.PI * 2f;
		float wobbleAmp = 0.002f + RNG.nextFloat() * 0.008f;
		return new Drop(xNorm, yStart, speed, width, length, alpha, shape,
				wobbleFreq, wobblePhase, wobbleAmp, System.currentTimeMillis());
	}

	private static Shape pickShape() {
		int n = RNG.nextInt(100);
		if (n < 55) return Shape.STREAM;
		if (n < 78) return Shape.BLOB;
		if (n < 92) return Shape.ZIGZAG;
		return Shape.SMEAR;
	}

	private static Splat makeSplat(int x, int y, int baseAlpha) {
		int count = 3 + RNG.nextInt(5);
		int[][] arr = new int[count][4];
		for (int i = 0; i < count; i++) {
			int dx = -5 + RNG.nextInt(11);
			int dy = -2 + RNG.nextInt(4);
			int w = 1 + RNG.nextInt(3);
			int h = 1 + RNG.nextInt(2);
			arr[i] = new int[]{dx, dy, w, h};
		}
		int radius = 4 + RNG.nextInt(5);
		long life = 700L + RNG.nextInt(900);
		return new Splat(x, y, radius, Math.min(200, baseAlpha), life, System.currentTimeMillis(), arr);
	}

	private enum Shape { STREAM, BLOB, ZIGZAG, SMEAR }

	private record Drop(
			float xNorm,
			float yStart,
			float speedPerSec,
			int width,
			int length,
			int baseAlpha,
			Shape shape,
			float wobbleFreq,
			float wobblePhase,
			float wobbleAmp,
			long spawnedMs
	) {
	}

	private record Splat(
			int xPx,
			int yPx,
			int radius,
			int baseAlpha,
			long lifetimeMs,
			long spawnedMs,
			int[][] splats
	) {
	}
}
