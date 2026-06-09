package com.example.superheroes.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class BleedingMobEffect extends MobEffect {
	public BleedingMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level() instanceof ServerLevel level) {
			MobEffectInstance regen = entity.getEffect(MobEffects.REGENERATION);
			if (regen != null) {
				int lowered = regen.getAmplifier() - 1;
				entity.removeEffect(MobEffects.REGENERATION);
				if (lowered >= 0) {
					entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
							regen.getDuration(), lowered, regen.isAmbient(), regen.isVisible(), regen.showIcon()));
				}
			}
			entity.hurt(entity.damageSources().magic(), 1.0f + amplifier);
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
					entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
					5 + amplifier * 2, 0.25, 0.25, 0.25, 0.04);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}
}
