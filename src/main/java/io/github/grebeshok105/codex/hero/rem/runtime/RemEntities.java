package io.github.grebeshok105.codex.hero.rem.runtime;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Rem's entity types — registered directly per R23 (no {@code ModEntities} indirection).
 * Lives in {@code runtime/} because {@link RamEntity} and {@link RamCompanionController}
 * reference each other: a separate {@code entity/} leaf would form a bidirectional
 * package pair.
 */
public final class RemEntities {
	public static final EntityType<RamEntity> RAM = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("ram"),
			EntityType.Builder.of(RamEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.build("ram")
	);

	private RemEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(RAM, RamEntity.createAttributes());
	}
}
