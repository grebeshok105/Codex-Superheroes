package com.example.superheroes.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class LokiGlamourAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.LOKI_GLAMOUR;
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
		return 0.4f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		applyEffects(player);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		applyEffects(player);
	}

	private static void applyEffects(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, true, false, true));
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		player.removeEffect(MobEffects.INVISIBILITY);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.DIG_SPEED);
	}
}
