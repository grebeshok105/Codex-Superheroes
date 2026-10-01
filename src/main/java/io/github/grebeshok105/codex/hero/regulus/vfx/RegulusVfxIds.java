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
	private RegulusVfxIds() {
	}

	/** Greed's Embrace windup clip, emitted at cast start (acquire 0-13t, fire 18t). */
	public static final ResourceLocation ANIM_GREEDS_EMBRACE_CAST = ModId.of("regulus/anim/greeds_embrace_cast");

	/** Lion-heart cast: starts the authored 1.60s activation clip (trigger frame 0.70s). */
	public static final ResourceLocation ANIM_LION_HEART_ACTIVATION = ModId.of("regulus/anim/lion_heart_activation");

	/** Debris-kick activation: starts the authored kick clip on every client. */
	public static final ResourceLocation ANIM_DEBRIS_KICK = ModId.of("regulus/anim/debris_kick");
	/** Fire tick of the debris fan (~0.70 s in): the debris burst + impact bed. */
	public static final ResourceLocation DEBRIS_IMPACT = ModId.of("regulus/debris_impact");

	/** Mania of Greed windup clip (2.10 s), emitted at cast start; the magnet grabs at 0.94 s. */
	public static final ResourceLocation ANIM_MANIA_OF_GREED_CAST = ModId.of("regulus/anim/mania_of_greed_cast");
	/** Counter strike clip (0.90 s, impact @0.32 s), emitted late in ARRIVE so the hit frame meets the slam. */
	public static final ResourceLocation ANIM_COUNTER_ATTACK = ModId.of("regulus/anim/counter_attack");
	/** Visual slam impact (particles + shake + client flash) — the old {@code level.explode} replacement. */
	public static final ResourceLocation COUNTER_SLAM_IMPACT = ModId.of("regulus/counter_slam_impact");

	/** Ritual begin: starts the authored {@code evangelium_activation} clip (3.40 s). */
	public static final ResourceLocation ANIM_EVANGELIUM_ACTIVATION = ModId.of("regulus/anim/evangelium_activation");
	/** Madness collapse: starts the authored {@code evangelium_deactivation} clip (2.00 s). */
	public static final ResourceLocation ANIM_EVANGELIUM_DEACTIVATION = ModId.of("regulus/anim/evangelium_deactivation");
	/** Tick 34 of the 60-tick ritual (authored ~1.66 s): the major flash — an event, not a clip restart. */
	public static final ResourceLocation EVANGELIUM_MAJOR = ModId.of("regulus/evangelium_major");
}
