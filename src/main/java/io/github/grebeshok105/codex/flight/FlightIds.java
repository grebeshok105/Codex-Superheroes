package io.github.grebeshok105.codex.flight;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Ability ids the shared flight mechanic recognizes, and the {@code id -> FlightMode} mapping.
 * The mechanic owns these literals so {@code flight/} never reaches into {@code mechanic.ability};
 * hero ability classes and {@code SharedAbilityIds} alias them.
 */
public final class FlightIds {
	public static final ResourceLocation FLIGHT = ModId.of("flight");
	public static final ResourceLocation IRON_MAN_FLIGHT = ModId.of("iron_man_flight");
	public static final ResourceLocation SUPERSONIC = ModId.of("supersonic");

	private FlightIds() {
	}

	public static FlightMode modeOf(ResourceLocation abilityId) {
		if (SUPERSONIC.equals(abilityId)) {
			return FlightMode.SUPERSONIC;
		}
		if (IRON_MAN_FLIGHT.equals(abilityId)) {
			return FlightMode.IRON_MAN;
		}
		if (FLIGHT.equals(abilityId)) {
			return FlightMode.NORMAL;
		}
		return null;
	}
}
