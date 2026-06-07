package com.example.superheroes.ability;

import com.example.superheroes.effect.FlightController;
import com.example.superheroes.flight.FlightMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SupersonicAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.SUPERSONIC;
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
		return 6.0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!FlightController.start(player, FlightMode.SUPERSONIC)) {
			return false;
		}
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 1.6f);
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
