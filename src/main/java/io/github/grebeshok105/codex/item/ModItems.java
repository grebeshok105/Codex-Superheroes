package io.github.grebeshok105.codex.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneItem;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;
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

	public static final RegulusSuitItem REGULUS_SUIT = register(
			"regulus_suit",
			new RegulusSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final EvangelionItem EVANGELION = register(
			"evangelion",
			new EvangelionItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final ShadowMonarchsCloakItem SHADOW_MONARCHS_CLOAK = register(
			"shadow_monarchs_cloak",
			new ShadowMonarchsCloakItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final InfinityGauntletItem INFINITY_GAUNTLET = register(
			"infinity_gauntlet",
			new InfinityGauntletItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem POWER_STONE = register(
			InfinityStoneType.POWER.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.POWER, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem SPACE_STONE = register(
			InfinityStoneType.SPACE.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.SPACE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem REALITY_STONE = register(
			InfinityStoneType.REALITY.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.REALITY, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem SOUL_STONE = register(
			InfinityStoneType.SOUL.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.SOUL, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem TIME_STONE = register(
			InfinityStoneType.TIME.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.TIME, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem MIND_STONE = register(
			InfinityStoneType.MIND.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.MIND, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	// persisted id kept from the Doctor Strange era — do not rename the string.
	public static final TransformationItem PANDORA_SUIT = register(
			"doctor_strange_suit",
			new TransformationItem(ModId.of("pandora"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_RED,
							List.of(new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.untransform", ChatFormatting.GOLD))))
	);

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(BuiltInRegistries.ITEM, ModId.of(name), item);
	}

	public static void init() {
	}
}
