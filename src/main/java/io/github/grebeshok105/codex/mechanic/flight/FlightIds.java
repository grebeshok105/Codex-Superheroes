package io.github.grebeshok105.codex.mechanic.flight;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Persisted ability ids owned by the flight mechanic. Leaf by design: the flight
 * package is referenced by both `mechanic.ability` (shared abilities) and legacy
 * `ability.*` holders, so it must not import either of them.
 */
public final class FlightIds {
	public static final ResourceLocation IRON_MAN_FLIGHT = ModId.of("iron_man_flight");
	public static final ResourceLocation SUPERSONIC = ModId.of("supersonic");

	private FlightIds() {
	}
}
