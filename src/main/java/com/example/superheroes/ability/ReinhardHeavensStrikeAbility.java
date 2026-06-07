package com.example.superheroes.ability;

import com.example.superheroes.effect.HeavensStrikeController;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ReinhardHeavensStrikeAbility implements Ability {
	private static final int COOLDOWN_TICKS = 30 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_HEAVENS_STRIKE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 400f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (AbilityCooldowns.isOnCooldown(player, getId())) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_heavens_strike.cooldown"), true);
			return false;
		}
		if (HeavensStrikeController.isCharging(player)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!HeavensStrikeController.start(player, HeavensStrikeController.Variant.REINHARD)) {
			return false;
		}
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_heavens_strike.charging", "4.0"), true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
