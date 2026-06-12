package com.example.superheroes.horde.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import com.example.superheroes.horde.entity.projectile.HordeFireBombEntity;
import net.minecraft.world.level.Level;

public class HordeSpitterEntity extends BaseHordeEntity implements RangedAttackMob {
	public HordeSpitterEntity(EntityType<? extends HordeSpitterEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 40, 16.0f));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		// Lob an arcing fire bomb (ignite blast on impact, no block damage).
		HordeFireBombEntity bomb = new HordeFireBombEntity(this, level());
		double dx = target.getX() - getX();
		double dy = target.getY(0.5) - bomb.getY();
		double dz = target.getZ() - getZ();
		double horiz = Math.sqrt(dx * dx + dz * dz);
		bomb.shoot(dx, dy + horiz * 0.18, dz, 1.05f, 4.0f);
		level().addFreshEntity(bomb);
		playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 0.6f);
		if (level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.FLAME, getX(), getEyeY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
		}
	}
}
