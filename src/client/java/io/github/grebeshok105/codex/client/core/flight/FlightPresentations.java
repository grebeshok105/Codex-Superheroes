package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hero-id → {@link FlightPresentation} registry. Client modules opt their
 * hero into continuous flight presentation via
 * {@code HeroClientContext.flightPresentation}; hero-scoped flight drivers
 * and the model mixin consult {@link #of} to decide which players get the
 * animated flight pose (players whose hero has no presentation keep the
 * pre-existing static leg pose).
 */
public final class FlightPresentations {
	private static final Map<ResourceLocation, FlightPresentation> BY_HERO = new ConcurrentHashMap<>();

	private FlightPresentations() {
	}

	/** Called from client-module bootstrap only; one presentation per hero id. */
	public static void register(ResourceLocation heroId, FlightPresentation presentation) {
		if (BY_HERO.put(heroId, presentation) != null) {
			throw new IllegalStateException("Duplicate FlightPresentation for hero " + heroId);
		}
	}

	/** The hero's flight presentation, or empty when the hero has none / {@code heroId} is null. */
	public static Optional<FlightPresentation> of(@Nullable ResourceLocation heroId) {
		return Optional.ofNullable(heroId == null ? null : BY_HERO.get(heroId));
	}
}
