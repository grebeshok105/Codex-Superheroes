package io.github.grebeshok105.codex.core.lifecycle;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Neutral seam between hero code and {@code compat/<mod>/} bridges. The layering
 * rules forbid hero→compat and compat→hero edges entirely, so cross-mod
 * notifications flow through here: compat classes subscribe during composition-root
 * wiring, hero code fires the event.
 *
 * <p>Single consumer today (falbiks' snap-strip); justified by the overview's
 * dependency matrix — this is the only legal hero↔compat channel.
 */
public final class CrossModHooks {
	private static final List<Consumer<Player>> SNAP_VICTIMS = new ArrayList<>();

	private CrossModHooks() {
	}

	/** Compat bridges subscribe a per-victim strip (runs inside the snap's victim loop). */
	public static void onSnapVictim(Consumer<Player> hook) {
		SNAP_VICTIMS.add(hook);
	}

	public static void fireSnapVictim(Player victim) {
		for (Consumer<Player> hook : SNAP_VICTIMS) {
			hook.accept(victim);
		}
	}
}
