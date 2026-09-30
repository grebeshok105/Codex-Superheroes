package io.github.grebeshok105.codex.core.content;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Vanilla-registry entry points shared by hero modules. */
public final class ModContent {
	private ModContent() {
	}

	public static <T extends Item> T item(String path, T item) {
		return Registry.register(BuiltInRegistries.ITEM, ModId.of(path), item);
	}

	public static <T extends Block> T block(String path, T block) {
		return Registry.register(BuiltInRegistries.BLOCK, ModId.of(path), block);
	}

	public static SoundEvent sound(String path) {
		ResourceLocation id = ModId.of(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
