package io.github.grebeshok105.codex.core.lifecycle;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Single server-tick dispatch for gameplay tick tasks (audit debt 3): {@link Phase#START}
 * tasks run on {@code START_SERVER_TICK}, all other phases on {@code END_SERVER_TICK}.
 *
 * <p>Replaces the per-player call list in {@code SuperheroesMod}: ordering is explicit
 * ({@link Phase}), dead players are skipped once centrally instead of per call site, and
 * {@code HeroData} is fetched once per player for all player-phase tasks.
 *
 * <p>Phases run in enum order every tick: {@link Phase#START} on the START event, then
 * inside the END tick {@link Phase#EARLY} (former self-registered END listeners, until D2b
 * empty), {@link Phase#GLOBAL} server-wide tasks, {@link Phase#LEVELS} per-level tasks,
 * {@link Phase#PLAYERS} per-player tasks, {@link Phase#ABILITY_ACTIVE} per-player tasks
 * gated on an active ability id.
 */
public final class HeroTickDispatcher {

	public enum Phase {
		/** START_SERVER_TICK — before vanilla ticks levels and entities. */
		START,
		/** END_SERVER_TICK, before GLOBAL — former self-registered END listeners, in module order. */
		EARLY,
		GLOBAL, LEVELS, PLAYERS, ABILITY_ACTIVE
	}

	@FunctionalInterface
	public interface PlayerTask {
		void tick(MinecraftServer server, ServerPlayer player, HeroData data);
	}

	private record AbilityTask(ResourceLocation abilityId, PlayerTask task) {
	}

	private static final List<Consumer<MinecraftServer>> START_TASKS = new ArrayList<>();
	private static final List<Consumer<MinecraftServer>> EARLY_TASKS = new ArrayList<>();
	private static final List<Consumer<MinecraftServer>> GLOBAL_TASKS = new ArrayList<>();
	private static final List<BiConsumer<MinecraftServer, ServerLevel>> LEVEL_TASKS = new ArrayList<>();
	private static final List<PlayerTask> PLAYER_TASKS = new ArrayList<>();
	private static final List<AbilityTask> ABILITY_TASKS = new ArrayList<>();

	private HeroTickDispatcher() {
	}

	/** Server-wide task, {@link Phase#START}. */
	public static void onServerTickStart(Consumer<MinecraftServer> task) {
		START_TASKS.add(task);
	}

	/** Server-wide task, {@link Phase#EARLY}. */
	public static void onEarlyTick(Consumer<MinecraftServer> task) {
		EARLY_TASKS.add(task);
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

	/** Per-player task for players whose current hero is {@code heroId}, {@link Phase#PLAYERS}. */
	public static void onHeroTick(ResourceLocation heroId, PlayerTask task) {
		onPlayerTick((server, player, data) -> {
			if (data.hasHero() && heroId.equals(data.heroId())) {
				task.tick(server, player, data);
			}
		});
	}

	/** Hooks the dispatcher into Fabric's tick events; call where SuperheroesMod registered END_SERVER_TICK before. */
	public static void init() {
		ServerTickEvents.START_SERVER_TICK.register(server -> START_TASKS.forEach(task -> task.accept(server)));
		ServerTickEvents.END_SERVER_TICK.register(HeroTickDispatcher::tick);
	}

	/** The module-facing facade over the static on… methods. */
	public static TickRegistrar registrar() {
		return Registrar.INSTANCE;
	}

	public static void tick(MinecraftServer server) {
		for (Consumer<MinecraftServer> task : EARLY_TASKS) {
			task.accept(server);
		}
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
			HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
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

	private enum Registrar implements TickRegistrar {
		INSTANCE;

		@Override public void start(Consumer<MinecraftServer> task) { onServerTickStart(task); }
		@Override public void early(Consumer<MinecraftServer> task) { onEarlyTick(task); }
		@Override public void global(Consumer<MinecraftServer> task) { onGlobalTick(task); }
		@Override public void level(BiConsumer<MinecraftServer, ServerLevel> task) { onLevelTick(task); }
		@Override public void player(PlayerTask task) { onPlayerTick(task); }
		@Override public void hero(ResourceLocation heroId, PlayerTask task) { onHeroTick(heroId, task); }
		@Override public void activeAbility(ResourceLocation abilityId, PlayerTask task) { onActiveAbilityTick(abilityId, task); }
	}
}
