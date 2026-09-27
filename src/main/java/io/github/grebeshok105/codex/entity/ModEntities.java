package io.github.grebeshok105.codex.entity;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.ironman.entity.IronLegionDroneEntity;
import io.github.grebeshok105.codex.hero.ironman.entity.SmartMissileEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {

	public static final EntityType<IronLegionDroneEntity> IRON_LEGION_DRONE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("iron_legion_drone"),
			EntityType.Builder.of(IronLegionDroneEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.85f)
					.clientTrackingRange(10)
					.fireImmune()
					.build("iron_legion_drone")
	);

	public static final EntityType<SmartMissileEntity> SMART_MISSILE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("smart_missile"),
			EntityType.Builder.<SmartMissileEntity>of(SmartMissileEntity::new, MobCategory.MISC)
					.sized(0.4f, 0.4f)
					.clientTrackingRange(8)
					.updateInterval(1)
					.build("smart_missile")
	);

	private ModEntities() {
	}

	public static void init() {

		FabricDefaultAttributeRegistry.register(IRON_LEGION_DRONE, IronLegionDroneEntity.createAttributes());
	}
}
