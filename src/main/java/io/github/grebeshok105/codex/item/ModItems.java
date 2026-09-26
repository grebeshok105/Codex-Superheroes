package io.github.grebeshok105.codex.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneItem;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

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

	public static final VoughtSignalItem VOUGHT_SIGNAL = register(
			"vought_signal",
			new VoughtSignalItem(new Item.Properties().stacksTo(4).rarity(Rarity.EPIC))
	);

	public static final SpawnEggItem HOMELANDER_BOSS_SPAWN_EGG = register(
			"homelander_boss_spawn_egg",
			new SpawnEggItem(ModEntities.HOMELANDER_BOSS, 0x2FB200, 0x6BD43A,
					new Item.Properties().rarity(Rarity.EPIC))
	);

	public static final ShadowMonarchsCloakItem SHADOW_MONARCHS_CLOAK = register(
			"shadow_monarchs_cloak",
			new ShadowMonarchsCloakItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final TransformationItem DOOMSDAY_GENOME = register(
			"doomsday_genome",
			new TransformationItem(ModId.of("doomsday"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_PURPLE,
							List.of(new TransformationLore.Line("item.superheroes.doomsday_genome.lore.line1", ChatFormatting.LIGHT_PURPLE),
									new TransformationLore.Line("item.superheroes.doomsday_genome.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.doomsday_genome.lore.usage", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.doomsday_genome.lore.untransform", ChatFormatting.GOLD))))
	);

	public static final TransformationItem GOKU_GI = register(
			"goku_gi",
			new TransformationItem(ModId.of("goku"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.goku_gi.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.goku_gi.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.goku_gi.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.goku_gi.lore.untransform", ChatFormatting.RED))))
	);

	public static final NarutoHeadbandItem NARUTO_HEADBAND = register(
			"naruto_headband",
			new NarutoHeadbandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final CaptainAmericaSuitItem CAPTAIN_AMERICA_SUIT = register(
			"captain_america_suit",
			new CaptainAmericaSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final VibraniumShieldItem VIBRANIUM_SHIELD = register(
			"vibranium_shield",
			new VibraniumShieldItem(new Item.Properties().stacksTo(1).durability(2000).rarity(Rarity.EPIC))
	);

	public static final BladeOfChaosItem BLADE_OF_CHAOS = register(
			"blade_of_chaos",
			new BladeOfChaosItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final LokiScepterItem LOKI_SCEPTER = register(
			"loki_scepter",
			new LokiScepterItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
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

	public static final KryptoniteShardItem KRYPTONITE_SHARD = register(
			"kryptonite_shard",
			new KryptoniteShardItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE))
	);

	public static final TransformationItem REINHARD_SUIT = register(
			"reinhard_suit",
			new TransformationItem(ModId.of("reinhard"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.reinhard_suit.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.reinhard_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.reinhard_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.reinhard_suit.lore.untransform", ChatFormatting.RED))))
	);

	public static final RoyalIcicleItem ROYAL_ICICLE = register(
			"royal_icicle",
			new RoyalIcicleItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final TransformationItem RAIDEN_SUIT = register(
			"raiden_suit",
			new TransformationItem(ModId.of("raiden_shogun"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.LIGHT_PURPLE,
							List.of(new TransformationLore.Line("item.superheroes.raiden_suit.lore.line1", ChatFormatting.LIGHT_PURPLE),
									new TransformationLore.Line("item.superheroes.raiden_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.raiden_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.raiden_suit.lore.untransform", ChatFormatting.RED))))
	);

	public static final TransformationItem INVINCIBLE_SUIT = register(
			"invincible_suit",
			new TransformationItem(ModId.of("invincible"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.BLUE,
							List.of(new TransformationLore.Line("item.superheroes.invincible_suit.lore.line1", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.invincible_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.invincible_suit.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.invincible_suit.lore.untransform", ChatFormatting.RED))))
	);

	public static final TransformationItem OMNIMAN_SUIT = register(
			"omniman_suit",
			new TransformationItem(ModId.of("omniman"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_RED,
							List.of(new TransformationLore.Line("item.superheroes.omniman_suit.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.omniman_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.omniman_suit.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.omniman_suit.lore.untransform", ChatFormatting.RED))))
	);

	public static final TransformationItem KAZUHA_VISION = register(
			"kazuha_vision",
			new TransformationItem(ModId.of("kazuha"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GREEN,
							List.of(new TransformationLore.Line("item.superheroes.kazuha_vision.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.kazuha_vision.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.kazuha_vision.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.kazuha_vision.lore.untransform", ChatFormatting.RED))))
	);

	public static final TransformationItem SCARAMOUCHE_VISION = register(
			"scaramouche_vision",
			new TransformationItem(ModId.of("scaramouche"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_AQUA,
							List.of(new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.line1", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.untransform", ChatFormatting.RED))))
	);

	public static final TransformationItem BATTLE_BEAST_MEDALLION = register(
			"battle_beast_medallion",
			new TransformationItem(ModId.of("battle_beast"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_RED,
							List.of(new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.untransform", ChatFormatting.RED))))
	);

	public static final TransformationItem REM_ONI_HORN = register(
			"rem_oni_horn",
			new TransformationItem(ModId.of("rem"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.BLUE,
							List.of(new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.line1", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.untransform", ChatFormatting.RED))))
	);

	public static final RemMorningStarItem REM_MORNING_STAR = register(
			"rem_morning_star",
			new RemMorningStarItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final TransformationItem A_TRAIN_SUIT = register(
			"a_train_suit",
			new TransformationItem(ModId.of("a_train"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.BLUE,
							List.of(new TransformationLore.Line("item.superheroes.a_train_suit.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.a_train_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.a_train_suit.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.a_train_suit.lore.untransform", ChatFormatting.RED))))
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

	public static final MusouNoHitotachiItem MUSOU_NO_HITOTACHI = register(
			"musou_no_hitotachi",
			new MusouNoHitotachiItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final io.github.grebeshok105.codex.horde.HordeCrystalItem HORDE_CRYSTAL = register(
			"horde_crystal",
			new io.github.grebeshok105.codex.horde.HordeCrystalItem()
	);

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(BuiltInRegistries.ITEM, ModId.of(name), item);
	}

	public static void init() {
	}
}
