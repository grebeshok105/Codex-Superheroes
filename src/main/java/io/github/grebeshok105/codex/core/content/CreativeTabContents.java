package io.github.grebeshok105.codex.core.content;

import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * Module-owned items appended to the mod's creative tab, in registration order.
 * {@code ModItemGroups} lists them after the legacy items and before the admin block.
 */
public final class CreativeTabContents {
	private static final List<ItemLike> ITEMS = new ArrayList<>();

	private CreativeTabContents() {
	}

	public static void add(ItemLike item) {
		ITEMS.add(item);
	}

	public static List<ItemLike> all() {
		return List.copyOf(ITEMS);
	}
}
