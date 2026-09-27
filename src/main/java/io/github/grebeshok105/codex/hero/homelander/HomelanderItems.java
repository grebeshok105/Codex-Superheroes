package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.homelander.item.MilkBottleItem;
import io.github.grebeshok105.codex.hero.homelander.item.UraniumDaggerItem;
import io.github.grebeshok105.codex.hero.homelander.item.UraniumIsotopeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class HomelanderItems {
	public static final TransformationItem HOMELANDER_SUIT = ModContent.item(
			"homelander_suit",
			new TransformationItem(ModId.of("homelander"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_RED,
							List.of(new TransformationLore.Line("item.superheroes.homelander_suit.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.homelander_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.homelander_suit.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.homelander_suit.lore.untransform", ChatFormatting.YELLOW))))
	);

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
		content.creativeTab(HOMELANDER_SUIT);
		content.creativeTab(MILK_BOTTLE);
		content.creativeTab(URANIUM_ISOTOPE);
		content.creativeTab(URANIUM_DAGGER);
	}
}
