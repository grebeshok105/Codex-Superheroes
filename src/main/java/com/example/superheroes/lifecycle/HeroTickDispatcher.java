package com.example.superheroes.lifecycle;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Single {@code END_SERVER_TICK} dispatch for gameplay tick tasks (audit debt 3).
 *
 * <p>Replaces the per-player call list in {@code SuperheroesMod}: ordering is explicit
 * ({@link Phase}), dead players are skipped once centrally instead of per call site, and
 * {@code HeroData} is fetched once per player for all player-phase tasks.
 *
 * <p>Phases run in enum order every tick: {@link Phase#GLOBAL} server-wide tasks,
 * {@link Phase#LEVELS} per-level tasks, {@link Phase#PLAYERS} per-player tasks,
 * {@link Phase#ABILITY_ACTIVE} per-player tasks gated on an active ability id.
 */
public final class HeroTickDispatcher {

	public enum Phase {
		GLOBAL, LEVELS, PLAYERS, ABILITY_ACTIVE
	}

	@FunctionalInterface
	public interface PlayerTask {
		void tick(MinecraftServer server, ServerPlayer player, HeroData data);
	}

	private record AbilityTask(ResourceLocation abilityId, PlayerTask task) {
	}

	private static final List<Consumer<MinecraftServer>> GLOBAL_TASKS = new ArrayList<>();
	private static final List<BiConsumer<MinecraftServer, ServerLevel>> LEVEL_TASKS = new ArrayList<>();
	private static final List<PlayerTask> PLAYER_TASKS = new ArrayList<>();
	private static final List<AbilityTask> ABILITY_TASKS = new ArrayList<>();

	private HeroTickDispatcher() {
	}

	/** Server-wide task, {@link Phase#GLOBAL}. */
	public static void onGlobalTick(Consumer<MinecraftServer> task) {
		GLOBAL_TASKS.add(task);
	}

	/** Per-level task, {@link Phase#LEVELS}: runs once per loaded level. */
	public static void onLevelTick(BiConsumer<MinecraftServer, ServerLevel> task) {
		LEVEL_TASKS.add(task);
	}

	/**
	 * Per-player task, {@link Phase#PLAYERS}: runs for every player that is not dead
	 * (audit B17), with the player's {@code HeroData} already fetched.
	 */
	public static void onPlayerTick(PlayerTask task) {
		PLAYER_TASKS.add(task);
	}

	/** Per-player task gated on {@code data.isActive(abilityId)}, {@link Phase#ABILITY_ACTIVE}. */
	public static void onActiveAbilityTick(ResourceLocation abilityId, PlayerTask task) {
		ABILITY_TASKS.add(new AbilityTask(abilityId, task));
	}

	public static void tick(MinecraftServer server) {
		for (Consumer<MinecraftServer> task : GLOBAL_TASKS) {
			task.accept(server);
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (BiConsumer<MinecraftServer, ServerLevel> task : LEVEL_TASKS) {
				task.accept(server, level);
			}
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// Dead players stay in the player list until respawn — ability ticks must not
			// run on a corpse (audit B17).
			if (player.isDeadOrDying()) {
				continue;
			}
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			for (PlayerTask task : PLAYER_TASKS) {
				task.tick(server, player, data);
			}
			for (AbilityTask task : ABILITY_TASKS) {
				if (data.isActive(task.abilityId())) {
					task.task().tick(server, player, data);
				}
			}
		}
	}
}
