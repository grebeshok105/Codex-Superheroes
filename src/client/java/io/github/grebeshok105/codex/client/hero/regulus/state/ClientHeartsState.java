package io.github.grebeshok105.codex.client.hero.regulus.state;

import io.github.grebeshok105.codex.client.ClientSessionState;
import java.util.List;
import java.util.Set;

/**
 * Owner-view of the little-king hearts, fed by {@code HeartsSyncS2CPayload}: the bearer
 * entity network ids (for {@code ClientLevel.getEntity(int)} outline lookups), whether
 * lion heart is active, and its overheat counter. Server-authoritative — reset on
 * disconnect like every session holder.
 */
public final class ClientHeartsState {
	private static volatile Set<Integer> heartEntityIds = Set.of();
	private static volatile boolean lionHeartActive = false;
	private static volatile int overheatTicks = 0;

	static {
		ClientSessionState.register(ClientHeartsState::reset);
	}

	private ClientHeartsState() {
	}

	public static void update(List<Integer> ids, boolean lionActive, int overheat) {
		heartEntityIds = ids.isEmpty() ? Set.of() : Set.copyOf(ids);
		lionHeartActive = lionActive;
		overheatTicks = overheat;
	}

	public static Set<Integer> heartEntityIds() {
		return heartEntityIds;
	}

	public static boolean isLionHeartActive() {
		return lionHeartActive;
	}

	public static int overheatTicks() {
		return overheatTicks;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		heartEntityIds = Set.of();
		lionHeartActive = false;
		overheatTicks = 0;
	}
}
