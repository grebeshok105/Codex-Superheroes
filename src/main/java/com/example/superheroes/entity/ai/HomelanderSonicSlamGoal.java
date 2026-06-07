package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class HomelanderSonicSlamGoal extends Goal {
	private static final int WINDUP_TICKS = 18;
	private static final int CHARGE_TICKS = 30;
	private static final int CD_AFTER = 200;
	private static final double MIN_RANGE = 8.0;
	private static final double MAX_RANGE = 22.0;
	private static final float IMPACT_DAMAGE = 22.0f;
	private static final double KNOCKBACK = 2.6;

	private final HomelanderBossEntity boss;
	private int phaseTick;
	private boolean hit;
	private Vec3 chargeDir;

	public HomelanderSonicSlamGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getSlamCooldown() > 0) return false;
		double d2 = boss.distanceToSqr(t);
		return d2 >= MIN_RANGE * MIN_RANGE && d2 <= MAX_RANGE * MAX_RANGE;
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity t = boss.getTarget();
		return t != null && t.isAlive() && phaseTick < WINDUP_TICKS + CHARGE_TICKS && !hit;
	}

	@Override
	public void start() {
		phaseTick = 0;
		hit = false;
		chargeDir = null;
		ServerLevel sl = (ServerLevel) boss.level();
		sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
				SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.6f, 0.4f);
	}

	@Override
	public void stop() {
		boss.setSlamCooldown(CD_AFTER);
		boss.setDeltaMovement(boss.getDeltaMovement().scale(0.3));
		chargeDir = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity t = boss.getTarget();
		if (t == null) return;
		boss.getLookControl().setLookAt(t, 60f, 60f);
		ServerLevel sl = (ServerLevel) boss.level();

		if (phaseTick < WINDUP_TICKS) {
			boss.setDeltaMovement(boss.getDeltaMovement().scale(0.6));
			sl.sendParticles(ParticleTypes.CLOUD,
					boss.getX(), boss.getY() + 1.0, boss.getZ(),
					3, 0.3, 0.2, 0.3, 0.02);
		} else {
			if (chargeDir == null) {
				Vec3 d = t.getEyePosition().subtract(boss.getEyePosition());
				double len = d.length();
				if (len > 0.01) {
					chargeDir = d.scale(1.0 / len);
					sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
							SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.6f);
				}
			}
			if (chargeDir != null) {
				Vec3 v = chargeDir.scale(2.4);
				boss.setDeltaMovement(v);
				boss.hasImpulse = true;
			}
			sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
					boss.getX(), boss.getY() + 1.0, boss.getZ(),
					1, 0.0, 0.0, 0.0, 0.0);
			sl.sendParticles(ParticleTypes.CLOUD,
					boss.getX(), boss.getY() + 1.0, boss.getZ(),
					6, 0.4, 0.3, 0.4, 0.05);

			if (boss.distanceToSqr(t) < 6.0) {
				DamageSource ds = com.example.superheroes.damage.ModDamageTypes.homelanderSonicSlam((net.minecraft.server.level.ServerLevel) boss.level(), boss);
				t.hurt(ds, IMPACT_DAMAGE);
				if (chargeDir != null) {
					Vec3 push = chargeDir.scale(KNOCKBACK).add(0.0, 0.6, 0.0);
					t.push(push.x, push.y, push.z);
				}
				sl.sendParticles(ParticleTypes.EXPLOSION,
						t.getX(), t.getY() + 1.0, t.getZ(),
						5, 0.5, 0.5, 0.5, 0.0);
				sl.playSound(null, t.getX(), t.getY(), t.getZ(),
						SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.8f);
				hit = true;
			}
		}
		phaseTick++;
	}
}
