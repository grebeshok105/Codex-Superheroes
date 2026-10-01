package io.github.grebeshok105.codex.hero.regulus.vfx;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Regulus's Visual Core effect ids ({@code superheroes:regulus/<name>}) — one-shot
 * {@code VfxEventS2CPayload} effect ids sent via {@code VfxFx.event}, and the
 * {@code regulus/<channel>} channel ids sent via {@code VfxFx.channel}. The
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

	/** Debris-kick cast: starts the authored 2.20 s clip; the volley fires on {@link #DEBRIS_IMPACT}. */
	public static final ResourceLocation ANIM_DEBRIS_KICK = ModId.of("regulus/anim/debris_kick");
	/** Authored impact frame (~0.70 s into the clip): dust cone + shard burst + distortion. */
	public static final ResourceLocation DEBRIS_IMPACT = ModId.of("regulus/debris_impact");

	/** Mania-of-greed cast clip (2.10 s, authored effect frame 0.94 s). */
	public static final ResourceLocation ANIM_MANIA_OF_GREED_CAST = ModId.of("regulus/anim/mania_of_greed_cast");
	/** Counter-attack clip (0.90 s, authored impact 0.32 s), fired at the ARRIVE→SLAM transition. */
	public static final ResourceLocation ANIM_COUNTER_ATTACK = ModId.of("regulus/anim/counter_attack");
	/** Reserve: the authored standalone roar clip (2.60 s) — currently no emitter wires it. */
	public static final ResourceLocation ANIM_LION_ROAR = ModId.of("regulus/anim/lion_roar");

	/** Counter-attack lift: white screen flash. */
	public static final ResourceLocation COUNTER_LIFT = ModId.of("regulus/counter_lift");
	/** Visual slam impact — the old {@code level.explode} replacement (flash + light + ring + impulse). */
	public static final ResourceLocation COUNTER_SLAM_IMPACT = ModId.of("regulus/counter_slam_impact");

	/** Ritual begin: starts the authored {@code evangelium_activation} clip (3.40 s). */
	public static final ResourceLocation ANIM_EVANGELIUM_ACTIVATION = ModId.of("regulus/anim/evangelium_activation");
	/** Madness collapse: starts the authored {@code evangelium_deactivation} clip (2.00 s). */
	public static final ResourceLocation ANIM_EVANGELIUM_DEACTIVATION = ModId.of("regulus/anim/evangelium_deactivation");
	/** Tick 34 of the 60-tick ritual (authored ~1.66 s): the major flash — an event, not a clip restart. */
	public static final ResourceLocation EVANGELIUM_MAJOR = ModId.of("regulus/evangelium_major");

	/** Hard landing: the vanilla shockwave stays — this adds the distortion layer + dust. */
	public static final ResourceLocation LANDING = ModId.of("regulus/landing");

	/** Golden thread caster→victim while a greed magnet holds. */
	public static final ResourceLocation CHANNEL_GREED_MAGNET = ModId.of("regulus/greed_magnet");
	/** Glass stasis dome over the embraced anchor. */
	public static final ResourceLocation CHANNEL_GREED_STASIS = ModId.of("regulus/greed_stasis");
	/** Distortion dome around the player while Lion's Heart is active. */
	public static final ResourceLocation CHANNEL_LION_HEART_DOME = ModId.of("regulus/lion_heart_dome");
	/** Weak golden pulse above the marked heart bearers (owner-only view). */
	public static final ResourceLocation CHANNEL_HEART_PULSE = ModId.of("regulus/heart_pulse");
}
