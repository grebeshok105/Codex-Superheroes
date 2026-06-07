package com.example.superheroes.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Counter Stance — Капитан Америка переходит в защитную стойку. Пока активна:
 *  — Resistance II (–50% входящего урона),
 *  — Strength I (+3 урона ближнего боя),
 *  — Speed I (мобильность для уклонений).
 * Расход энергии: 4/тик (~80/сек), пока в стойке.
 */
public final class CapCounterStanceAbility implements Ability {
	private static final int EFFECT_DURATION = 40;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.CAP_COUNTER_STANCE;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 4f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		applyStance(player);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		applyStance(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
	}

	private static void applyStance(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 0, true, false, true));
	}
}
