package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * One hero's opt-in to continuous flight presentation: the clips the pose
 * tracker crossfades on {@code PlayerAnimator.Layer.BASE} (takeoff/land ride
 * ACTION as one-shots), the one-shot effects spawned while flying, and the
 * entity-bound sounds. {@code trailEffect} runs while CRUISE/BOOST and
 * finishes itself when the phase leaves; {@code boostEffect} fires once on
 * BOOST entry; {@code speedRingEffect} fires on hard acceleration events
 * (cooldown-gated in the tracker). All are {@code VfxRuntime} effect ids and
 * may be {@code null} when the hero wants no effect there.
 */
public record FlightPresentation(
		ResourceLocation takeoffClip,
		ResourceLocation hoverClip,
		ResourceLocation cruiseClip,
		ResourceLocation boostClip,
		ResourceLocation landClip,
		@Nullable ResourceLocation trailEffect,
		@Nullable ResourceLocation boostEffect,
		@Nullable ResourceLocation speedRingEffect,
		SoundEvent loopSound,
		SoundEvent takeoffSound,
		SoundEvent boostSound,
		SoundEvent landSound) {
}
