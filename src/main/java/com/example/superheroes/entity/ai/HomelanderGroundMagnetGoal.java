package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.physics.ShockwaveUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class HomelanderGroundMagnetGoal extends Goal {
	private static final int DESCEND_MAX = 60;
	private static final int PULL_TICKS = 60;
	private static final int CD_AFTER = 380;
	private static final double PULL_RADIUS = 16.0;
	private static final double SHOCK_RADIUS = 9.0;
	private static final float SHOCK_DAMAGE = 18.0f;
	private static final double TRIGGER_RANGE = 26.0;

	private final HomelanderBossEntity boss;
	private int phase;
	private int phaseTimer;

	public HomelanderGroundMagnetGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getMagnetCooldown() > 0) return false;
		return boss.distanceToSqr(t) <= TRIGGER_RANGE * TRIGGER_RANGE;
	}

	@Override
	public boolean canContinueToUse() {
		if (phase == 3) return false;
		LivingEntity t = boss.getTarget();
		return t != null && t.isAlive();
	}

	@Override
	public void start() {
		phase = 0;
		phaseTimer = 0;
		LivingEntity t = boss.getTarget();
		if (t != null) {
			ServerLevel sl = (ServerLevel) boss.level();
			sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
					SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.8f);
			boss.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.6);
		}
	}

	@Override
	public void stop() {
		boss.setMagnetCooldown(CD_AFTER);
		boss.setNoGravity(true);
		phase = 3;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ServerLevel sl = (ServerLevel) boss.level();
		LivingEntity target = boss.getTarget();
		if (target == null) {
			phase = 3;
			return;
		}
		phaseTimer++;
		if (phase == 0) {
			boss.setNoGravity(false);
			boss.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.6);
			if (boss.onGround() || phaseTimer > DESCEND_MAX) {
				phase = 1;
				phaseTimer = 0;
				sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.6f);
				sl.sendParticles(ParticleTypes.EXPLOSION,
						boss.getX(), boss.getY() + 0.2, boss.getZ(),
						6, 0.6, 0.2, 0.6, 0.0);
			}
		} else if (phase == 1) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			AABB box = boss.getBoundingBox().inflate(PULL_RADIUS);
			List<Entity> entities = sl.getEntities(boss, box);
			Vec3 center = boss.position();
			for (Entity e : entities) {
				if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
				Vec3 d = center.subtract(e.position());
				double dist = d.length();
				if (dist < 1.5 || dist > PULL_RADIUS) continue;
				Vec3 pull = d.normalize().scale(0.5);
				e.setDeltaMovement(
						e.getDeltaMovement().x * 0.4 + pull.x,
						Math.max(e.getDeltaMovement().y * 0.4 + 0.1, 0.05),
						e.getDeltaMovement().z * 0.4 + pull.z);
				e.hurtMarked = true;
			}
			if (phaseTimer % 4 == 0) {
				sl.sendParticles(ParticleTypes.PORTAL,
						boss.getX(), boss.getY() + 1.0, boss.getZ(),
						18, PULL_RADIUS * 0.5, 0.4, PULL_RADIUS * 0.5, 0.6);
				sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0f, 0.5f);
			}
			if (phaseTimer >= PULL_TICKS) {
				phase = 2;
				phaseTimer = 0;
				ShockwaveUtil.detonateMob(boss, sl, boss.position(),
						SHOCK_RADIUS, SHOCK_DAMAGE, false);
			}
		} else if (phase == 2) {
			boss.setNoGravity(true);
			Vec3 up = boss.position().add(0, 8.0, 0);
			boss.getMoveControl().setWantedPosition(up.x, up.y, up.z, 2.0);
			if (phaseTimer >= 20) {
				phase = 3;
			}
		}
	}
}
