package io.github.grebeshok105.codex.client.core.input;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Client-side input lock registry — the generic mechanism behind cinematics like Pandora's
 * revival cut-scene. A hero client module registers a <i>reason</i>: while any reason is
 * active the {@code InputLock*} mixins freeze movement, swallow key/mouse presses, and lock
 * camera rotation.
 *
 * <p>The mixins never cancel {@code GLFW_RELEASE} actions (audit B15): {@code KeyMapping.set}
 * only runs on release, so a key held when the lock engages and released mid-lock would
 * otherwise stay logically pressed — phantom input after the lock ends.
 */
public final class InputLock {
	private static final Map<ResourceLocation, BooleanSupplier> REASONS = new LinkedHashMap<>();

	private InputLock() {
	}

	/** Registers a lock reason. {@code active} is polled every input event; registration order is preserved. */
	public static void register(ResourceLocation reason, BooleanSupplier active) {
		REASONS.put(reason, active);
	}

	/** True while at least one registered reason is active. */
	public static boolean isLocked() {
		for (BooleanSupplier active : REASONS.values()) {
			if (active.getAsBoolean()) {
				return true;
			}
		}
		return false;
	}
}
