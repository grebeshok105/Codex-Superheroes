package com.example.superheroes.ability;

import com.example.superheroes.effect.HeavensStrikeController;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RaidenPlungingStrikeAbility implements Ability {
	private static final float COST = 280f;
	private static final int COOLDOWN_TICKS = 18 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_PLUNGING_STRIKE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return COST;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (AbilityCooldowns.isOnCooldown(player, getId())) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_plunging_strike.cooldown"), true);
			return false;
		}
		if (HeavensStrikeController.isCharging(player)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!HeavensStrikeController.start(player, HeavensStrikeController.Variant.RAIDEN)) {
			return false;
		}
		player.displayClientMessage(
				Component.translatable("ability.superheroes.raiden_plunging_strike.charging", "4.0"), true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void onLanding(ServerPlayer player) {
		// no-op: replaced by HeavensStrikeController windup-based impact
	}
}
