package io.github.grebeshok105.codex.client.core.anim;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A parsed Bedrock player animation: id ({@code <ns>:<hero>/<clip>} derived from the
 * {@code animation.<ns>.<hero>.<clip>} key), declared length, loop mode, per-bone tracks
 * and named timeline events in seconds ({@code sound_effects} / {@code particle_effects} /
 * {@code events}; the contract's {@code events: {"contact": ms}} rows reach the runtime
 * through {@link #eventTimes}).
 */
public record AnimationClip(ResourceLocation id, float lengthSeconds, Loop loop,
		Map<String, BoneTrack> bones, Map<String, Float> eventTimes) {

	/**
	 * {@code OFF} plays once and releases; {@code WRAP} ({@code "loop": true}) wraps at the
	 * length; {@code HOLD} ({@code "loop": "hold_on_last_frame"}) never ends and keeps the
	 * final pose until stopped.
	 */
	public enum Loop {
		OFF, WRAP, HOLD
	}

	public AnimationClip {
		bones = Map.copyOf(bones);
		eventTimes = Map.copyOf(eventTimes);
	}
}
