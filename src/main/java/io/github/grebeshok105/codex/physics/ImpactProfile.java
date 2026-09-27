package io.github.grebeshok105.codex.physics;

import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record ImpactProfile(
		ResourceLocation heroId,
		ImpactTier tier,
		ImpactStyle style,
		Vec3 direction,
		float damage,
		double knockback,
		double upwardKnockback,
		double launchPower,
		float variance,
		double shakeRadius,
		float shakeIntensity,
		float debrisIntensity
) {
}
