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
 * Infected Creeper — explodes on contact dealing AoE damage but doesn't
 * destroy blocks.
 */
public class HordeInfectedCreeperEntity extends BaseHordeEntity {
	private int fuseTimer = -1;
	private static final int FUSE_TIME = 30;

	public HordeInfectedCreeperEntity(EntityType<? extends HordeInfectedCreeperEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		LivingEntity target = getTarget();
		if (target != null && distanceTo(target) < 3.0) {
			if (fuseTimer < 0) fuseTimer = FUSE_TIME;
			fuseTimer--;
			if (fuseTimer == 0) {
				explode();
			}
		} else {
			fuseTimer = -1;
		}
	}

	private void explode() {
		if (level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.0, getZ(), 1, 0, 0, 0, 0);
			sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 30, 2.0, 1.0, 2.0, 0.1);
			playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.8f);
			AABB area = getBoundingBox().inflate(4.0);
			for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
				if (e == this) continue;
				e.hurt(damageSources().mobAttack(this), 18.0f);
			}
		}
		discard();
	}
}
