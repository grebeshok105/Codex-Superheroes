package com.example.superheroes.horde.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Leviathan — T3 mega-boss. Ground slam + charge + enrage under 50% HP.
 */
public class HordeLeviathanEntity extends BaseHordeEntity {
	private int slamCooldown = 0;
	private boolean enraged = false;

	public HordeLeviathanEntity(EntityType<? extends HordeLeviathanEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 350.0)
				.add(Attributes.MOVEMENT_SPEED, 0.20)
				.add(Attributes.ATTACK_DAMAGE, 16.0)
				.add(Attributes.ARMOR, 16.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.3));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		if (slamCooldown > 0) slamCooldown--;
		if (!enraged && getHealth() < getMaxHealth() * 0.5f) {
			enraged = true;
			addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 1, false, true, false));
			addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 1, false, true, false));
			if (level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2, getZ(), 30, 1.5, 1.0, 1.5, 0.1);
				playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0f, 0.5f);
			}
		}
		if (getTarget() != null && distanceTo(getTarget()) < 5.0 && slamCooldown <= 0) {
			devastatingSlam();
			slamCooldown = enraged ? 60 : 120;
		}
	}

	private void devastatingSlam() {
		if (!(level() instanceof ServerLevel sl)) return;
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.4f);
		playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 0.6f);
		sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 2, 0, 0, 0, 0);
		sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 40, 3.0, 0.5, 3.0, 0.1);
		sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0, getZ(), 30, 2.0, 1.0, 2.0, 0.15);
		float slamDmg = enraged ? 24.0f : 16.0f;
		AABB area = getBoundingBox().inflate(5.0);
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (e == this || e instanceof BaseHordeEntity) continue;
			e.hurt(damageSources().mobAttack(this), slamDmg);
			e.setDeltaMovement(e.getDeltaMovement().add(0, 1.0, 0));
			e.hurtMarked = true;
		}
	}
}
