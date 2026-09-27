package io.github.grebeshok105.codex.client.core.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Suppliers that veto the vanilla crosshair (e.g. Iron Man draws his own
 * arc-reactor reticle). Consulted by the Gui crosshair mixin after the radial
 * menu check; each supplier self-gates on its hero being active.
 */
public final class CrosshairSuppressions {
	private static final List<BooleanSupplier> SUPPRESSIONS = new ArrayList<>();

	private CrosshairSuppressions() {
	}

	public static void register(BooleanSupplier suppression) {
		SUPPRESSIONS.add(suppression);
	}

	public static boolean shouldSuppress() {
		for (BooleanSupplier suppression : SUPPRESSIONS) {
			if (suppression.getAsBoolean()) {
				return true;
			}
		}
		return false;
	}
}
