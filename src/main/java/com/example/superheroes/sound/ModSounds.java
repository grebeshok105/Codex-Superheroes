package com.example.superheroes.sound;

import com.example.superheroes.ModId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {


	public static final SoundEvent LIGHTNING_THUNDER_ANIME = register("lightning.thunder.anime");
	public static final SoundEvent LIGHTNING_THUNDER_LOUD = register("lightning.thunder.loud");
	public static final SoundEvent UNIBEAM_CHARGE = register("unibeam.charge");
	public static final SoundEvent UNIBEAM_BEAM = register("unibeam.beam");
	public static final SoundEvent UNIBEAM_BLAST = register("unibeam.blast");
	public static final SoundEvent HOMELANDER_ROAR = register("homelander.roar");
	public static final SoundEvent HOMELANDER_ROAR_DEEP = register("homelander.roar.deep");
	public static final SoundEvent HOMELANDER_HAND_CLAP = register("homelander.hand_clap");
	public static final SoundEvent HOMELANDER_IRON_FISTS_IMPACT = register("homelander.iron_fists.impact");
	public static final SoundEvent HOMELANDER_IRON_FISTS_CHARGE = register("homelander.iron_fists.charge");
	public static final SoundEvent DOOMSDAY_ROAR = register("doomsday.roar");
	public static final SoundEvent THANOS_SNAP_VOICE = register("thanos.snap.voice");
	public static final SoundEvent REINHARD_SWORD_STRIKE_VOICE = register("reinhard.sword_strike.voice");
	public static final SoundEvent REINHARD_SWORD_DRAW_CEREMONY = register("reinhard.sword_draw.ceremony");
	public static final SoundEvent HOMELANDER_OMNIMAN_REACT = register("homelander.omniman_react");
	public static final SoundEvent IRONMAN_JARVIS_DETECT = register("ironman.jarvis_detect");
	public static final SoundEvent IRONMAN_JARVIS_DETECT_EXCITED = register("ironman.jarvis_detect_excited");
	public static final SoundEvent IRONMAN_JARVIS_DIAGNOSTIC = register("ironman.jarvis_diagnostic");
	public static final SoundEvent IRONMAN_JARVIS_OUTDATED_SUIT = register("ironman.jarvis_outdated_suit");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		ResourceLocation id = ModId.of(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}
}
