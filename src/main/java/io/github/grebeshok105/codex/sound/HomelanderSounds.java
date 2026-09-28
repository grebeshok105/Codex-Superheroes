package io.github.grebeshok105.codex.sound;

import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.sounds.SoundEvent;

/**
 * Homelander-owned sound events. They live in the shared {@code sound} package rather
 * than {@code hero.homelander.sound} because {@code content.boss.homelander} plus the
 * invincible, doomsday and raiden heroes borrow these events, and ArchUnit forbids both
 * {@code content.** -> hero.**} and hero -> hero references. {@code homelander.omniman_react}
 * deliberately stays in {@link ModSounds}: it is shared by {@code HomelanderReactionRule}
 * and {@code OmnimanReactionRule}, so it is not Homelander-owned.
 */
public final class HomelanderSounds {
	public static final SoundEvent ROAR = ModContent.sound("homelander.roar");
	public static final SoundEvent ROAR_DEEP = ModContent.sound("homelander.roar.deep");
	public static final SoundEvent HAND_CLAP = ModContent.sound("homelander.hand_clap");
	public static final SoundEvent IRON_FISTS_IMPACT = ModContent.sound("homelander.iron_fists.impact");
	public static final SoundEvent IRON_FISTS_CHARGE = ModContent.sound("homelander.iron_fists.charge");
	public static final SoundEvent IRON_FISTS_ACTIVATE = ModContent.sound("homelander.iron_fists.activate");
	public static final SoundEvent FLIGHT_TAKEOFF = ModContent.sound("homelander.flight.takeoff");
	public static final SoundEvent FLIGHT_LOOP = ModContent.sound("homelander.flight.loop");
	public static final SoundEvent FLIGHT_BOOST = ModContent.sound("homelander.flight.boost");
	public static final SoundEvent FLIGHT_LAND = ModContent.sound("homelander.flight.land");
	public static final SoundEvent LASER_CHARGE = ModContent.sound("homelander.laser.charge");
	public static final SoundEvent LASER_LOOP = ModContent.sound("homelander.laser.loop");
	public static final SoundEvent LASER_RELEASE = ModContent.sound("homelander.laser.release");
	public static final SoundEvent MILK_DRINK = ModContent.sound("homelander.milk.drink");
	public static final SoundEvent SUN_CHARGE = ModContent.sound("homelander.sun.charge");
	public static final SoundEvent SUN_DETONATE = ModContent.sound("homelander.sun.detonate");

	private HomelanderSounds() {
	}

	/** Forces class initialization so every event is registered. */
	public static void init() {
	}
}
