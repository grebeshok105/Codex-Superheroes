package io.github.grebeshok105.codex.hero.thanos.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

public final class ThanosSounds {
	public static final SoundEvent THANOS_SNAP_VOICE = ModContent.sound("thanos.snap.voice");

	private ThanosSounds() {
	}

	/** Forces class initialization during onInitialize so the sound is registered. */
	public static void register() {
	}
}
