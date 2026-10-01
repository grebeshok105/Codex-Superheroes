package io.github.grebeshok105.codex.hero.regulus.vfx;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Regulus's Visual Core effect ids ({@code superheroes:regulus/<name>}) — one-shot
 * {@code VfxEventS2CPayload} effect ids sent via {@code VfxFx.event}. The
 * {@code regulus/anim/<clip>} ids are the on-the-wire form of the authored EMF
 * clip ids ({@code animation.regulus.<clip>}), broadcast to tracking + self.
 * Client factories live in {@code client/hero/regulus/fx/RegulusFx}.
 */
public final class RegulusVfxIds {
	/** Ritual begin: starts the authored {@code evangelium_activation} clip (3.40 s). */
	public static final ResourceLocation ANIM_EVANGELIUM_ACTIVATION = ModId.of("regulus/anim/evangelium_activation");
	/** Madness collapse: starts the authored {@code evangelium_deactivation} clip (2.00 s). */
	public static final ResourceLocation ANIM_EVANGELIUM_DEACTIVATION = ModId.of("regulus/anim/evangelium_deactivation");
	/** Tick 34 of the 60-tick ritual (authored ~1.66 s): the major flash — an event, not a clip restart. */
	public static final ResourceLocation EVANGELIUM_MAJOR = ModId.of("regulus/evangelium_major");

	private RegulusVfxIds() {
	}
}
