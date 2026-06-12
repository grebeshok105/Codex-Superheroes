package com.example.superheroes.horde.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Base for horde "bomb" projectiles (acid / fire). They arc toward the target
 * and detonate on first contact, producing an area effect but NEVER breaking
 * blocks (per the mod rule: abilities/projectiles don't destroy terrain).
 *
 * Rendered client-side as a thrown item ({@link ThrowableItemProjectile} →
 * ThrownItemRenderer), so concrete subclasses pick a display item and add their
 * own trail + detonation visuals.
 */
public abstract class HordeBombEntity extends ThrowableItemProjectile {

	protected HordeBombEntity(EntityType<? extends HordeBombEntity> type, Level level) {
		super(type, level);
	}

	protected HordeBombEntity(EntityType<? extends HordeBombEntity> type, LivingEntity shooter, Level level) {
		super(type, shooter, level);
	}

	/** Slight arc so bombs lob instead of flying flat. */
	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			spawnTrail();
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!level().isClientSide()) {
			detonate();
			discard();
		}
	}

	/** Per-tick flight visuals (server-side particle spawns). */
	protected abstract void spawnTrail();

	/** One-shot impact: AoE + visuals + sound. No block changes. */
	protected abstract void detonate();
}
