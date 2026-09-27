package io.github.grebeshok105.codex.content.boss.homelander.api;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The boss-facing surface the AI goals drive: the nine ability cooldowns plus the vanilla
 * navigation/sensing members the goals read. Implemented by the entity package — this package
 * imports nothing from it, so ai -> api stays a leaf edge and entity -> ai stays one-way.
 * Every member is an inherited {@link Mob}/{@link Entity} method: implementors satisfy the
 * interface for free. {@link #asMob()} hands call sites the entity where a vanilla helper
 * wants an {@link Entity} argument.
 */
public interface HomelanderBossApi {
	default Mob asMob() {
		return (Mob) this;
	}

	// ── ability cooldowns ─────────────────────────────────────────────
	int getLaserCooldown();

	void setLaserCooldown(int ticks);

	int getShockwaveCooldown();

	void setShockwaveCooldown(int ticks);

	int getSweepCooldown();

	void setSweepCooldown(int ticks);

	int getLightningCooldown();

	void setLightningCooldown(int ticks);

	int getSlamCooldown();

	void setSlamCooldown(int ticks);

	int getMagnetCooldown();

	void setMagnetCooldown(int ticks);

	int getThrowCooldown();

	void setThrowCooldown(int ticks);

	int getRoarCooldown();

	void setRoarCooldown(int ticks);

	int getHandClapCooldown();

	void setHandClapCooldown(int ticks);

	// ── vanilla members the goals touch (inherited impls satisfy these) ──
	Level level();

	Vec3 position();

	BlockPos blockPosition();

	RandomSource getRandom();

	double getX();

	double getY();

	double getZ();

	LivingEntity getTarget();

	double distanceToSqr(Entity other);

	LookControl getLookControl();

	MoveControl getMoveControl();

	Vec3 getDeltaMovement();

	void setDeltaMovement(Vec3 delta);

	void setDeltaMovement(double x, double y, double z);

	Vec3 getViewVector(float partialTicks);

	Vec3 getEyePosition();

	AABB getBoundingBox();

	void setNoGravity(boolean noGravity);

	boolean onGround();

	float getBbHeight();

	boolean hasLineOfSight(Entity other);
}
