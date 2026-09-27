package io.github.grebeshok105.codex.hero.sungjinwoo.entity;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Sung Jin-Woo's entity types — registered directly per R23 (no {@code ModEntities} indirection). */
public final class SungJinwooEntities {
	public static final EntityType<ShadowSoldierEntity> SHADOW_SOLDIER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("shadow_soldier"),
			EntityType.Builder.of(ShadowSoldierEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.85f)
					.clientTrackingRange(10)
					.fireImmune()
					.build("shadow_soldier")
	);

	private SungJinwooEntities() {
	}

	/** Registers the entity attributes; also forces class init so the type exists. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(SHADOW_SOLDIER, ShadowSoldierEntity.createAttributes());
	}
}
