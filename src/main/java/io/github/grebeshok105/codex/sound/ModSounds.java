package io.github.grebeshok105.codex.sound;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {


	public static final SoundEvent LIGHTNING_THUNDER_ANIME = register("lightning.thunder.anime");
	public static final SoundEvent LIGHTNING_THUNDER_LOUD = register("lightning.thunder.loud");
	// Shared by HomelanderReactionRule and OmnimanReactionRule — not Homelander-owned,
	// so it stays here while the Homelander-owned events moved to HomelanderSounds.
	public static final SoundEvent HOMELANDER_OMNIMAN_REACT = register("homelander.omniman_react");
	// Valid Holder<SoundEvent> backed by an empty sounds.json entry — plays nothing.
	public static final SoundEvent SILENT = register("silent");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		ResourceLocation id = ModId.of(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}
}
