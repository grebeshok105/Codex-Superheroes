package io.github.grebeshok105.codex.hero.regulus.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

/** Regulus's sound events — leaf package so runtime code can reach them without importing the module root. */
public final class RegulusSounds {
	public static final SoundEvent DEBRIS_ROAR = ModContent.sound("regulus.debris_roar");
	public static final SoundEvent DEBRIS_IMPACT = ModContent.sound("regulus.debris_impact");

	private RegulusSounds() {
	}

	/** Forces class initialization during module register so the sounds are registered. */
	public static void init() {
	}
}
