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
 * Hivemind — T3 boss. Psychic scream debuffs players, buffs nearby horde mobs.
 */
public class HordeHivemindEntity extends BaseHordeEntity {
	private int screamCooldown = 0;
	private int buffCooldown = 0;

	public HordeHivemindEntity(EntityType<? extends HordeHivemindEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 250.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.ATTACK_DAMAGE, 12.0)
				.add(Attributes.ARMOR, 12.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.4));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		if (screamCooldown > 0) screamCooldown--;
		if (buffCooldown > 0) buffCooldown--;

		if (screamCooldown <= 0 && getTarget() != null && distanceTo(getTarget()) < 12.0) {
			psychicScream();
			screamCooldown = 200;
		}
		if (buffCooldown <= 0) {
			buffAllies();
			buffCooldown = 80;
		}
	}

	private void psychicScream() {
		if (!(level() instanceof ServerLevel sl)) return;
		playSound(SoundEvents.WARDEN_ROAR, 1.5f, 0.4f);
		sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
		sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 25, 3.0, 1.0, 3.0, 0.1);
		AABB area = getBoundingBox().inflate(10.0);
		for (Player p : level().getEntitiesOfClass(Player.class, area)) {
			p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
			p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
			p.hurt(damageSources().mobAttack(this), 8.0f);
		}
	}

	private void buffAllies() {
		AABB area = getBoundingBox().inflate(12.0);
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (e instanceof BaseHordeEntity && e != this && e.isAlive()) {
				e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, true, true));
				e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, true, true));
			}
		}
	}
}
