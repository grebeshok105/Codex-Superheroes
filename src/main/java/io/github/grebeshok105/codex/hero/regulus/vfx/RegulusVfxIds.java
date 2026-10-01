package io.github.grebeshok105.codex.hero.regulus.vfx;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Regulus visual-identity effect ids (Amendment A). Abilities and runtime
 * controllers reference these constants; the client maps each id to its
 * {@code ctx.vfx} factory in {@code RegulusClientModule}.
 */
public final class RegulusVfxIds {
	private RegulusVfxIds() {
	}

	/** Greed's Embrace windup clip, emitted at cast start (acquire 0-13t, fire 18t). */
	public static final ResourceLocation ANIM_GREEDS_EMBRACE_CAST = ModId.of("regulus/anim/greeds_embrace_cast");
}
