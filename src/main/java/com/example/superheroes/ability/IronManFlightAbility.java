package com.example.superheroes.ability;

import com.example.superheroes.effect.FlightController;
import com.example.superheroes.flight.FlightMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class IronManFlightAbility implements Ability {
	public static final float ENERGY_FLOOR = 100f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_FLIGHT;
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
	public boolean tryActivate(ServerPlayer player) {
		if (!FlightController.start(player, FlightMode.IRON_MAN)) {
			return false;
		}
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.6f, 1.4f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		FlightController.stop(player, getId());
	}
}
