package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HomelanderHeatVisionSweepGoal extends Goal {
	private static final int CHARGE_TICKS = 25;
	private static final int FIRE_TICKS = 30;
	private static final int CYCLE = CHARGE_TICKS + FIRE_TICKS;
	private static final int CD_AFTER = 280;
	private static final double RANGE = 18.0;
	private static final float DAMAGE_PER_HIT = 6.0f;
	private static final double HALF_ARC = Math.toRadians(60.0);
	private static final DustParticleOptions ORANGE_DUST =
			new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.1f), 1.8f);

	private final HomelanderBossEntity boss;
	private int phaseTick;
	private double startYaw;
	private final Set<UUID> hitOnce = new HashSet<>();

	public HomelanderHeatVisionSweepGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getSweepCooldown() > 0) return false;
		return boss.distanceToSqr(t) <= RANGE * RANGE;
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity t = boss.getTarget();
		return t != null && t.isAlive() && phaseTick < CYCLE;
	}

	@Override
	public void start() {
		phaseTick = 0;
		hitOnce.clear();
		LivingEntity t = boss.getTarget();
		if (t != null) {
			Vec3 d = t.position().subtract(boss.position());
			startYaw = Math.atan2(-d.x, d.z) - HALF_ARC;
		}
	}

	@Override
	public void stop() {
		boss.setSweepCooldown(CD_AFTER);
		hitOnce.clear();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity t = boss.getTarget();
		if (t == null) return;
		ServerLevel world = (ServerLevel) boss.level();
		Vec3 eye = boss.getEyePosition();

		if (phaseTick < CHARGE_TICKS) {
			boss.getLookControl().setLookAt(t, 60f, 60f);
			if (phaseTick == 0) {
				world.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.6f, 0.6f);
			}
			world.sendParticles(ORANGE_DUST, eye.x, eye.y, eye.z, 6, 0.18, 0.18, 0.18, 0.0);
		} else {
			int fireTick = phaseTick - CHARGE_TICKS;
			double sweepProgress = (double) fireTick / FIRE_TICKS;
			double yaw = startYaw + sweepProgress * (HALF_ARC * 2.0);
			Vec3 step = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));

			for (int i = 1; i <= 18; i++) {
				Vec3 p = eye.add(step.scale(i));
				world.sendParticles(ORANGE_DUST, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
			if (fireTick % 3 == 0) {
				world.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4f, 0.8f);
			}

			AABB box = boss.getBoundingBox().inflate(RANGE);
			List<Entity> nearby = world.getEntities(boss, box);
			DamageSource ds = com.example.superheroes.damage.ModDamageTypes.homelanderHeatVision((net.minecraft.server.level.ServerLevel) boss.level(), boss);
			for (Entity e : nearby) {
				if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
				if (hitOnce.contains(e.getUUID())) continue;
				Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
				double horiz2 = to.x * to.x + to.z * to.z;
				if (horiz2 < 0.01 || horiz2 > RANGE * RANGE) continue;
				double angle = Math.atan2(-to.x, to.z);
				double diff = Math.atan2(Math.sin(angle - yaw), Math.cos(angle - yaw));
				if (Math.abs(diff) < Math.toRadians(8.0)) {
					le.hurt(ds, DAMAGE_PER_HIT);
					hitOnce.add(e.getUUID());
				}
			}
		}
		phaseTick++;
	}
}
