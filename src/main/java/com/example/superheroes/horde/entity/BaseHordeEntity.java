package com.example.superheroes.horde.entity;

import com.example.superheroes.horde.HordeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Base class for all horde entities. Tracks the horde instance they belong to
 * and notifies the HordeManager on death.
 */
public abstract class BaseHordeEntity extends Monster {
	private UUID hordeId;

	protected BaseHordeEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 5;
	}

	public static AttributeSupplier.Builder createBaseHordeAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.STEP_HEIGHT, 1.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
	}

	private net.minecraft.world.phys.Vec3 lastStuckCheckPos;
	private int stuckTicks;

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			tickAntiStuck();
		}
	}

	/**
	 * Анти-застревание: если тварь с целью почти не двигается ~3 секунды,
	 * сначала пробует прыжок-рывок к цели, затем телепортируется поближе.
	 */
	private void tickAntiStuck() {
		var target = getTarget();
		if (target == null || !target.isAlive()) {
			stuckTicks = 0;
			lastStuckCheckPos = null;
			return;
		}
		double distSq = distanceToSqr(target);
		if (distSq < 9.0) {
			stuckTicks = 0;
			lastStuckCheckPos = position();
			return;
		}
		if (lastStuckCheckPos == null) {
			lastStuckCheckPos = position();
			return;
		}
		if (position().distanceToSqr(lastStuckCheckPos) > 0.6) {
			stuckTicks = 0;
			lastStuckCheckPos = position();
			return;
		}
		stuckTicks++;
		if (stuckTicks == 30) {
			// рывок: прыжок в сторону цели
			net.minecraft.world.phys.Vec3 dir = target.position().subtract(position()).normalize();
			setDeltaMovement(dir.x * 0.7, 0.55, dir.z * 0.7);
			hurtMarked = true;
		} else if (stuckTicks >= 60) {
			stuckTicks = 0;
			lastStuckCheckPos = null;
			teleportNear(target);
		}
	}

	private void teleportNear(net.minecraft.world.entity.LivingEntity target) {
		if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
		for (int attempt = 0; attempt < 12; attempt++) {
			double ang = random.nextDouble() * Math.PI * 2;
			double dist = 5.0 + random.nextDouble() * 5.0;
			double tx = target.getX() + Math.cos(ang) * dist;
			double tz = target.getZ() + Math.sin(ang) * dist;
			int ty = server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(int) Math.floor(tx), (int) Math.floor(tz));
			if (Math.abs(ty - target.getY()) > 12) continue;
			if (randomTeleport(tx, ty, tz, false)) {
				server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
						getX(), getY() + 0.8, getZ(), 14, 0.3, 0.5, 0.3, 0.05);
				getNavigation().stop();
				return;
			}
		}
	}

	public void setHordeId(UUID hordeId) {
		this.hordeId = hordeId;
	}

	public UUID getHordeId() {
		return hordeId;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide() && hordeId != null) {
			HordeManager.onMobKilled(hordeId);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (hordeId != null) tag.putUUID("HordeId", hordeId);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("HordeId")) hordeId = tag.getUUID("HordeId");
	}

	@Override
	public boolean removeWhenFarAway(double distSq) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}
}
