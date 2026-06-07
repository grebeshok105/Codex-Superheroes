package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class HomelanderLightningCallGoal extends Goal {
	private static final int CHARGE_TICKS = 30;
	private static final int CD_AFTER = 240;
	private static final double RANGE = 40.0;
	private static final float BONUS_DAMAGE = 8.0f;

	private final HomelanderBossEntity boss;
	private int phaseTick;
	private LivingEntity lockedTarget;

	public HomelanderLightningCallGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getLightningCooldown() > 0) return false;
		if (!boss.level().canSeeSky(t.blockPosition())) return false;
		return boss.distanceToSqr(t) <= RANGE * RANGE;
	}

	@Override
	public boolean canContinueToUse() {
		return phaseTick <= CHARGE_TICKS && lockedTarget != null && lockedTarget.isAlive();
	}

	@Override
	public void start() {
		phaseTick = 0;
		lockedTarget = boss.getTarget();
		if (boss.level() instanceof ServerLevel sl) {
			sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
					SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.6f, 1.4f);
		}
	}

	@Override
	public void stop() {
		boss.setLightningCooldown(CD_AFTER);
		lockedTarget = null;
		phaseTick = 0;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (lockedTarget == null) return;
		boss.getLookControl().setLookAt(lockedTarget, 60f, 60f);
		phaseTick++;
		if (phaseTick == CHARGE_TICKS) {
			ServerLevel sl = (ServerLevel) boss.level();
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
			if (bolt != null) {
				bolt.moveTo(lockedTarget.getX(), lockedTarget.getY(), lockedTarget.getZ());
				bolt.setCause(null);
				sl.addFreshEntity(bolt);
			}
			DamageSource ds = com.example.superheroes.damage.ModDamageTypes.homelanderLightningCall((net.minecraft.server.level.ServerLevel) boss.level(), boss);
			lockedTarget.hurt(ds, BONUS_DAMAGE);
		}
	}
}
