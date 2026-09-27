package io.github.grebeshok105.codex.hero.naruto.entity;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Naruto's entity types — registered directly per R23 (no {@code ModEntities} indirection). */
public final class NarutoEntities {
	public static final EntityType<KageBunshinEntity> KAGE_BUNSHIN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("kage_bunshin"),
			EntityType.Builder.of(KageBunshinEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.fireImmune()
					.build("kage_bunshin")
	);

	private NarutoEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(KAGE_BUNSHIN, KageBunshinEntity.createAttributes());
	}
}
