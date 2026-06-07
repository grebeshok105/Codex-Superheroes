package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.physics.ShockwaveUtil;
import com.example.superheroes.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class HomelanderShockwaveDiveGoal extends Goal {
	private static final double ASCEND_HEIGHT = 14.0;
	private static final double IMPACT_RADIUS = 8.0;
	private static final float IMPACT_DAMAGE = 18.0f;
	private static final int MIN_CD = 300;
	private static final int MAX_CD = 460;
	private static final int TIMEOUT_TICKS = 120;

	private final HomelanderBossEntity boss;
	private int phase;
	private int phaseTimer;
	private Vec3 diveTarget;

	public HomelanderShockwaveDiveGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = boss.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}
		return boss.getShockwaveCooldown() <= 0 && boss.distanceToSqr(target) <= 30.0 * 30.0;
	}

	@Override
	public boolean canContinueToUse() {
		return phase < 2 && phaseTimer < TIMEOUT_TICKS && boss.getTarget() != null;
	}

	@Override
	public void start() {
		phase = 0;
		phaseTimer = 0;
		diveTarget = null;
		LivingEntity target = boss.getTarget();
		if (target != null) {
			boss.getMoveControl().setWantedPosition(boss.getX(), target.getY() + ASCEND_HEIGHT + 2.0, boss.getZ(), 2.0);
			((ServerLevel) boss.level()).playSound(null, boss.getX(), boss.getY(), boss.getZ(),
					SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.6f, 0.7f);
		}
	}

	@Override
	public void stop() {
		boss.setShockwaveCooldown(MIN_CD + boss.getRandom().nextInt(MAX_CD - MIN_CD));
		phase = 2;
		diveTarget = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		phaseTimer++;
		LivingEntity target = boss.getTarget();
		if (target == null) {
			phase = 2;
			return;
		}
		if (phase == 0) {
			if (boss.getY() >= target.getY() + ASCEND_HEIGHT) {
				phase = 1;
				diveTarget = target.position();
				boss.getMoveControl().setWantedPosition(diveTarget.x, diveTarget.y, diveTarget.z, 3.0);
				((ServerLevel) boss.level()).playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 0.7f);
			}
		} else if (phase == 1) {
			if (diveTarget != null) {
				double dx = diveTarget.x - boss.getX();
				double dz = diveTarget.z - boss.getZ();
				double horiz2 = dx * dx + dz * dz;
				if (boss.getY() <= diveTarget.y + 1.0 || (boss.onGround() && horiz2 < 4.0)) {
					ServerLevel sl = (ServerLevel) boss.level();
					ShockwaveUtil.detonateMob(boss, sl,
							boss.position(), IMPACT_RADIUS, IMPACT_DAMAGE, false,
							com.example.superheroes.damage.ModDamageTypes.homelanderShockwaveDive(sl, boss));
					sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
							ModSounds.HOMELANDER_IRON_FISTS_IMPACT, SoundSource.HOSTILE, 1.6f, 0.85f);
					phase = 2;
				}
			}
		}
	}
}
