package com.example.superheroes.horde.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * Broodmother — T2 mini-boss. Periodically heals nearby horde mobs
 * and deals AoE poison aura.
 */
public class HordeBroodmotherEntity extends BaseHordeEntity {
	private int auraTick = 0;

	public HordeBroodmotherEntity(EntityType<? extends HordeBroodmotherEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 120.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.4));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		auraTick++;
		if (auraTick % 60 == 0) {
			healNearbyHorde();
		}
	}

	private void healNearbyHorde() {
		if (!(level() instanceof ServerLevel sl)) return;
		AABB area = getBoundingBox().inflate(8.0);
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (e instanceof BaseHordeEntity && e != this && e.isAlive()) {
				e.heal(4.0f);
				sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 1.0, e.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
			}
		}
		sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0, getZ(), 15, 1.0, 0.5, 1.0, 0.02);
	}
}
