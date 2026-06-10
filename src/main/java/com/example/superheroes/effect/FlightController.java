package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.flight.FlightAbilityState;
import com.example.superheroes.flight.FlightMode;
import com.example.superheroes.flight.FlightPhase;
import com.example.superheroes.flight.FlightPhaseResolver;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class FlightController {
	private static final int URANIUM_AUTO_OFF_TICKS = 100;
	private static final int URANIUM_COOLDOWN_TICKS = 20;
	private static final int SYNC_INTERVAL_TICKS = 5;
	private static final float IRON_MAN_ENERGY_FLOOR = 100f;

	private static final Map<UUID, State> STATES = new HashMap<>();
	private static final Map<UUID, Long> URANIUM_ACTIVE_SINCE = new HashMap<>();
	private static final Map<UUID, Long> URANIUM_COOLDOWN_UNTIL = new HashMap<>();

	private FlightController() {
	}

	private static final class State {
		FlightMode mode;
		FlightPhase phase = FlightPhase.IDLE;
		FlightMode lastSyncedMode;
		FlightPhase lastSyncedPhase = FlightPhase.IDLE;
		long startedAt;
		long lastSyncAt;
		float lastSyncSpeed;

		State(FlightMode mode, long now) {
			this.mode = mode;
			this.startedAt = now;
			this.lastSyncAt = Long.MIN_VALUE;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
			cleanup(server);
		});
	}

	public static boolean start(ServerPlayer player, FlightMode mode) {
		long now = player.level().getGameTime();
		State state = STATES.computeIfAbsent(player.getUUID(), id -> new State(mode, now));
		state.phase = FlightPhase.TAKEOFF;
		state.startedAt = now;
		if (state.mode != mode) {
			state.mode = mode;
		}
		enableVanillaFlight(player);
		player.startFallFlying();
		player.fallDistance = 0f;
		sync(player, state, 0f, true, true);
		return true;
	}

	public static void stop(ServerPlayer player) {
		UUID id = player.getUUID();
		State state = STATES.remove(id);
		URANIUM_ACTIVE_SINCE.remove(id);
		disableVanillaFlight(player);
		if (state != null) {
			ModNetworking.syncFlightState(player, state.mode, FlightPhase.IDLE, 0f, false);
		} else {
			ModNetworking.syncFlightState(player, FlightMode.NORMAL, FlightPhase.IDLE, 0f, false);
		}
	}

	public static void stop(ServerPlayer player, ResourceLocation deactivatedAbility) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		FlightMode remaining = FlightAbilityState.activeModeExcept(data, deactivatedAbility);
		if (remaining == null) {
			stop(player);
			return;
		}
		long now = player.level().getGameTime();
		State state = STATES.computeIfAbsent(player.getUUID(), id -> new State(remaining, now));
		state.mode = remaining;
		enableVanillaFlight(player);
		if (!player.isFallFlying()) {
			player.startFallFlying();
		}
		sync(player, state, 0f, true, true);
	}

	public static void clear(UUID id) {
		STATES.remove(id);
		URANIUM_ACTIVE_SINCE.remove(id);
		URANIUM_COOLDOWN_UNTIL.remove(id);
	}

	public static boolean isOnCooldown(ServerPlayer player) {
		Long until = URANIUM_COOLDOWN_UNTIL.get(player.getUUID());
		if (until == null) return false;
		if (player.level().getGameTime() >= until) {
			URANIUM_COOLDOWN_UNTIL.remove(player.getUUID());
			return false;
		}
		return true;
	}

	public static boolean isFlightAbility(ResourceLocation abilityId) {
		return FlightAbilityState.isFlightAbility(abilityId);
	}

	public static boolean isFlightActive(HeroData data) {
		return FlightAbilityState.isActive(data);
	}

	public static FlightMode activeMode(HeroData data) {
		return FlightAbilityState.activeMode(data);
	}

	private static void tickPlayer(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		FlightMode mode = data.hasHero() ? activeMode(data) : null;
		if (mode == null) {
			clearIfPresent(player);
			return;
		}
		if (handleHomelanderUraniumLimit(player, data, mode)) {
			return;
		}

		long now = player.level().getGameTime();
		State state = STATES.computeIfAbsent(player.getUUID(), id -> new State(mode, now));
		if (state.mode != mode) {
			state.mode = mode;
			state.startedAt = now;
			state.phase = FlightPhase.TAKEOFF;
		}

		enableVanillaFlight(player);
		if (!player.isFallFlying()) {
			player.startFallFlying();
		}
		player.fallDistance = 0f;

		Vec3 motion = player.getDeltaMovement();
		double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		boolean onGround = player.onGround();
		int activeTicks = (int) Math.max(0L, now - state.startedAt);
		boolean moving = horizontalSpeed > 0.08 || Math.abs(motion.y) > 0.06;
		FlightPhase phase = FlightPhaseResolver.resolve(mode, activeTicks, onGround, horizontalSpeed, motion.y, moving);
		state.phase = phase;

		if (mode == FlightMode.IRON_MAN) {
			tickIronManEffects(player, data);
		}
		if (mode == FlightMode.SUPERSONIC) {
			tickSupersonicEffects(player);
		}
		sync(player, state, (float) horizontalSpeed, true, false);
	}

	private static boolean handleHomelanderUraniumLimit(ServerPlayer player, HeroData data, FlightMode mode) {
		boolean isHomelander = HomelanderHero.ID.equals(data.heroId());
		UUID id = player.getUUID();
		if (!isHomelander || mode != FlightMode.NORMAL || ModEffects.isMadness(player)
				|| !UraniumDefenseController.isUnderUraniumThreat(player)) {
			URANIUM_ACTIVE_SINCE.remove(id);
			return false;
		}
		if (player.onGround() || player.isInWater()) {
			forceUraniumOff(player);
			return true;
		}
		long now = player.level().getGameTime();
		Long since = URANIUM_ACTIVE_SINCE.get(id);
		if (since == null) {
			URANIUM_ACTIVE_SINCE.put(id, now);
			return false;
		}
		if (now - since >= URANIUM_AUTO_OFF_TICKS) {
			forceUraniumOff(player);
			return true;
		}
		return false;
	}

	private static void forceUraniumOff(ServerPlayer player) {
		UUID id = player.getUUID();
		URANIUM_ACTIVE_SINCE.remove(id);
		URANIUM_COOLDOWN_UNTIL.put(id, player.level().getGameTime() + URANIUM_COOLDOWN_TICKS);
		AbilityRouter.deactivate(player, AbilityIds.FLIGHT);
	}

	private static void tickIronManEffects(ServerPlayer player, HeroData data) {
		if (data.energy() < IRON_MAN_ENERGY_FLOOR) {
			HeroData updated = data.withResources(IRON_MAN_ENERGY_FLOOR, data.mana());
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncResources(player, updated);
		}
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();
		Vec3 look = player.getLookAngle();
		Vec3 trail = pos.add(look.reverse().scale(0.4)).add(0, 0.2, 0);
		if (player.tickCount % 2 == 0) {
			level.sendParticles(ModParticles.TRANSFORM_SPARK,
					trail.x, trail.y, trail.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				pos.x, pos.y - 0.2, pos.z, 1, 0.1, 0.0, 0.1, 0.005);
	}

	private static void tickSupersonicEffects(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();
		Vec3 dir = player.getLookAngle();
		Vec3 back = pos.add(dir.reverse().scale(0.5));
		level.sendParticles(ParticleTypes.CLOUD,
				back.x, back.y + 0.3, back.z, 4, 0.15, 0.15, 0.15, 0.02);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				back.x, back.y + 0.3, back.z, 2, 0.15, 0.15, 0.15, 0.02);
		level.sendParticles(ModParticles.LASER_SPARK,
				back.x, back.y + 0.3, back.z, 3, 0.1, 0.1, 0.1, 0.0);
		if (player.tickCount % 6 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.2f, 1.6f);
		}
	}

	private static void enableVanillaFlight(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		if (!abilities.mayfly || !abilities.flying) {
			abilities.mayfly = true;
			abilities.flying = true;
			player.onUpdateAbilities();
		}
	}

	private static void disableVanillaFlight(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		abilities.flying = false;
		if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
			abilities.mayfly = false;
		}
		player.onUpdateAbilities();
		player.stopFallFlying();
	}

	private static void clearIfPresent(ServerPlayer player) {
		State state = STATES.remove(player.getUUID());
		URANIUM_ACTIVE_SINCE.remove(player.getUUID());
		if (state != null) {
			disableVanillaFlight(player);
			ModNetworking.syncFlightState(player, state.mode, FlightPhase.IDLE, 0f, false);
		}
	}

	private static void sync(ServerPlayer player, State state, float horizontalSpeed, boolean active, boolean force) {
		long now = player.level().getGameTime();
		boolean phaseChanged = state.phase != state.lastSyncedPhase || state.mode != state.lastSyncedMode;
		boolean speedChanged = Math.abs(horizontalSpeed - state.lastSyncSpeed) > 0.08f;
		if (force || phaseChanged || speedChanged || now - state.lastSyncAt >= SYNC_INTERVAL_TICKS) {
			ModNetworking.syncFlightState(player, state.mode, state.phase, horizontalSpeed, active);
			state.lastSyncAt = now;
			state.lastSyncSpeed = horizontalSpeed;
			state.lastSyncedMode = state.mode;
			state.lastSyncedPhase = state.phase;
		}
	}

	private static void cleanup(net.minecraft.server.MinecraftServer server) {
		Iterator<Map.Entry<UUID, Long>> it = URANIUM_COOLDOWN_UNTIL.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Long> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || p.level().getGameTime() >= e.getValue()) {
				it.remove();
			}
		}
		STATES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		URANIUM_ACTIVE_SINCE.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
	}
}
