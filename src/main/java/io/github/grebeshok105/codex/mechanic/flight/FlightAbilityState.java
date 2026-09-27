package io.github.grebeshok105.codex.mechanic.flight;

import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.resources.ResourceLocation;

public final class FlightAbilityState {
	private FlightAbilityState() {
	}

	public static boolean isFlightAbility(ResourceLocation abilityId) {
		return FlightIds.modeOf(abilityId) != null;
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
		if (data.isActive(FlightIds.FLIGHT)) {
			if (!FlightIds.FLIGHT.equals(removedAbility)) {
				return FlightMode.NORMAL;
			}
		}
		return null;
	}
}
