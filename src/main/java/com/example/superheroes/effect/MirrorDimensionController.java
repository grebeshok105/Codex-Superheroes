package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.network.MirrorDimensionS2CPayload;
import com.example.superheroes.network.MirrorDimensionStatusC2SPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side state of Pandora's «Дом тщеславия» (House of Vanity). Unlike the
 * old single-target Mirror Dimension, the House is a <b>zone</b> centred on
 * Pandora at the moment she casts: <b>every</b> other player within
 * {@link #PULL_RADIUS} blocks is dragged inside, sees the Acid "round world"
 * warp on their own client, and is trapped within a {@link #ZONE_RADIUS} sphere
 * until Pandora drops the House. New players who wander into the radius while
 * the House is open are absorbed too.
 *
 * <p>Pandora herself is the caster — she is "inside" her own House (so her
 * dimension-only abilities unlock, see {@link #hasActiveHouse}) but does NOT get
 * the warp shader applied to her screen.
 *
 * <p>Sends ON/OFF/KEEPALIVE/SWITCH payloads to each victim's client and relays
 * their status answers back to Pandora.
 */
public final class MirrorDimensionController {
	private static final int KEEPALIVE_INTERVAL_TICKS = 20;

	/** Radius (blocks) around Pandora from which players are pulled into the House. */
	private static final double PULL_RADIUS = 50.0;
	/** Radius (blocks) of the zone a victim cannot escape while trapped. */
	private static final double ZONE_RADIUS = 50.0;
	/** When yanked back, the victim lands this fraction of the radius from the center. */
	private static final double PULL_BACK_FACTOR = 0.4;
	/** Ticks between forced yank-backs so the victim isn't teleport-spammed every tick. */
	private static final int YANK_COOLDOWN_TICKS = 8;

	/** Mode cycle for the mode-switch ability and its matching J (sphere scale). */
	private static final int[] MODE_CYCLE = {4, 5, 6, 9};
	private static final int[] SCALE_CYCLE = {16, 8, 32, 256};

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, UUID> VICTIM_TO_CASTER = new HashMap<>();
	private static int clock;

	private MirrorDimensionController() {
	}

	/** Per-victim trap state inside a House session. */
	private static final class VictimState {
		boolean applied;
		int yankCooldown;
	}

	/** One open House: the caster (Pandora), where it is anchored, and its victims. */
	private static final class Session {
		final Vec3 center;
		int mode;
		int scale;
		final Map<UUID, VictimState> victims = new HashMap<>();

		Session(int mode, int scale, Vec3 center) {
			this.mode = mode;
			this.scale = scale;
			this.center = center;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MirrorDimensionController::tick);
	}

	/**
	 * Opens (or refreshes) Pandora's House centred on her current position and
	 * drags in every eligible player within {@link #PULL_RADIUS}.
	 *
	 * @return always {@code true} — the House opens even if nobody is nearby yet
	 *         (latecomers are absorbed on the fly). Reports a summary to Pandora.
	 */
	public static boolean start(ServerPlayer caster, int mode, int scale) {
		stop(caster, false);
		Session session = new Session(mode, scale, caster.position());
		SESSIONS.put(caster.getUUID(), session);
		int pulled = absorbNearby(caster, session);
		if (pulled == 0) {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.empty")
							.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		} else {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.pulled", pulled)
							.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		}
		return true;
	}

	/**
	 * Pulls every eligible player within {@link #PULL_RADIUS} of the House center
	 * into the given session. Skips the caster, spectators, the dead, players
	 * already trapped by another House, and players whose client lacks the mod.
	 *
	 * @return how many <i>new</i> victims were absorbed by this call.
	 */
	private static int absorbNearby(ServerPlayer caster, Session session) {
		int pulled = 0;
		double r2 = PULL_RADIUS * PULL_RADIUS;
		for (ServerPlayer p : caster.serverLevel().players()) {
			if (p == caster || p.isSpectator() || p.isDeadOrDying()) {
				continue;
			}
			if (p.distanceToSqr(session.center.x, session.center.y, session.center.z) > r2) {
				continue;
			}
			UUID id = p.getUUID();
			if (VICTIM_TO_CASTER.containsKey(id)) {
				continue; // already trapped (by this or another House)
			}
			if (!ServerPlayNetworking.canSend(p, MirrorDimensionS2CPayload.TYPE)) {
				continue; // no Codex-Superheroes mod on their client — can't render
			}
			session.victims.put(id, new VictimState());
			VICTIM_TO_CASTER.put(id, caster.getUUID());
			ServerPlayNetworking.send(p,
					new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_ON, session.mode, session.scale));
			spawnCastParticles(p);
			pulled++;
		}
		return pulled;
	}

	/**
	 * Mode-switch ability: advance the House's warp to the next MODE/J in the
	 * cycle and push it to <b>all</b> trapped victims (SWITCH = re-apply without
	 * re-snapshotting their original shader state).
	 *
	 * @return false when Pandora has no open House.
	 */
	public static boolean cycleMode(ServerPlayer caster) {
		Session session = SESSIONS.get(caster.getUUID());
		if (session == null) {
			return false;
		}
		int next = (indexOfMode(session.mode) + 1) % MODE_CYCLE.length;
		session.mode = MODE_CYCLE[next];
		session.scale = SCALE_CYCLE[next];
		for (UUID victimId : session.victims.keySet()) {
			ServerPlayer victim = caster.server.getPlayerList().getPlayer(victimId);
			if (victim != null) {
				ServerPlayNetworking.send(victim,
						new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_SWITCH, session.mode, session.scale));
			}
		}
		caster.displayClientMessage(
				Component.translatable("ability.superheroes.mirror_mode_cycle.switched", session.mode)
						.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		return true;
	}

	private static int indexOfMode(int mode) {
		for (int i = 0; i < MODE_CYCLE.length; i++) {
			if (MODE_CYCLE[i] == mode) {
				return i;
			}
		}
		return 0;
	}

	/** Closes Pandora's House (if any) and tells every victim to restore shaders. */
	public static void stop(ServerPlayer caster, boolean notifyCaster) {
		Session session = SESSIONS.remove(caster.getUUID());
		if (session == null) {
			return;
		}
		SpatialBindController.releaseAllOf(caster.getUUID());
		VanityAuthority.clear(caster);
		for (UUID victimId : session.victims.keySet()) {
			VICTIM_TO_CASTER.remove(victimId);
			ServerPlayer victim = caster.server.getPlayerList().getPlayer(victimId);
			if (victim != null) {
				ServerPlayNetworking.send(victim, new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
			}
		}
		if (notifyCaster) {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.released").withStyle(ChatFormatting.GRAY), true);
		}
	}

	/** @return true if this player is a Pandora with an open House (used to gate dimension-only abilities). */
	public static boolean hasActiveHouse(ServerPlayer caster) {
		return SESSIONS.containsKey(caster.getUUID());
	}

	/** @return the live ServerPlayer victims currently trapped in this caster's House (online only). */
	public static java.util.List<ServerPlayer> trappedVictims(ServerPlayer caster) {
		Session session = SESSIONS.get(caster.getUUID());
		if (session == null) {
			return java.util.List.of();
		}
		java.util.List<ServerPlayer> out = new java.util.ArrayList<>();
		for (UUID id : session.victims.keySet()) {
			ServerPlayer p = caster.server.getPlayerList().getPlayer(id);
			if (p != null && !p.isRemoved() && !p.isDeadOrDying()) {
				out.add(p);
			}
		}
		return out;
	}

	/** @return true if this player is currently trapped inside someone's House. */
	public static boolean isTrapped(ServerPlayer victim) {
		return VICTIM_TO_CASTER.containsKey(victim.getUUID());
	}

	public static void handleStatus(ServerPlayer victim, int status) {
		UUID casterId = VICTIM_TO_CASTER.get(victim.getUUID());
		ServerPlayer caster = casterId == null ? null : victim.server.getPlayerList().getPlayer(casterId);
		if (status == MirrorDimensionStatusC2SPayload.OK_APPLIED && casterId != null) {
			Session session = SESSIONS.get(casterId);
			if (session != null) {
				VictimState vs = session.victims.get(victim.getUUID());
				if (vs != null) {
					vs.applied = true;
				}
			}
		}
		String key = switch (status) {
			case MirrorDimensionStatusC2SPayload.OK_APPLIED -> "ability.superheroes.mirror_dimension.applied";
			case MirrorDimensionStatusC2SPayload.NO_IRIS -> "ability.superheroes.mirror_dimension.no_iris";
			case MirrorDimensionStatusC2SPayload.NO_PACK -> "ability.superheroes.mirror_dimension.no_pack";
			case MirrorDimensionStatusC2SPayload.IRIS_API_FAIL -> "ability.superheroes.mirror_dimension.iris_fail";
			default -> null;
		};
		if (caster != null && key != null) {
			ChatFormatting color = status == MirrorDimensionStatusC2SPayload.OK_APPLIED
					? ChatFormatting.GREEN : ChatFormatting.RED;
			// actionbar (true), не чат — по запросу убрать спам Дома тщеславия из чата.
			caster.displayClientMessage(
					Component.translatable(key, victim.getName()).withStyle(color), true);
		}
		boolean failed = status == MirrorDimensionStatusC2SPayload.NO_IRIS
				|| status == MirrorDimensionStatusC2SPayload.NO_PACK
				|| status == MirrorDimensionStatusC2SPayload.IRIS_API_FAIL;
		if (failed && casterId != null) {
			// This one victim can't render the warp — drop just them, keep the House
			// open for everyone else (and for latecomers).
			Session session = SESSIONS.get(casterId);
			if (session != null) {
				session.victims.remove(victim.getUUID());
			}
			VICTIM_TO_CASTER.remove(victim.getUUID());
		}
	}

	private static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) {
			clock = 0;
			return;
		}
		clock++;
		boolean keepalive = clock % KEEPALIVE_INTERVAL_TICKS == 0;
		Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Session> entry = it.next();
			Session session = entry.getValue();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
			if (!casterValid(caster)) {
				releaseAll(server, session);
				SpatialBindController.releaseAllOf(entry.getKey());
				it.remove();
				if (caster != null) {
					VanityAuthority.clear(caster);
					AbilityRouter.deactivate(caster, AbilityIds.MIRROR_DIMENSION);
				}
				continue;
			}
			// Pandora's Vanity Authority: flight + immunity while the House is open.
			VanityAuthority.applyToCaster(caster);
			// Continuously drag in any new players who entered the radius.
			if (keepalive) {
				absorbNearby(caster, session);
			}
			tickVictims(server, session, keepalive);
		}
	}

	/** Per-session victim upkeep: drop the gone/dead, enforce the trap, keepalive. */
	private static void tickVictims(MinecraftServer server, Session session, boolean keepalive) {
		Iterator<Map.Entry<UUID, VictimState>> vit = session.victims.entrySet().iterator();
		while (vit.hasNext()) {
			Map.Entry<UUID, VictimState> ve = vit.next();
			ServerPlayer victim = server.getPlayerList().getPlayer(ve.getKey());
			if (victim == null || victim.isRemoved() || victim.isDeadOrDying() || victim.isSpectator()) {
				if (victim != null) {
					ServerPlayNetworking.send(victim,
							new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
				}
				VICTIM_TO_CASTER.remove(ve.getKey());
				vit.remove();
				continue;
			}
			if (ve.getValue().applied) {
				enforceZone(victim, session, ve.getValue());
			}
			if (keepalive) {
				VanityAuthority.applyToVictim(victim);
				ServerPlayNetworking.send(victim,
						new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_KEEPALIVE, 0, 0));
			}
		}
	}

	private static void releaseAll(MinecraftServer server, Session session) {
		for (UUID victimId : session.victims.keySet()) {
			VICTIM_TO_CASTER.remove(victimId);
			ServerPlayer victim = server.getPlayerList().getPlayer(victimId);
			if (victim != null) {
				ServerPlayNetworking.send(victim,
						new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
			}
		}
	}

	/**
	 * Spatial trap: the victim cannot leave a {@link #ZONE_RADIUS}-block sphere
	 * around the House center. Crossing the border spins them around (yaw +180)
	 * and yanks them back inside so they get lost in the dimension.
	 */
	private static void enforceZone(ServerPlayer victim, Session session, VictimState state) {
		if (state.yankCooldown > 0) {
			state.yankCooldown--;
			return;
		}
		Vec3 c = session.center;
		Vec3 p = victim.position();
		double dx = p.x - c.x;
		double dz = p.z - c.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);
		double dy = p.y - c.y;
		boolean outHoriz = horiz > ZONE_RADIUS;
		boolean outVert = Math.abs(dy) > ZONE_RADIUS;
		if (!outHoriz && !outVert) {
			return;
		}

		double nx = p.x;
		double nz = p.z;
		if (outHoriz) {
			double f = (ZONE_RADIUS * PULL_BACK_FACTOR) / horiz;
			nx = c.x + dx * f;
			nz = c.z + dz * f;
		}
		float yaw = Mth360(victim.getYRot() + 180f);

		ServerLevel level = victim.serverLevel();
		// Always drop the victim onto a safe surface so the yank never teleports
		// them inside blocks (requested fix). Fall back to the current/center Y if
		// the heightmap result would still collide.
		double ny = safeSurfaceY(level, victim, nx, nz, outVert ? c.y : p.y);
		victim.teleportTo(level, nx, ny, nz, Set.of(), yaw, victim.getXRot());
		victim.setDeltaMovement(Vec3.ZERO);
		victim.hurtMarked = true;
		level.sendParticles(ParticleTypes.PORTAL, nx, ny + 1.0, nz, 60, 0.6, 1.0, 0.6, 0.4);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, nx, ny + 1.0, nz, 30, 0.4, 0.8, 0.4, 0.05);
		state.yankCooldown = YANK_COOLDOWN_TICKS;
	}

	/**
	 * Finds a safe Y to place the victim at the given XZ: the top surface from the
	 * motion-blocking (no-leaves) heightmap, then nudged up until the player's
	 * hitbox no longer collides (cap +6). Falls back to {@code fallbackY} if the
	 * surface itself is unusable.
	 */
	private static double safeSurfaceY(ServerLevel level, ServerPlayer victim, double x, double z, double fallbackY) {
		net.minecraft.core.BlockPos.MutableBlockPos cursor =
				net.minecraft.core.BlockPos.containing(x, fallbackY, z).mutable();
		int surfaceY = level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				cursor.getX(), cursor.getZ());
		double baseY = surfaceY;
		net.minecraft.world.entity.EntityDimensions dims = victim.getDimensions(victim.getPose());
		for (int up = 0; up <= 6; up++) {
			net.minecraft.world.phys.AABB box = dims.makeBoundingBox(x, baseY + up, z);
			if (level.noCollision(victim, box)) {
				return baseY + up;
			}
		}
		return Math.max(baseY, fallbackY);
	}

	private static void spawnCastParticles(ServerPlayer victim) {
		ServerLevel level = victim.serverLevel();
		level.sendParticles(ParticleTypes.REVERSE_PORTAL,
				victim.getX(), victim.getY() + 1.0, victim.getZ(),
				80, 0.8, 1.0, 0.8, 0.05);
	}

	private static float Mth360(float yaw) {
		yaw %= 360f;
		if (yaw < -180f) {
			yaw += 360f;
		} else if (yaw > 180f) {
			yaw -= 360f;
		}
		return yaw;
	}

	private static boolean casterValid(ServerPlayer caster) {
		if (caster == null || caster.isRemoved() || caster.isDeadOrDying()) {
			return false;
		}
		HeroData data = caster.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !PandoraHero.ID.equals(data.heroId())) {
			return false;
		}
		return data.isActive(AbilityIds.MIRROR_DIMENSION);
	}
}
