package com.example.superheroes.horde.entity;

import com.example.superheroes.ModId;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class HordeEntities {
	public static final EntityType<HordeCrawlerEntity> CRAWLER = register("horde_crawler",
			EntityType.Builder.of(HordeCrawlerEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 0.5f).clientTrackingRange(10));
	public static final EntityType<HordeLurkerEntity> LURKER = register("horde_lurker",
			EntityType.Builder.of(HordeLurkerEntity::new, MobCategory.MONSTER)
					.sized(0.7f, 1.4f).clientTrackingRange(10));
	public static final EntityType<HordeSpitterEntity> SPITTER = register("horde_spitter",
			EntityType.Builder.of(HordeSpitterEntity::new, MobCategory.MONSTER)
					.sized(0.7f, 1.2f).clientTrackingRange(10));
	public static final EntityType<HordeSwooperEntity> SWOOPER = register("horde_swooper",
			EntityType.Builder.of(HordeSwooperEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 0.8f).clientTrackingRange(10));
	public static final EntityType<HordeStalkerEntity> STALKER = register("horde_stalker",
			EntityType.Builder.of(HordeStalkerEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.6f).clientTrackingRange(10));
	public static final EntityType<HordeInfectorEntity> INFECTOR = register("horde_infector",
			EntityType.Builder.of(HordeInfectorEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.0f).clientTrackingRange(10));
	public static final EntityType<HordeParasiticHoundEntity> PARASITIC_HOUND = register("horde_parasitic_hound",
			EntityType.Builder.of(HordeParasiticHoundEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 0.7f).clientTrackingRange(10));
	public static final EntityType<HordeInfectedZombieEntity> INFECTED_ZOMBIE = register("horde_infected_zombie",
			EntityType.Builder.of(HordeInfectedZombieEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.95f).clientTrackingRange(10));
	public static final EntityType<HordeInfectedSkeletonEntity> INFECTED_SKELETON = register("horde_infected_skeleton",
			EntityType.Builder.of(HordeInfectedSkeletonEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.99f).clientTrackingRange(10));
	public static final EntityType<HordeInfectedSpiderEntity> INFECTED_SPIDER = register("horde_infected_spider",
			EntityType.Builder.of(HordeInfectedSpiderEntity::new, MobCategory.MONSTER)
					.sized(1.4f, 0.9f).clientTrackingRange(10));
	public static final EntityType<HordeInfectedCreeperEntity> INFECTED_CREEPER = register("horde_infected_creeper",
			EntityType.Builder.of(HordeInfectedCreeperEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.7f).clientTrackingRange(10));
	public static final EntityType<HordeVoidParasiteEntity> VOID_PARASITE = register("horde_void_parasite",
			EntityType.Builder.of(HordeVoidParasiteEntity::new, MobCategory.MONSTER)
					.sized(0.7f, 1.3f).clientTrackingRange(10));
	public static final EntityType<HordeHollowVillagerEntity> HOLLOW_VILLAGER = register("horde_hollow_villager",
			EntityType.Builder.of(HordeHollowVillagerEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.95f).clientTrackingRange(10));
	public static final EntityType<HordeInfectedCattleEntity> INFECTED_CATTLE = register("horde_infected_cattle",
			EntityType.Builder.of(HordeInfectedCattleEntity::new, MobCategory.MONSTER)
					.sized(0.9f, 1.4f).clientTrackingRange(10));
	public static final EntityType<HordeBroodmotherEntity> BROODMOTHER = register("horde_broodmother",
			EntityType.Builder.of(HordeBroodmotherEntity::new, MobCategory.MONSTER)
					.sized(1.4f, 1.2f).clientTrackingRange(10));
	public static final EntityType<HordeCorruptedGolemEntity> CORRUPTED_GOLEM = register("horde_corrupted_golem",
			EntityType.Builder.of(HordeCorruptedGolemEntity::new, MobCategory.MONSTER)
					.sized(1.4f, 2.7f).clientTrackingRange(10));
	public static final EntityType<HordeHivemindEntity> HIVEMIND = register("horde_hivemind",
			EntityType.Builder.of(HordeHivemindEntity::new, MobCategory.MONSTER)
					.sized(1.0f, 2.0f).clientTrackingRange(10));
	public static final EntityType<HordeLeviathanEntity> LEVIATHAN = register("horde_leviathan",
			EntityType.Builder.of(HordeLeviathanEntity::new, MobCategory.MONSTER)
					.sized(2.0f, 3.0f).clientTrackingRange(16));
	public static final EntityType<InfectedHomelanderBossEntity> INFECTED_HOMELANDER = register("horde_infected_homelander",
			EntityType.Builder.of(InfectedHomelanderBossEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.95f).clientTrackingRange(16).fireImmune());

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
			String name, EntityType.Builder<T> builder) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, ModId.of(name), builder.build(name));
	}

	private HordeEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CRAWLER, HordeCrawlerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LURKER, HordeLurkerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPITTER, HordeSpitterEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SWOOPER, HordeSwooperEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STALKER, HordeStalkerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTOR, HordeInfectorEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PARASITIC_HOUND, HordeParasiticHoundEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED_ZOMBIE, HordeInfectedZombieEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED_SKELETON, HordeInfectedSkeletonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED_SPIDER, HordeInfectedSpiderEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED_CREEPER, HordeInfectedCreeperEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_PARASITE, HordeVoidParasiteEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HOLLOW_VILLAGER, HordeHollowVillagerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED_CATTLE, HordeInfectedCattleEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BROODMOTHER, HordeBroodmotherEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CORRUPTED_GOLEM, HordeCorruptedGolemEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HIVEMIND, HordeHivemindEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LEVIATHAN, HordeLeviathanEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED_HOMELANDER, InfectedHomelanderBossEntity.createAttributes());
	}
}
