package com.example.superheroes.item;

import com.example.superheroes.ModId;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.item.infinity.InfinityStoneItem;
import com.example.superheroes.item.infinity.InfinityStoneType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final HomelanderSuitItem HOMELANDER_SUIT = register(
			"homelander_suit",
			new HomelanderSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final IronManSuitItem IRON_MAN_SUIT = register(
			"iron_man_suit",
			new IronManSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
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

	public static final DoomsdaySuitItem DOOMSDAY_GENOME = register(
			"doomsday_genome",
			new DoomsdaySuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final GokuGiItem GOKU_GI = register(
			"goku_gi",
			new GokuGiItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
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

	public static final ReinhardSuitItem REINHARD_SUIT = register(
			"reinhard_suit",
			new ReinhardSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final RoyalIcicleItem ROYAL_ICICLE = register(
			"royal_icicle",
			new RoyalIcicleItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final RaidenSuitItem RAIDEN_SUIT = register(
			"raiden_suit",
			new RaidenSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final MusouNoHitotachiItem MUSOU_NO_HITOTACHI = register(
			"musou_no_hitotachi",
			new MusouNoHitotachiItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(BuiltInRegistries.ITEM, ModId.of(name), item);
	}

	public static void init() {
	}
}
