package io.github.grebeshok105.codex.mechanic.flight;

import io.github.grebeshok105.codex.ModId;

import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;

public final class FlightAbilityState {
	private static final ResourceLocation FLIGHT_ID = ModId.of("flight");
	private FlightAbilityState() {
	}

	public static boolean isFlightAbility(ResourceLocation abilityId) {
		return FLIGHT_ID.equals(abilityId)
				|| FlightIds.IRON_MAN_FLIGHT.equals(abilityId)
				|| FlightIds.SUPERSONIC.equals(abilityId);
	}

	public static boolean isActive(HeroData data) {
		return activeMode(data) != null;
	}

	public static FlightMode activeMode(HeroData data) {
		return activeModeExcept(data, null);
	}

	public static FlightMode activeModeExcept(HeroData data, ResourceLocation removedAbility) {
		if (data.isActive(FlightIds.SUPERSONIC)) {
			if (!FlightIds.SUPERSONIC.equals(removedAbility)) {
				return FlightMode.SUPERSONIC;
			}
		}
		if (data.isActive(FlightIds.IRON_MAN_FLIGHT)) {
			if (!FlightIds.IRON_MAN_FLIGHT.equals(removedAbility)) {
				return FlightMode.IRON_MAN;
			}
		}
		if (data.isActive(FLIGHT_ID)) {
			if (!FLIGHT_ID.equals(removedAbility)) {
				return FlightMode.NORMAL;
			}
		}
		return null;
	}
}
