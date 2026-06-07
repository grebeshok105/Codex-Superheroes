package com.example.superheroes.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class SnappedMobEffect extends MobEffect {
	private static final int TICK_INTERVAL = 10;

	public SnappedMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % TICK_INTERVAL == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		MobEffectInstance instance = entity.getEffect(ModEffects.SNAPPED);
		int remaining = instance != null ? instance.getDuration() : 0;
		if (remaining <= TICK_INTERVAL) {
			entity.invulnerableTime = 0;
			entity.hurt(entity.damageSources().wither(), Float.MAX_VALUE);
			if (entity.isAlive()) {
				entity.setHealth(0f);
				if (entity.isAlive()) {
					entity.kill();
				}
			}
			return true;
		}
		float maxHp = entity.getMaxHealth();
		float dmg = Math.max(1.5f, maxHp * 0.10f);
		entity.invulnerableTime = 0;
		entity.hurt(entity.damageSources().wither(), dmg);
		return true;
	}
}
