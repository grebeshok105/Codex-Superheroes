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
		// entity overload → звук летит вместе с Железным человеком (он сразу взлетает),
		// а не остаётся в точке активации
		level.playSound(null, player,
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.6f, 1.4f);
		int suitVariant = player.getAttachedOrCreate(com.example.superheroes.attachment.ModAttachments.SUIT_VARIANT);
		if (suitVariant == 1) { // Mark 85
			level.playSound(null, player,
					com.example.superheroes.sound.ModSounds.IRONMAN_JARVIS_MARK85_PRESET,
					SoundSource.PLAYERS, 0.9f, 1.0f);
		}
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
