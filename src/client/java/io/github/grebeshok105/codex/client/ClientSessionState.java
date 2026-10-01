package io.github.grebeshok105.codex.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

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
 *
 * <p>Level swaps (dimension change, respawn, rejoin into the same session) do
 * <em>not</em> fire {@code DISCONNECT}, and {@code ClientPlayConnectionEvents.JOIN}
 * only fires on the initial login. A narrower reset exists for the few caches
 * that key state to a specific {@link ClientLevel} instance — e.g.
 * {@code RenderedPoseCache} snapshots are keyed by entity id, so a cache entry
 * from a swapped-out level could hand a renderer a pose from the wrong
 * dimension. Holders opt in via {@link #registerLevelReset(Runnable)}; a
 * per-tick watcher fires those callbacks when {@code Minecraft.level}'s
 * identity changes. Uuid/entity-id keyed caches that reseed themselves should
 * keep their entries for continuity instead.
 */
public final class ClientSessionState {
	private static final List<Runnable> RESETS = new CopyOnWriteArrayList<>();
	private static final List<Runnable> LEVEL_RESETS = new CopyOnWriteArrayList<>();

	private static @Nullable ClientLevel lastLevel;

	private ClientSessionState() {
	}

	/** Registers the disconnect hook that drops every registered piece of session state. */
	public static void init() {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetAll());
		ClientTickEvents.END_CLIENT_TICK.register(ClientSessionState::watchLevel);
	}

	/** Called from a holder's static initializer; runs once per holder per JVM. */
	public static void register(Runnable reset) {
		RESETS.add(reset);
	}

	/**
	 * Called from a holder's static initializer; fires when the {@link ClientLevel}
	 * instance swaps mid-session (dimension change, respawn, rejoin). Use only for
	 * state that cannot survive a level swap — uuid/entity-id keyed caches that
	 * reseed themselves keep their entries for continuity instead.
	 */
	public static void registerLevelReset(Runnable reset) {
		LEVEL_RESETS.add(reset);
	}

	/** Drop every registered piece of session state (disconnect / world leave). */
	public static void resetAll() {
		for (Runnable reset : RESETS) {
			reset.run();
		}
	}

	/** Fires every level-swap reset; driven by {@link #watchLevel}. */
	static void resetLevel() {
		for (Runnable reset : LEVEL_RESETS) {
			reset.run();
		}
	}

	private static void watchLevel(Minecraft client) {
		ClientLevel level = client.level;
		if (level == lastLevel) {
			return;
		}
		lastLevel = level;
		if (level != null) {
			resetLevel();
		}
	}
}
