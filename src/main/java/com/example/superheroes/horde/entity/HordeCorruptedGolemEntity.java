package com.example.superheroes.horde.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
 * Corrupted Golem — slow but massive damage, AoE ground slam.
 */
public class HordeCorruptedGolemEntity extends BaseHordeEntity {
	private int slamCooldown = 0;

	public HordeCorruptedGolemEntity(EntityType<? extends HordeCorruptedGolemEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 160.0)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.ARMOR, 15.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
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
		LivingEntity target = getTarget();
		if (target != null && distanceTo(target) < 4.0 && slamCooldown <= 0) {
			groundSlam();
			slamCooldown = 100;
		}
	}

	private void groundSlam() {
		if (!(level() instanceof ServerLevel sl)) return;
		playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
		sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 3, 1.5, 0.2, 1.5, 0.0);
		sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 20, 2.0, 0.3, 2.0, 0.05);
		AABB area = getBoundingBox().inflate(3.5);
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (e == this || e instanceof BaseHordeEntity) continue;
			e.hurt(damageSources().mobAttack(this), 12.0f);
			e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
			e.hurtMarked = true;
		}
	}
}
