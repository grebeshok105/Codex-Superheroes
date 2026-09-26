package io.github.grebeshok105.codex.lifecycle;

import io.github.grebeshok105.codex.SuperheroesMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Single dispatch point for player lifecycle events that ability/effect controllers
 * subscribe to for cleanup. Game-state cleanup must NOT hang off
 * {@code ServerPlayConnectionEvents.DISCONNECT} — Fabric can fire it from the Netty thread
 * ({@code Connection.channelInactive}). {@link ServerPlayerEvents#LEAVE} is the safe hook:
 * it fires on the server thread before the player is saved and removed from the world.
 */
public final class PlayerLifecycle {
	private static final List<Consumer<ServerPlayer>> JOIN = new ArrayList<>();
	private static final List<Consumer<ServerPlayer>> LEAVE = new ArrayList<>();
	private static final List<Consumer<ServerPlayer>> DEATH = new ArrayList<>();
	private static final List<Consumer<ServerPlayer>> RESPAWN = new ArrayList<>();
	private static final List<Consumer<MinecraftServer>> STOPPED = new ArrayList<>();

	private PlayerLifecycle() {
	}

	/** Player joined the server. */
	public static void onJoin(Consumer<ServerPlayer> hook) {
		JOIN.add(hook);
	}

	/** Player is leaving: server thread, before the player is saved and removed. */
	public static void onLeave(Consumer<ServerPlayer> hook) {
		LEAVE.add(hook);
	}

	/** Player died (after death was allowed). Runs before the respawned entity exists. */
	public static void onDeath(Consumer<ServerPlayer> hook) {
		DEATH.add(hook);
	}

	/** Player respawned; the hook receives the new entity. */
	public static void onRespawn(Consumer<ServerPlayer> hook) {
		RESPAWN.add(hook);
	}

	/**
	 * Server finished shutting down — covers both real shutdown and single-player world
	 * switches. Static per-player state must be dropped here so it cannot leak into the
	 * next world.
	 */
	public static void onServerStopped(Consumer<MinecraftServer> hook) {
		STOPPED.add(hook);
	}

	public static void init() {
		ServerPlayerEvents.JOIN.register(player -> fire(JOIN, player, "join"));
		ServerPlayerEvents.LEAVE.register(player -> fire(LEAVE, player, "leave"));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				fire(DEATH, player, "death");
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> fire(RESPAWN, newPlayer, "respawn"));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			for (Consumer<MinecraftServer> hook : STOPPED) {
				try {
					hook.accept(server);
				} catch (Throwable t) {
					SuperheroesMod.LOGGER.error("PlayerLifecycle server-stopped hook failed", t);
				}
			}
		});
	}

	private static void fire(List<Consumer<ServerPlayer>> hooks, ServerPlayer player, String event) {
		for (Consumer<ServerPlayer> hook : hooks) {
			try {
				hook.accept(player);
			} catch (Throwable t) {
				SuperheroesMod.LOGGER.error("PlayerLifecycle {} hook failed", event, t);
			}
		}
	}
}
