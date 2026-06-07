package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class HomelanderBlockThrowGoal extends Goal {
	private static final int WINDUP_TICKS = 30;
	private static final int CD_AFTER = 220;
	private static final double RANGE = 36.0;

	private final HomelanderBossEntity boss;
	private int phaseTick;
	private BlockState pickedState;
	private BlockPos pickedPos;

	public HomelanderBlockThrowGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getThrowCooldown() > 0) return false;
		return boss.distanceToSqr(t) <= RANGE * RANGE && boss.distanceToSqr(t) > 36.0;
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity t = boss.getTarget();
		return t != null && t.isAlive() && phaseTick <= WINDUP_TICKS;
	}

	@Override
	public void start() {
		phaseTick = 0;
		pickedState = null;
		pickedPos = null;
		ServerLevel sl = (ServerLevel) boss.level();
		BlockPos under = boss.blockPosition().below();
		for (int dy = 0; dy <= 2; dy++) {
			BlockPos p = under.below(dy);
			BlockState bs = sl.getBlockState(p);
			if (bs.isSolid() && !bs.isAir() && bs.getDestroySpeed(sl, p) >= 0f) {
				pickedState = bs;
				pickedPos = p;
				break;
			}
		}
		sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
				SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.2f, 0.7f);
	}

	@Override
	public void stop() {
		boss.setThrowCooldown(CD_AFTER);
		pickedState = null;
		pickedPos = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = boss.getTarget();
		if (target == null) return;
		boss.getLookControl().setLookAt(target, 60f, 60f);
		ServerLevel sl = (ServerLevel) boss.level();

		if (phaseTick < WINDUP_TICKS) {
			if (pickedState != null && pickedPos != null && phaseTick % 4 == 0) {
				sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, pickedState),
						pickedPos.getX() + 0.5, pickedPos.getY() + 1.05, pickedPos.getZ() + 0.5,
						6, 0.4, 0.2, 0.4, 0.05);
			}
		} else if (phaseTick == WINDUP_TICKS) {
			if (pickedState != null && pickedPos != null) {
				sl.removeBlock(pickedPos, false);

				Vec3 spawn = boss.position().add(boss.getViewVector(1f).scale(2.0)).add(0, 1.5, 0);
				FallingBlockEntity fb = FallingBlockEntity.fall(sl, BlockPos.containing(spawn), pickedState);
				fb.setPos(spawn.x, spawn.y, spawn.z);
				fb.setHurtsEntities(2.0f, 30);
				fb.time = 1;

				Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(spawn);
				double dist = to.length();
				Vec3 dir = dist > 0.001 ? to.scale(1.0 / dist) : new Vec3(0, 0, 1);
				double speed = Math.min(2.5, 1.6 + dist * 0.04);
				fb.setDeltaMovement(dir.x * speed, dir.y * speed + 0.18, dir.z * speed);
				fb.hurtMarked = true;

				sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
						SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.4f);
			}
		}
		phaseTick++;
	}
}
