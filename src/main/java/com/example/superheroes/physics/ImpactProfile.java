package com.example.superheroes.physics;

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
		boolean breakBlocks,
		boolean wallPierce,
		double breakRadius,
		int maxBlocks,
		float maxHardness,
		double shakeRadius,
		float shakeIntensity,
		float debrisIntensity
) {
}
