package io.github.grebeshok105.codex.client.hero.homelander.emf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-entity playback bookkeeping for the generated EMF expressions: current
 * blend weights, target weights and clip-local times (in 60-fps frames).
 * Pure state + math — no Minecraft types — so the smoothing and timing rules
 * are unit-testable; {@code HomelanderEmfRuntime} mirrors these into the
 * entity's {@code var.*} map each frame.
 *
 * <p>Weights approach their targets exponentially with a configurable
 * half-life (same smoothing as the old {@code FlightPoseMath.step}). Times
 * advance only while {@code weight > 0} — spec rule so loops do not run
 * hidden — at 3 frames per client tick (60 fps / 20 tps).
 */
public final class EmfPlaybackState {
	public static final float FRAMES_PER_TICK = 3f;

	private final Map<String, Float> weights = new HashMap<>();
	private final Map<String, Float> targets = new HashMap<>();
	private final Map<String, Float> times = new HashMap<>();
	private final Map<String, Integer> lengths = new HashMap<>();
	private final Set<String> loopClips = new HashSet<>();

	private float halfLifeTicks = 3f;

	public void setHalfLifeTicks(float halfLifeTicks) {
		this.halfLifeTicks = halfLifeTicks;
	}

	public void registerClip(String name, int frameCount, boolean loop) {
		lengths.put(name, frameCount);
		if (loop) {
			loopClips.add(name);
		}
		times.putIfAbsent(name, 0f);
	}

	public void setTargetWeight(String name, float weight) {
		targets.put(name, clamp01(weight));
	}

	public float weight(String name) {
		return weights.getOrDefault(name, 0f);
	}

	public float target(String name) {
		return targets.getOrDefault(name, 0f);
	}

	public float time(String name) {
		return times.getOrDefault(name, 0f);
	}

	public boolean isLoop(String name) {
		return loopClips.contains(name);
	}

	/** Whether the given one-shot clip has played to its end. */
	public boolean oneShotFinished(String name) {
		Integer len = lengths.get(name);
		return len != null && !loopClips.contains(name) && time(name) >= len;
	}

	/** Restarts a clip's local time at frame 0. */
	public void restart(String name) {
		times.put(name, 0f);
	}

	/**
	 * Exponential approach toward {@code target}: after {@code halfLifeTicks}
	 * ticks the remaining gap halves. Shared by the lean smoothing in
	 * {@code HomelanderEmfRuntime} and {@code HomelanderFlightLean}.
	 */
	public static double smoothApproach(double current, double target, double ticks,
			double halfLifeTicks) {
		if (halfLifeTicks <= 0) {
			return target;
		}
		double a = 1.0 - Math.pow(0.5, ticks / halfLifeTicks);
		return current + (target - current) * a;
	}

	/**
	 * Advances smoothing and clip times by {@code ticks} game ticks
	 * (call with 1.0 once per client tick, or with partial ticks inside the
	 * render pass — weights only ever approach targets, never snap).
	 */
	public void advance(float ticks) {
		float approach = halfLifeTicks <= 0f
				? 1f
				: (float) (1.0 - Math.pow(0.5, ticks / halfLifeTicks));
		for (Map.Entry<String, Float> entry : targets.entrySet()) {
			String name = entry.getKey();
			float current = weight(name);
			float next = current + (entry.getValue() - current) * approach;
			weights.put(name, clamp01(Math.abs(next) < 1e-4f ? 0f : next));
		}
		for (Map.Entry<String, Float> entry : times.entrySet()) {
			String name = entry.getKey();
			if (weight(name) <= 0f) {
				continue;
			}
			float t = entry.getValue() + FRAMES_PER_TICK * ticks;
			Integer loopLen = lengths.get(name);
			if (loopClips.contains(name) && loopLen != null && loopLen > 0) {
				t %= loopLen;
			}
			entry.setValue(t);
		}
	}

	public void reset() {
		weights.clear();
		targets.clear();
		times.clear();
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(v, 1f);
	}
}
