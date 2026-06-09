package com.example.superheroes.ability;

import com.example.superheroes.effect.RemDemonismController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RemHumaIceSpikesAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_HUMA_ICE_SPIKES;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 55f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return RemDemonismController.isActive(player)
				&& !RemDemonismController.isIceWaveActive(player)
				&& !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return RemDemonismController.startIceSpikeWave(player);
	}
}
