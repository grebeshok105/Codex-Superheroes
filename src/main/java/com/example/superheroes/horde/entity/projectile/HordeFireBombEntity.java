package com.example.superheroes.horde.entity.projectile;

import com.example.superheroes.horde.entity.HordeEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * Fire bomb — thrown by ranged "fire" horde mobs. On impact, a fiery burst that
 * ignites and knocks back nearby living entities. Never breaks or sets blocks
 * on fire (no terrain modification), only entity-side fire + visuals.
 */
public class HordeFireBombEntity extends HordeBombEntity {

	private static final double BLAST_RADIUS = 2.6;

	public HordeFireBombEntity(EntityType<? extends HordeFireBombEntity> type, Level level) {
		super(type, level);
	}

	public HordeFireBombEntity(LivingEntity shooter, Level level) {
		super(HordeEntities.FIRE_BOMB, shooter, level);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (!level().isClientSide() && result.getEntity() instanceof LivingEntity living) {
			living.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity le ? le : null), 4.0f);
		}
	}

	@Override
	protected void spawnTrail() {
		if (level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.01);
			sl.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	@Override
	protected void detonate() {
		if (!(level() instanceof ServerLevel sl)) {
			return;
		}
		sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.1, getZ(), 18, 0.5, 0.2, 0.5, 0.0);
		sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 60, 0.8, 0.3, 0.8, 0.08);
		sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 20, 0.6, 0.4, 0.6, 0.02);
		sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.4f);
		sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2f, 0.8f);

		LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
		AABB box = new AABB(getX() - BLAST_RADIUS, getY() - BLAST_RADIUS, getZ() - BLAST_RADIUS,
				getX() + BLAST_RADIUS, getY() + BLAST_RADIUS, getZ() + BLAST_RADIUS);
		List<LivingEntity> hit = sl.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e != owner);
		for (LivingEntity living : hit) {
			double distSq = living.distanceToSqr(getX(), getY(), getZ());
			if (distSq > BLAST_RADIUS * BLAST_RADIUS) {
				continue;
			}
			living.hurt(damageSources().mobProjectile(this, owner), 3.0f);
			living.setRemainingFireTicks(80);
			living.knockback(0.55, getX() - living.getX(), getZ() - living.getZ());
		}
	}
}
