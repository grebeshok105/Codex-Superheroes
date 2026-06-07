package com.example.superheroes.flight;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.transform.HeroData;
import net.minecraft.resources.ResourceLocation;

public final class FlightAbilityState {
	private FlightAbilityState() {
	}

	public static boolean isFlightAbility(ResourceLocation abilityId) {
		return AbilityIds.FLIGHT.equals(abilityId)
				|| AbilityIds.IRON_MAN_FLIGHT.equals(abilityId)
				|| AbilityIds.SUPERSONIC.equals(abilityId);
	}

	public static boolean isActive(HeroData data) {
		return activeMode(data) != null;
	}

	public static FlightMode activeMode(HeroData data) {
		return activeModeExcept(data, null);
	}

	public static FlightMode activeModeExcept(HeroData data, ResourceLocation removedAbility) {
		if (data.isActive(AbilityIds.SUPERSONIC)) {
			if (!AbilityIds.SUPERSONIC.equals(removedAbility)) {
				return FlightMode.SUPERSONIC;
			}
		}
		if (data.isActive(AbilityIds.IRON_MAN_FLIGHT)) {
			if (!AbilityIds.IRON_MAN_FLIGHT.equals(removedAbility)) {
				return FlightMode.IRON_MAN;
			}
		}
		if (data.isActive(AbilityIds.FLIGHT)) {
			if (!AbilityIds.FLIGHT.equals(removedAbility)) {
				return FlightMode.NORMAL;
			}
		}
		return null;
	}
}
