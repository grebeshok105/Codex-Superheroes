package com.example.superheroes.entity.ai;

import com.example.superheroes.effect.UraniumDefenseController;
import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class HomelanderEyeLaserGoal extends Goal {
	private static final int CHARGE_TICKS = 6;
	private static final int FIRE_TICKS = 30;
	private static final int CYCLE = CHARGE_TICKS + FIRE_TICKS;
	private static final int CD_AFTER = 60;
	private static final double RANGE = 64.0;
	private static final double CHEST_FRACTION = 0.7;
	private static final float MIN_DPS = 14.0f;
	private static final float MAX_DPS = 30.0f;
	private static final float DAMAGE_MULT = 14.0f;

	private final HomelanderBossEntity boss;
	private int phaseTick;

	public HomelanderEyeLaserGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getLaserCooldown() > 0) return false;
		return boss.distanceToSqr(t) <= RANGE * RANGE && boss.hasLineOfSight(t);
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity t = boss.getTarget();
		return t != null && t.isAlive() && phaseTick < CYCLE;
	}

	@Override
	public void start() {
		phaseTick = 0;
		ServerLevel level = (ServerLevel) boss.level();
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4f, 1.6f);
	}

	@Override
	public void stop() {
		boss.setLaserCooldown(CD_AFTER);
		phaseTick = 0;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = boss.getTarget();
		if (target == null) return;
		boss.getLookControl().setLookAt(target, 90f, 90f);

		if (phaseTick < CHARGE_TICKS) {
			ServerLevel level = (ServerLevel) boss.level();
			Vec3 eye = boss.getEyePosition();
			level.sendParticles(ModParticles.LASER_SPARK,
					eye.x, eye.y, eye.z, 2, 0.10, 0.10, 0.10, 0.04);
		} else {
			fireBeam();
			if ((phaseTick - CHARGE_TICKS) % 6 == 0) {
				ServerLevel level = (ServerLevel) boss.level();
				level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.7f, 1.6f);
			}
		}
		phaseTick++;
	}

	private void fireBeam() {
		ServerLevel level = (ServerLevel) boss.level();
		Vec3 eye = boss.getEyePosition();
		LivingEntity target = boss.getTarget();
		Vec3 forward;
		if (target != null) {
			Vec3 chest = new Vec3(target.getX(),
					target.getY() + target.getBbHeight() * CHEST_FRACTION, target.getZ());
			Vec3 dir = chest.subtract(eye);
			double len = dir.length();
			forward = len > 0.001 ? dir.scale(1.0 / len) : boss.getViewVector(1f);
		} else {
			forward = boss.getViewVector(1f);
		}
		Vec3 end = eye.add(forward.scale(RANGE));

		BlockHitResult blockHit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
		Vec3 entitySearchEnd = blockHit.getType() == HitResult.Type.BLOCK
				? blockHit.getLocation() : end;
		AABB box = boss.getBoundingBox().expandTowards(forward.scale(RANGE)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				level, boss, eye, entitySearchEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != boss && !e.isSpectator());

		Vec3 actualEnd = entitySearchEnd;
		float damage = damagePerTick() * DAMAGE_MULT;
		if (hit != null && hit.getEntity() instanceof net.minecraft.world.entity.player.Player victim
				&& UraniumDefenseController.hasUraniumDagger(victim)) {
			damage *= 0.5f;
		}
		if (hit != null) {
			LivingEntity hitTarget = (LivingEntity) hit.getEntity();
			DamageSource ds = com.example.superheroes.damage.ModDamageTypes.homelanderEyeLaser((net.minecraft.server.level.ServerLevel) boss.level(), boss);
			hitTarget.hurt(ds, damage);
			actualEnd = new Vec3(hitTarget.getX(),
					hitTarget.getY() + hitTarget.getBbHeight() * CHEST_FRACTION,
					hitTarget.getZ());
			level.sendParticles(ModParticles.LASER_SPARK,
					actualEnd.x, actualEnd.y, actualEnd.z,
					3, 0.10, 0.10, 0.10, 0.04);
		}
		ModNetworking.broadcastLaserFromEntity(boss, eye, actualEnd);
	}

	private float damagePerTick() {
		float dps = (MIN_DPS + MAX_DPS) * 0.5f;
		return dps / 20f;
	}
}
