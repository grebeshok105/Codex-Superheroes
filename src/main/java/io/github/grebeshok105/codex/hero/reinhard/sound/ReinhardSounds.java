package io.github.grebeshok105.codex.hero.reinhard.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

/** Reinhard's sound events — leaf package so runtime code can reach them without importing the module root. */
public final class ReinhardSounds {
	public static final SoundEvent REINHARD_SWORD_STRIKE_VOICE = ModContent.sound("reinhard.sword_strike.voice");
	public static final SoundEvent REINHARD_SWORD_DRAW_CEREMONY = ModContent.sound("reinhard.sword_draw.ceremony");

	private ReinhardSounds() {
	}

	/** Forces class initialization during module register so the sounds are registered. */
	public static void init() {
	}
}
