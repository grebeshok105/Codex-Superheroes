package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.homelander.item.MilkBottleItem;
import io.github.grebeshok105.codex.hero.homelander.item.UraniumDaggerItem;
import io.github.grebeshok105.codex.hero.homelander.item.UraniumIsotopeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class HomelanderItems {
	public static final MilkBottleItem MILK_BOTTLE = ModContent.item(
			"milk_bottle",
			new MilkBottleItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE))
	);

	public static final UraniumIsotopeItem URANIUM_ISOTOPE = ModContent.item(
			"uranium_isotope",
			new UraniumIsotopeItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE))
	);

	public static final UraniumDaggerItem URANIUM_DAGGER = ModContent.item(
			"uranium_dagger",
			new UraniumDaggerItem(new Item.Properties().stacksTo(1).durability(250).rarity(Rarity.EPIC))
	);

	private HomelanderItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(MILK_BOTTLE);
		content.creativeTab(URANIUM_ISOTOPE);
		content.creativeTab(URANIUM_DAGGER);
	}
}
