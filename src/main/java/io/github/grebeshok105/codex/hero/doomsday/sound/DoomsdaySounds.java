package io.github.grebeshok105.codex.hero.doomsday.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

/** Doomsday's sound events — leaf package so runtime code can reach them without importing the module root. */
public final class DoomsdaySounds {
	public static final SoundEvent DOOMSDAY_ROAR = ModContent.sound("doomsday.roar");

	private DoomsdaySounds() {
	}

	/** Forces class initialization during module register so the sounds are registered. */
	public static void init() {
	}
}
