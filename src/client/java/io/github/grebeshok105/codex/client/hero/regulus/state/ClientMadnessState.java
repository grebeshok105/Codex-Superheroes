package io.github.grebeshok105.codex.client.hero.regulus.state;

import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusBonusLife;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.minecraft.client.Minecraft;

/**
 * Read-through view of the synced {@code regulus_madness} attachment — there is
 * no client-side copy anymore (the {@code madness_sync}/{@code madness_visual}
 * payloads are gone). Progress is always derived from
 * {@code deadline - level.getGameTime()}, never from wall-clock, so a client
 * that observes the attachment late sees the same deadlines as everyone else.
 *
 * <p>The only owned state is the madness edge flag {@link #madnessEdge()}
 * consumes once per client tick (BloodRain trigger/clear); it is session state
 * and must reset on disconnect.
 */
public final class ClientMadnessState {
	private static boolean lastMadness = false;

	static {
		ClientSessionState.register(ClientMadnessState::reset);
	}

	private ClientMadnessState() {
	}

	private static RegulusMadnessState state() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return RegulusMadnessState.EMPTY;
		}
		return mc.player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
	}

	private static long gameTime() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0L : mc.level.getGameTime();
	}

	public static boolean isMadness() {
		return state().madness();
	}

	public static boolean isReading() {
		return state().isReading(gameTime());
	}

	/** Absolute game-tick deadline of the open ritual window (0 when not reading). */
	public static long ritualUntilTick() {
		return state().ritualUntilTick();
	}

	/** Absolute game-tick deadline of the madness (0 = unbounded / not mad). */
	public static long madnessUntilTick() {
		return state().madnessUntilTick();
	}

	/**
	 * Ticks elapsed since the madness began, derived from the synced deadline —
	 * correct even for a client that observed it after the ritual completed.
	 */
	public static long madnessElapsedTicks() {
		RegulusMadnessState s = state();
		if (!s.madness() || s.madnessUntilTick() <= 0L) {
			return 0L;
		}
		long start = s.madnessUntilTick() - RegulusMadnessController.MADNESS_DURATION_TICKS;
		return Math.max(0L, gameTime() - start);
	}

	public static boolean isBonusLifeAvailable() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null
				&& Boolean.TRUE.equals(mc.player.getAttached(RegulusBonusLife.ATTACHMENT));
	}

	/**
	 * The madness rising/falling edge for HUD events (call once per client tick):
	 * {@code 1} on the first tick mad, {@code -1} on the first tick no longer mad,
	 * {@code 0} otherwise.
	 */
	public static int madnessEdge() {
		boolean cur = isMadness();
		int edge = cur == lastMadness ? 0 : (cur ? 1 : -1);
		lastMadness = cur;
		return edge;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		lastMadness = false;
	}
}
