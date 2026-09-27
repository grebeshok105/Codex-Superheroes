package io.github.grebeshok105.codex.hero.captainamerica.entity;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Captain America's entity types — registered directly per R23 (no {@code ModEntities} indirection). */
public final class CaptainAmericaEntities {
	public static final EntityType<ShieldProjectileEntity> SHIELD_PROJECTILE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ModId.of("shield_projectile"),
			EntityType.Builder.<ShieldProjectileEntity>of(ShieldProjectileEntity::new, MobCategory.MISC)
					.sized(0.6f, 0.6f)
					.clientTrackingRange(10)
					.updateInterval(2)
					.build("shield_projectile")
	);

	private CaptainAmericaEntities() {
	}

	/** Forces class initialization during the module's {@code register} so the type exists. */
	public static void register() {
	}
}
