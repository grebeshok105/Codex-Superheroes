package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.core.emf.EmfBridge;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Registers every {@code superheroes_hl_*} variable the generated jem
 * references. Suppliers read the entity EMF is currently evaluating from
 * {@link EmfBridge#currentEntityUuid()} and pull floats out of
 * {@link HomelanderPoseState}; anything unresolved reads 0 (vanilla pose).
 * Primitive readers keep the only boxing at EMF's {@code Supplier<Float>}
 * boundary — the per-frame path must not allocate.
 */
final class HomelanderEmfVariables {

	private HomelanderEmfVariables() {
	}

	static void register() {
		if (!EmfBridge.isAvailable()) {
			return;
		}
		registerClock("superheroes_hl_w",
				"homelander authored-presentation master weight", HomelanderPoseState::weight);
		registerClock("superheroes_hl_hover_t", "homelander HOVER clip time", HomelanderPoseState::hoverTime);
		registerClock("superheroes_hl_takeoff_t", "homelander TAKEOFF clip time", HomelanderPoseState::takeoffTime);
		registerClock("superheroes_hl_boost_t", "homelander BOOST clip time", HomelanderPoseState::boostTime);
		registerClock("superheroes_hl_clap_t", "homelander HAND CLAP clip time", HomelanderPoseState::clapTime);
		registerClock("superheroes_hl_milk_t", "homelander MILK DRINK clip time", HomelanderPoseState::milkTime);
		registerWeight("superheroes_hl_boost_w", "homelander BOOST clip weight", HomelanderPoseState::boostWeight);
		registerWeight("superheroes_hl_takeoff_w", "homelander TAKEOFF clip weight", HomelanderPoseState::takeoffWeight);
		registerWeight("superheroes_hl_clap_w", "homelander HAND CLAP clip weight", HomelanderPoseState::clapWeight);
		registerWeight("superheroes_hl_milk_w", "homelander MILK DRINK clip weight", HomelanderPoseState::milkWeight);
	}

	@FunctionalInterface
	private interface ClockReader {
		float time(UUID uuid, float partial);
	}

	@FunctionalInterface
	private interface WeightReader {
		float weight(UUID uuid, float partial);
	}

	private static void registerClock(String name, String explanation, ClockReader reader) {
		EmfBridge.registerFloatVariable(name, explanation,
				() -> reader.time(currentUuid(), partialTick()));
	}

	private static void registerWeight(String name, String explanation, WeightReader reader) {
		EmfBridge.registerFloatVariable(name, explanation,
				() -> reader.weight(currentUuid(), partialTick()));
	}

	private static UUID currentUuid() {
		return EmfBridge.currentEntityUuid();
	}

	private static float partialTick() {
		// World-space render sampling convention (PlayerModelPoseMixin, VfxRuntime): under
		// /tick freeze client ticks stop, so clocks genuinely pause; passing true keeps the
		// frozen residual continuous instead of snapping to 1.0 (which (false) would do).
		return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
	}
}
