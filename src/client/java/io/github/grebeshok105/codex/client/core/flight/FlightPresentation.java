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
 * BOOST entry. Both are {@code VfxRuntime} effect ids and may be
 * {@code null} when the hero wants no effect there.
 *
 * <p>Clip ids may be {@code null} individually: a hero whose model pose is
 * driven elsewhere (EMF) still gets sounds and effects with no clip plays.
 * {@code poseParams} is an optional {@code vfx/<path>} params id layered over
 * {@code flight/pose} when computing the pose target for players EMF owns —
 * it carries authored extras such as {@code emfBoostRootPitch}. It is
 * ignored for players EMF does not own.
 */
public record FlightPresentation(
		@Nullable ResourceLocation takeoffClip,
		@Nullable ResourceLocation hoverClip,
		@Nullable ResourceLocation cruiseClip,
		@Nullable ResourceLocation boostClip,
		@Nullable ResourceLocation landClip,
		@Nullable ResourceLocation trailEffect,
		@Nullable ResourceLocation boostEffect,
		SoundEvent loopSound,
		SoundEvent takeoffSound,
		SoundEvent boostSound,
		SoundEvent landSound,
		@Nullable ResourceLocation poseParams) {
}
