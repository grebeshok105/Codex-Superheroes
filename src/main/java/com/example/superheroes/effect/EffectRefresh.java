package com.example.superheroes.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Re-applies a mob effect only when it is missing or past half its duration.
 * Every {@code addEffect} call sends a {@code ClientboundUpdateMobEffectPacket},
 * so controllers that refresh a lock/aura each tick used to emit one packet per
 * effect per tick per target (audit §3). Refreshing at half-life keeps behavior
 * identical while cutting the traffic to a handful of packets per effect.
 */
public final class EffectRefresh {
	private EffectRefresh() {
	}

	public static void refresh(LivingEntity target, Holder<MobEffect> effect, int duration, int amplifier,
			boolean ambient, boolean visible, boolean showIcon) {
		MobEffectInstance current = target.getEffect(effect);
		if (current == null || current.getAmplifier() != amplifier || current.getDuration() <= duration / 2) {
			target.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon));
		}
	}
}
