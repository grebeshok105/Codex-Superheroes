package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.flight.FlightPresentation;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.hero.homelander.HomelanderVfxIds;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.resources.ResourceLocation;

/**
 * Homelander's Visual Core registration hub — every {@code HomelanderVfxIds}
 * effect/channel factory and the flight presentation get wired here. Task 8
 * registers the LANDING event plus the flight trail/boost effects and the
 * {@link FlightPresentation}; Tasks 9–12 extend {@link #register} with their
 * ids (laser channel, sun, iron fists, clap, roar, milk).
 */
public final class HomelanderFx {
	private HomelanderFx() {
	}

	public static void register(HeroClientContext ctx) {
		ctx.vfx(HomelanderVfxIds.LANDING, FlightFx::landing);
		ctx.vfx(FlightFx.TRAIL, FlightFx::trail);
		ctx.vfx(FlightFx.BOOST, FlightFx::boost);
		ctx.flightPresentation(new FlightPresentation(
				clip("flight_takeoff"),
				clip("flight_hover"),
				clip("flight_cruise"),
				clip("flight_boost"),
				clip("flight_land"),
				FlightFx.TRAIL,
				FlightFx.BOOST,
				HomelanderSounds.FLIGHT_LOOP,
				HomelanderSounds.FLIGHT_TAKEOFF,
				HomelanderSounds.FLIGHT_BOOST,
				HomelanderSounds.FLIGHT_LAND));
	}

	private static ResourceLocation clip(String name) {
		return ModId.of("homelander/" + name);
	}
}
