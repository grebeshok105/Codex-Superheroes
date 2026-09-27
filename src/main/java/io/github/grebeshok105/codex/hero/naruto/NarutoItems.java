package io.github.grebeshok105.codex.hero.naruto;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.naruto.item.NarutoHeadbandItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class NarutoItems {
	public static final NarutoHeadbandItem NARUTO_HEADBAND = ModContent.item("naruto_headband",
			new NarutoHeadbandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

	private NarutoItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(NARUTO_HEADBAND);
	}
}
