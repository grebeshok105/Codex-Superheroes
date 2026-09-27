package io.github.grebeshok105.codex.hero.ironman.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

/**
 * Iron Man's sound events — byte-identical to the rows {@code ModSounds} used to own.
 * Registered eagerly by {@code IronManModule.register}.
 */
public final class IronManSounds {
	public static final SoundEvent UNIBEAM_CHARGE = register("unibeam.charge");
	public static final SoundEvent UNIBEAM_BEAM = register("unibeam.beam");
	public static final SoundEvent UNIBEAM_BLAST = register("unibeam.blast");
	public static final SoundEvent IRONMAN_JARVIS_DETECT = register("ironman.jarvis_detect");
	public static final SoundEvent IRONMAN_JARVIS_DETECT_EXCITED = register("ironman.jarvis_detect_excited");
	public static final SoundEvent IRONMAN_JARVIS_DIAGNOSTIC = register("ironman.jarvis_diagnostic");
	public static final SoundEvent IRONMAN_JARVIS_OUTDATED_SUIT = register("ironman.jarvis_outdated_suit");
	public static final SoundEvent IRONMAN_JARVIS_MARK85_PRESET = register("ironman.jarvis_mark85_preset");
	public static final SoundEvent IRONMAN_JARVIS_LEGION_LAUNCH = register("ironman.jarvis_legion_launch");

	private IronManSounds() {
	}

	private static SoundEvent register(String name) {
		return ModContent.sound(name);
	}

	/** Forces class initialization during module register. */
	public static void init() {
	}
}
