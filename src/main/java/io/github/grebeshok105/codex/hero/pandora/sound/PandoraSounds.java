package io.github.grebeshok105.codex.hero.pandora.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

/** Pandora's sound events — leaf package so runtime code can reach them without importing the module root. */
public final class PandoraSounds {
	public static final SoundEvent PANDORA_CHILD_GIGGLE = ModContent.sound("pandora.child_giggle");
	public static final SoundEvent PANDORA_VANITY_REVIVE = ModContent.sound("pandora.vanity_revive");

	private PandoraSounds() {
	}

	/** Forces class initialization during module register so the sounds are registered. */
	public static void init() {
	}
}
