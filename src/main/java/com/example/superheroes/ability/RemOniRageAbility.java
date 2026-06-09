package com.example.superheroes.ability;

import com.example.superheroes.effect.RemDemonismController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RemOniRageAbility implements Ability {
	private static final int EXIT_COOLDOWN_TICKS = 8 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_ONI_RAGE;
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
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId())
				&& !RemDemonismController.isActive(player)
				&& RemDemonismController.getCharge(player) >= RemDemonismController.MAX_DEMONISM - 0.001f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return RemDemonismController.tryActivate(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		RemDemonismController.stopManual(player);
		AbilityCooldowns.setCooldownTicks(player, getId(), EXIT_COOLDOWN_TICKS);
	}
}
