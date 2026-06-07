package com.example.superheroes.entity;

import com.example.superheroes.ModId;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<HomelanderBossEntity> HOMELANDER_BOSS = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("homelander_boss"),
			EntityType.Builder.of(HomelanderBossEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.95f)
					.clientTrackingRange(10)
					.build("homelander_boss")
	);

	public static final EntityType<ShadowSoldierEntity> SHADOW_SOLDIER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("shadow_soldier"),
			EntityType.Builder.of(ShadowSoldierEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.85f)
					.clientTrackingRange(10)
					.fireImmune()
					.build("shadow_soldier")
	);

	public static final EntityType<KageBunshinEntity> KAGE_BUNSHIN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("kage_bunshin"),
			EntityType.Builder.of(KageBunshinEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.fireImmune()
					.build("kage_bunshin")
	);

	public static final EntityType<ShieldProjectileEntity> SHIELD_PROJECTILE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("shield_projectile"),
			EntityType.Builder.<ShieldProjectileEntity>of(ShieldProjectileEntity::new, MobCategory.MISC)
					.sized(0.6f, 0.6f)
					.clientTrackingRange(10)
					.updateInterval(2)
					.build("shield_projectile")
	);

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(HOMELANDER_BOSS, HomelanderBossEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SHADOW_SOLDIER, ShadowSoldierEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KAGE_BUNSHIN, KageBunshinEntity.createAttributes());
	}
}
