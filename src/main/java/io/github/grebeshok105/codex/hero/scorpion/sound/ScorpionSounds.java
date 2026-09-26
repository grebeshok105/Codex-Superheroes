package io.github.grebeshok105.codex.hero.scorpion.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

public final class ScorpionSounds {
	public static final SoundEvent GET_OVER_HERE = ModContent.sound("scorpion.get_over_here");

	private ScorpionSounds() {
	}

	/** Forces class initialization during onInitialize so the sound is registered. */
	public static void register() {
	}
}
