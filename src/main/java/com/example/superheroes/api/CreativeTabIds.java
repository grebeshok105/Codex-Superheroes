package com.example.superheroes.api;

import com.example.superheroes.ModId;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Stable IDs for the Superheroes mod's creative-mode tabs. Addons can use
 * these as the target of
 * {@code ItemGroupEvents.modifyEntriesEvent(...)} to add their own items to
 * the existing Superheroes tab without creating a competing one.
 *
 * <p>Example:
 * <pre>{@code
 * ItemGroupEvents.modifyEntriesEvent(CreativeTabIds.SUPERHEROES_TAB)
 *     .register(entries -> entries.accept(MyItems.MY_ITEM));
 * }</pre>
 */
public final class CreativeTabIds {
	/**
	 * Main Superheroes creative tab. Resolves to
	 * {@code superheroes:superheroes}.
	 */
	public static final ResourceKey<CreativeModeTab> SUPERHEROES_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, ModId.of("superheroes"));

	private CreativeTabIds() {
	}
}
