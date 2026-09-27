package io.github.grebeshok105.codex.core.hero;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Mutable melee-impact parameters handed to {@link Hero#modifyImpact}: {@code mechanic.impact}
 * fills the baseline from tier/style/power, the hero adjusts fields in place, the engine builds
 * the final {@code ImpactProfile} from whatever remains. {@code piercing} marks a launch that
 * tunnels the target through walls ({@code BallisticBodyTracker} super-pierce mode).
 */
public final class ImpactContext {
	private final ServerPlayer attacker;
	private final ResourceLocation heroId;
	private final LivingEntity target;
	private final int heldTicks;

	private float damage;
	private double knockback;
	private double upwardKnockback;
	private double launchPower;
	private float variance;
	private double shakeRadius;
	private float shakeIntensity;
	private float debrisIntensity;
	private boolean piercing;

	public ImpactContext(ServerPlayer attacker, ResourceLocation heroId, LivingEntity target, int heldTicks,
			float damage, double knockback, double upwardKnockback, double launchPower,
			float variance, double shakeRadius, float shakeIntensity, float debrisIntensity) {
		this.attacker = attacker;
		this.heroId = heroId;
		this.target = target;
		this.heldTicks = heldTicks;
		this.damage = damage;
		this.knockback = knockback;
		this.upwardKnockback = upwardKnockback;
		this.launchPower = launchPower;
		this.variance = variance;
		this.shakeRadius = shakeRadius;
		this.shakeIntensity = shakeIntensity;
		this.debrisIntensity = debrisIntensity;
	}

	public ServerPlayer attacker() {
		return attacker;
	}

	public ResourceLocation heroId() {
		return heroId;
	}

	public LivingEntity target() {
		return target;
	}

	public int heldTicks() {
		return heldTicks;
	}

	public float damage() {
		return damage;
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public double knockback() {
		return knockback;
	}

	public void setKnockback(double knockback) {
		this.knockback = knockback;
	}

	public double upwardKnockback() {
		return upwardKnockback;
	}

	public void setUpwardKnockback(double upwardKnockback) {
		this.upwardKnockback = upwardKnockback;
	}

	public double launchPower() {
		return launchPower;
	}

	public void setLaunchPower(double launchPower) {
		this.launchPower = launchPower;
	}

	public float variance() {
		return variance;
	}

	public void setVariance(float variance) {
		this.variance = variance;
	}

	public double shakeRadius() {
		return shakeRadius;
	}

	public void setShakeRadius(double shakeRadius) {
		this.shakeRadius = shakeRadius;
	}

	public float shakeIntensity() {
		return shakeIntensity;
	}

	public void setShakeIntensity(float shakeIntensity) {
		this.shakeIntensity = shakeIntensity;
	}

	public float debrisIntensity() {
		return debrisIntensity;
	}

	public void setDebrisIntensity(float debrisIntensity) {
		this.debrisIntensity = debrisIntensity;
	}

	public boolean piercing() {
		return piercing;
	}

	public void setPiercing(boolean piercing) {
		this.piercing = piercing;
	}
}
