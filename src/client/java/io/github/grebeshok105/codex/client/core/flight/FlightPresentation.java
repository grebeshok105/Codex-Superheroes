package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * One hero's opt-in to continuous flight presentation — the EMF-era shape:
 * only the audio/effect config survives, because clip playback moved into
 * the hero's own animation runtime (Homelander drives the
 * {@code takeoff}/{@code hover}/{@code boost} EMF clips through
 * {@code HomelanderFlightDriver}; the state machine used to live in the
 * deleted {@code FlightPoseTracker}).
 *
 * <p>{@code trailEffect} runs while CRUISE/BOOST and finishes itself when
 * the phase leaves; {@code boostEffect} fires once on BOOST entry. Both are
 * {@code VfxRuntime} effect ids and may be {@code null} when the hero wants
 * no effect there.
 */
public record FlightPresentation(
		@Nullable ResourceLocation trailEffect,
		@Nullable ResourceLocation boostEffect,
		SoundEvent loopSound,
		SoundEvent takeoffSound,
		SoundEvent boostSound,
		SoundEvent landSound) {
}
