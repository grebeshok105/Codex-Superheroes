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
 *
 * Vanilla-mirroring members MUST stay {@code default} delegates through {@link #asMob()}:
 * interface methods cannot be "satisfied for free" by superclass inheritance because vanilla
 * method names are remapped to intermediary names in the production jar while interface
 * member names are not — an abstract mirror leaves the implementor with only the obfuscated
 * superclass method and {@code invokeinterface} throws {@link AbstractMethodError}.
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

	// ── vanilla members the goals touch (default delegates: remap-safe) ──
	default Level level() {
		return asMob().level();
	}

	default Vec3 position() {
		return asMob().position();
	}

	default BlockPos blockPosition() {
		return asMob().blockPosition();
	}

	default RandomSource getRandom() {
		return asMob().getRandom();
	}

	default double getX() {
		return asMob().getX();
	}

	default double getY() {
		return asMob().getY();
	}

	default double getZ() {
		return asMob().getZ();
	}

	default LivingEntity getTarget() {
		return asMob().getTarget();
	}

	default double distanceToSqr(Entity other) {
		return asMob().distanceToSqr(other);
	}

	default LookControl getLookControl() {
		return asMob().getLookControl();
	}

	default MoveControl getMoveControl() {
		return asMob().getMoveControl();
	}

	default Vec3 getDeltaMovement() {
		return asMob().getDeltaMovement();
	}

	default void setDeltaMovement(Vec3 delta) {
		asMob().setDeltaMovement(delta);
	}

	default void setDeltaMovement(double x, double y, double z) {
		asMob().setDeltaMovement(x, y, z);
	}

	default Vec3 getViewVector(float partialTicks) {
		return asMob().getViewVector(partialTicks);
	}

	default Vec3 getEyePosition() {
		return asMob().getEyePosition();
	}

	default AABB getBoundingBox() {
		return asMob().getBoundingBox();
	}

	default void setNoGravity(boolean noGravity) {
		asMob().setNoGravity(noGravity);
	}

	default boolean onGround() {
		return asMob().onGround();
	}

	default float getBbHeight() {
		return asMob().getBbHeight();
	}

	default boolean hasLineOfSight(Entity other) {
		return asMob().hasLineOfSight(other);
	}
}
