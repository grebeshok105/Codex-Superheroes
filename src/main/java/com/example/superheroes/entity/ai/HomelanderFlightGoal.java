package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class HomelanderFlightGoal extends Goal {
	private final HomelanderBossEntity boss;
	private int recalc;

	public HomelanderFlightGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = boss.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (recalc > 0) {
			recalc--;
			return;
		}
		recalc = 3;
		LivingEntity target = boss.getTarget();
		if (target == null) {
			return;
		}
		double dx = target.getX() - boss.getX();
		double dz = target.getZ() - boss.getZ();
		double horiz = Math.sqrt(dx * dx + dz * dz);
		double desiredDist = 6.0;
		double height = 3.0;
		double tx;
		double tz;
		if (horiz > desiredDist + 0.5) {
			double f = Math.min(1.0, (horiz - desiredDist) / Math.max(horiz, 0.01));
			tx = boss.getX() + dx * f;
			tz = boss.getZ() + dz * f;
		} else if (horiz < desiredDist - 1.0) {
			double f = (desiredDist - horiz) / Math.max(horiz, 0.01);
			tx = boss.getX() - dx * f;
			tz = boss.getZ() - dz * f;
		} else {
			tx = boss.getX();
			tz = boss.getZ();
		}
		double ty = target.getY() + height;
		boss.getMoveControl().setWantedPosition(tx, ty, tz, 1.6);
		boss.getLookControl().setLookAt(target, 60f, 60f);
	}
}
