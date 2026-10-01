package io.github.grebeshok105.codex.client.hero.regulus.state;

import io.github.grebeshok105.codex.client.ClientSessionState;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;

/**
 * Owner-view of the little-king hearts, fed by {@code HeartsSyncS2CPayload}: the bearer
 * entity network ids (for {@code ClientLevel.getEntity(int)} outline lookups), whether
 * lion heart is active, and its overheat counter. Server-authoritative — reset on
 * disconnect like every session holder.
 */
public final class ClientHeartsState {
	private static final long FORCED_OFF_FLASH_TICKS = 10L;

	private static volatile Set<Integer> heartEntityIds = Set.of();
	private static volatile boolean lionHeartActive = false;
	private static volatile int overheatTicks = 0;
	/** Level gameTime the forced-off flash dies at — armed when the shield drops mid-overheat. */
	private static volatile long forcedOffFlashUntil = Long.MIN_VALUE;

	static {
		ClientSessionState.register(ClientHeartsState::reset);
	}

	private ClientHeartsState() {
	}

	public static void update(List<Integer> ids, boolean lionActive, int overheat) {
		// An overheat-burning shield that vanishes was forced off at hp<=4 — the client
		// sees no dedicated packet, so the payload edge (active→inactive while
		// overheat>0) arms the full-intensity flash itself.
		boolean forcedOff = lionHeartActive && !lionActive && overheatTicks > 0;
		heartEntityIds = ids.isEmpty() ? Set.of() : Set.copyOf(ids);
		lionHeartActive = lionActive;
		overheatTicks = overheat;
		if (forcedOff) {
			var level = Minecraft.getInstance().level;
			forcedOffFlashUntil = (level == null ? 0L : level.getGameTime()) + FORCED_OFF_FLASH_TICKS;
		}
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

	/** True for {@value #FORCED_OFF_FLASH_TICKS} client ticks after a forced-off edge. */
	public static boolean forcedOffFlash() {
		var level = Minecraft.getInstance().level;
		return level != null && level.getGameTime() < forcedOffFlashUntil;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		heartEntityIds = Set.of();
		lionHeartActive = false;
		overheatTicks = 0;
		forcedOffFlashUntil = Long.MIN_VALUE;
	}
}
