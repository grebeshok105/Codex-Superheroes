package io.github.grebeshok105.codex.hero.regulus.vfx;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Regulus's Visual Core effect ids ({@code superheroes:regulus/<name>}).
 * Anim ids ({@code regulus/anim/<clip>}) name an authored EMF clip to play on the
 * caster; the rest are one-shot {@code VfxEventS2CPayload} effect ids. Server code
 * sends them via {@code VfxFx}; {@code client/hero/regulus/fx/RegulusFx} registers
 * the matching client factories.
 */
public final class RegulusVfxIds {
	/** Debris-kick activation: starts the authored kick clip on every client. */
	public static final ResourceLocation ANIM_DEBRIS_KICK = ModId.of("regulus/anim/debris_kick");
	/** Fire tick of the debris fan (~0.70 s in): the debris burst + impact bed. */
	public static final ResourceLocation DEBRIS_IMPACT = ModId.of("regulus/debris_impact");

	private RegulusVfxIds() {
	}
}
