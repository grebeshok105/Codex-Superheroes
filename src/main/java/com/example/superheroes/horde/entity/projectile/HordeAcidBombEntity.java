package com.example.superheroes.horde.entity.projectile;

import com.example.superheroes.horde.entity.HordeEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Acid bomb — thrown by ranged "poison" horde mobs. On impact leaves a lingering
 * poison cloud. Does not damage blocks.
 */
public class HordeAcidBombEntity extends HordeBombEntity {

	public HordeAcidBombEntity(EntityType<? extends HordeAcidBombEntity> type, Level level) {
		super(type, level);
	}

	public HordeAcidBombEntity(LivingEntity shooter, Level level) {
		super(HordeEntities.ACID_BOMB, shooter, level);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SLIME_BALL;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (!level().isClientSide() && result.getEntity() instanceof LivingEntity living) {
			living.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity le ? le : null), 3.0f);
			living.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1, false, true, true));
		}
	}

	@Override
	protected void spawnTrail() {
		if (level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
		}
	}

	@Override
	protected void detonate() {
		if (!(level() instanceof ServerLevel sl)) {
			return;
		}
		sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.1, getZ(), 40, 0.6, 0.2, 0.6, 0.1);
		sl.sendParticles(ParticleTypes.SNEEZE, getX(), getY() + 0.1, getZ(), 20, 0.6, 0.2, 0.6, 0.02);
		sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.7f);

		AreaEffectCloud cloud = new AreaEffectCloud(sl, getX(), getY(), getZ());
		if (getOwner() instanceof LivingEntity owner) {
			cloud.setOwner(owner);
		}
		cloud.setParticle(ParticleTypes.ITEM_SLIME);
		cloud.setRadius(3.0f);
		cloud.setDuration(100);
		cloud.setWaitTime(0);
		cloud.setRadiusOnUse(-0.04f);
		cloud.setRadiusPerTick(-(3.0f / 100f));
		cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
		sl.addFreshEntity(cloud);
	}
}
