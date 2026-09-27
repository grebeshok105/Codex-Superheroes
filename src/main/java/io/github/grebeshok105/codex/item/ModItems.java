package io.github.grebeshok105.codex.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class ModItems {
	public static final TransformationItem HOMELANDER_SUIT = register(
			"homelander_suit",
			new TransformationItem(ModId.of("homelander"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_RED,
							List.of(new TransformationLore.Line("item.superheroes.homelander_suit.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.homelander_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.homelander_suit.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.homelander_suit.lore.untransform", ChatFormatting.YELLOW))))
	);

	public static final TransformationItem IRON_MAN_SUIT = register(
			"iron_man_suit",
			new TransformationItem(ModId.of("iron_man"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.iron_man_suit.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.iron_man_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.iron_man_suit.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.iron_man_suit.lore.untransform", ChatFormatting.RED))))
	);

	public static final CompoundVItem COMPOUND_V = register(
			"compound_v",
			new CompoundVItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))
	);

	public static final MilkBottleItem MILK_BOTTLE = register(
			"milk_bottle",
			new MilkBottleItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE))
	);

	public static final IronManReactorItem IRON_MAN_REACTOR = register(
			"iron_man_reactor",
			new IronManReactorItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE))
	);

	public static final UraniumIsotopeItem URANIUM_ISOTOPE = register(
			"uranium_isotope",
			new UraniumIsotopeItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE))
	);

	public static final UraniumDaggerItem URANIUM_DAGGER = register(
			"uranium_dagger",
			new UraniumDaggerItem(new Item.Properties().stacksTo(1).durability(250).rarity(Rarity.EPIC))
	);

	public static final TransformationItem REGULUS_SUIT = register(
			"regulus_suit",
			new TransformationItem(ModId.of("regulus"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.regulus_suit.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.regulus_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.regulus_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.regulus_suit.lore.untransform", ChatFormatting.GOLD))))
	);

	public static final EvangelionItem EVANGELION = register(
			"evangelion",
			new EvangelionItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(BuiltInRegistries.ITEM, ModId.of(name), item);
	}

	public static void init() {
	}
}
