package io.github.grebeshok105.codex.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Session-scoped reset hub for every piece of client-side state (audit B15).
 *
 * <p>Each {@code Client*State} holder (plus the few session singletons outside that naming
 * scheme) registers its own reset in a static initializer, and the disconnect path runs ALL of
 * them through {@link #resetAll()}. The registry is the single enumeration point — there is no
 * per-class list in {@link SuperheroesClient} left to go stale when a new state class appears.
 *
 * <p>Registration lives in the holder's static initializer on purpose: a holder that was never
 * touched in a session is never class-loaded, carries only default state, and needs no reset.
 * {@link io.github.grebeshok105.codex.ProjectSanityTest} asserts every {@code Client*State} source
 * keeps its {@code ClientSessionState.register(...)} call.
 */
public final class ClientSessionState {
	private static final List<Runnable> RESETS = new CopyOnWriteArrayList<>();

	private ClientSessionState() {
	}

	/** Called from a holder's static initializer; runs once per holder per JVM. */
	public static void register(Runnable reset) {
		RESETS.add(reset);
	}

	/** Drop every registered piece of session state (disconnect / world leave). */
	public static void resetAll() {
		for (Runnable reset : RESETS) {
			reset.run();
		}
	}
}
