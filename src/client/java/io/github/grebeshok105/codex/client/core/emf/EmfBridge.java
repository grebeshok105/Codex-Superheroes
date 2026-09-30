package io.github.grebeshok105.codex.client.core.emf;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.entity_model_features.EMFAnimationApi;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * All direct {@code traben.*} access goes through this class. EMF/ETF ship
 * jar-in-jar for the supported client configuration, so the bridge is a thin
 * availability-gated adapter: every entry point no-ops or returns empty when
 * EMF is absent (dedicated server, stripped classpath).
 */
public final class EmfBridge {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModId.MOD_ID);
	private static final String EMF_MOD_ID = "entity_model_features";

	private EmfBridge() {
	}

	public static boolean isAvailable() {
		return FabricLoader.getInstance().isModLoaded(EMF_MOD_ID);
	}

	/**
	 * Register a singleton animation variable (evaluated once per part per
	 * frame — the supplier must not allocate). {@code name} is the full
	 * variable name as the jem references it, e.g. {@code superheroes_hl_w}.
	 */
	public static void registerFloatVariable(String name, String explanation, Supplier<Float> supplier) {
		if (!isAvailable()) {
			return;
		}
		try {
			EMFAnimationApi.registerSingletonAnimationVariable(ModId.MOD_ID, name, explanation, supplier);
		} catch (Exception e) {
			LOGGER.error("EMF variable {} failed to register", name, e);
		}
	}

	/** Uuid of the entity EMF is currently evaluating, or null outside animation. */
	public static @Nullable UUID currentEntityUuid() {
		if (!isAvailable()) {
			return null;
		}
		var entity = EMFAnimationApi.getCurrentEntity();
		return entity == null ? null : entity.etf$getUuid();
	}

	/**
	 * Register a force-vanilla-model condition: EMF keeps the vanilla player
	 * model for entities the predicate accepts. Applied to players only.
	 */
	public static void registerVanillaModelCondition(Predicate<UUID> condition) {
		if (!isAvailable()) {
			return;
		}
		try {
			EMFAnimationApi.registerVanillaModelCondition(
					entity -> entity instanceof AbstractClientPlayer && condition.test(entity.etf$getUuid()));
		} catch (Exception e) {
			LOGGER.error("EMF vanilla-model condition failed to register", e);
		}
	}
}
