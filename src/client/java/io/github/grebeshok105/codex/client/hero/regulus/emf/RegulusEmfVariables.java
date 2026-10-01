package io.github.grebeshok105.codex.client.hero.regulus.emf;

import io.github.grebeshok105.codex.client.core.emf.EmfBridge;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;

/**
 * Registers every variable the merged regulus jem expressions read.
 * Suppliers resolve the entity EMF is currently evaluating via
 * {@link EmfBridge#currentEntityUuid()} and pull floats out of
 * {@link RegulusPoseState}; unresolved entities read 0 (vanilla pose).
 * The {@code var.regulus_<clip>_time} names match the authoring pack's
 * {@code time_input} contract — a start event simply resets the clock.
 */
final class RegulusEmfVariables {

	/** Clip name (authored {@code animation.regulus.<name>}) per one-shot. */
	private static final Map<RegulusPoseState.Clip, String> CLIP_NAMES = Map.ofEntries(
			Map.entry(RegulusPoseState.Clip.LION_HEART_ACTIVATION, "lion_heart_activation"),
			Map.entry(RegulusPoseState.Clip.LION_ROAR, "lion_roar"),
			Map.entry(RegulusPoseState.Clip.MANIA_OF_GREED_CAST, "mania_of_greed_cast"),
			Map.entry(RegulusPoseState.Clip.GREEDS_EMBRACE_CAST, "greeds_embrace_cast"),
			Map.entry(RegulusPoseState.Clip.COUNTER_ATTACK, "counter_attack"),
			Map.entry(RegulusPoseState.Clip.EVANGELIUM_ACTIVATION, "evangelium_activation"),
			Map.entry(RegulusPoseState.Clip.EVANGELIUM_DEACTIVATION, "evangelium_deactivation"),
			Map.entry(RegulusPoseState.Clip.DEBRIS_KICK, "debris_kick"));

	private RegulusEmfVariables() {
	}

	static void register() {
		if (!EmfBridge.isAvailable()) {
			return;
		}
		registerFloat("superheroes_rg_w",
				"regulus authored-presentation master weight", RegulusPoseState::weight);
		registerFloat("superheroes_rg_combat_idle_w",
				"regulus COMBAT_IDLE clip weight", RegulusPoseState::combatIdleWeight);
		registerFloat("superheroes_rg_evangelium_active_idle_w",
				"regulus EVANGELIUM_ACTIVE_IDLE clip weight", RegulusPoseState::evangeliumIdleWeight);
		registerFloat("var.regulus_combat_idle_time",
				"regulus COMBAT_IDLE clip time", RegulusPoseState::combatIdleTime);
		registerFloat("var.regulus_evangelium_active_idle_time",
				"regulus EVANGELIUM_ACTIVE_IDLE clip time", RegulusPoseState::evangeliumIdleTime);
		for (Map.Entry<RegulusPoseState.Clip, String> entry : CLIP_NAMES.entrySet()) {
			RegulusPoseState.Clip clip = entry.getKey();
			String name = entry.getValue();
			registerFloat("var.regulus_" + name + "_time",
					"regulus " + name.toUpperCase() + " clip time",
					(uuid, partial) -> RegulusPoseState.oneShotTime(uuid, clip, partial));
			registerFloat("superheroes_rg_" + name + "_w",
					"regulus " + name.toUpperCase() + " clip weight",
					(uuid, partial) -> RegulusPoseState.oneShotWeight(uuid, clip, partial));
		}
	}

	@FunctionalInterface
	private interface FloatReader {
		float read(UUID uuid, float partial);
	}

	private static void registerFloat(String name, String explanation, FloatReader reader) {
		EmfBridge.registerFloatVariable(name, explanation,
				() -> reader.read(currentUuid(), partialTick()));
	}

	private static UUID currentUuid() {
		return EmfBridge.currentEntityUuid();
	}

	private static float partialTick() {
		// World-space render sampling convention (HomelanderEmfVariables): under
		// /tick freeze client ticks stop, so clocks genuinely pause; passing true
		// keeps the frozen residual continuous instead of snapping to 1.0.
		return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
	}
}
