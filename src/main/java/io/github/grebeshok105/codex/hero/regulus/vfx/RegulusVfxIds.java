package io.github.grebeshok105.codex.hero.regulus.vfx;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Regulus's Visual Core effect/channel ids ({@code superheroes:regulus/<name>}).
 * {@code regulus/anim/<clip>} ids carry authored EMF clips one-to-one (Amendment A) —
 * server code sends them via {@code VfxFx}; the client factories are wired by the
 * hero-local {@code RegulusFx} hub.
 */
public final class RegulusVfxIds {
	private RegulusVfxIds() {
	}

	/** Greed's Embrace windup clip, emitted at cast start (acquire 0-13t, fire 18t). */
	public static final ResourceLocation ANIM_GREEDS_EMBRACE_CAST = ModId.of("regulus/anim/greeds_embrace_cast");

	/** Lion-heart cast: starts the authored 1.60s activation clip (trigger frame 0.70s). */
	public static final ResourceLocation ANIM_LION_HEART_ACTIVATION = ModId.of("regulus/anim/lion_heart_activation");
}
