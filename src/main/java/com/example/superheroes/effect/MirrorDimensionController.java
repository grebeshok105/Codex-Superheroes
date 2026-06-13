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
 * Server-side state of Doctor Strange's Mirror Dimension: which caster warps
 * which victim, the active warp MODE/J, and the spatial "trap" that keeps the
 * victim inside a bounded zone around where they were caught.
 *
 * Sends ON/OFF/KEEPALIVE/SWITCH payloads to the victim's client and relays the
 * victim's status answers back to the caster.
 */
public final class MirrorDimensionController {
	private static final int KEEPALIVE_INTERVAL_TICKS = 20;

	/** Radius (blocks) of the zone the victim cannot escape while trapped. */
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

	private static final class Session {
		final UUID victim;
		int mode;
		int scale;
		final Vec3 center;
		boolean applied;
		int yankCooldown;

		Session(UUID victim, int mode, int scale, Vec3 center) {
			this.victim = victim;
			this.mode = mode;
			this.scale = scale;
			this.center = center;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MirrorDimensionController::tick);
	}

	/** @return false when the victim is already trapped by another Strange. */
	public static boolean start(ServerPlayer caster, ServerPlayer victim, int mode, int scale) {
		UUID existingCaster = VICTIM_TO_CASTER.get(victim.getUUID());
		if (existingCaster != null && !existingCaster.equals(caster.getUUID())) {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.busy").withStyle(ChatFormatting.RED), true);
			return false;
		}
		stop(caster, false);
		SESSIONS.put(caster.getUUID(), new Session(victim.getUUID(), mode, scale, victim.position()));
		VICTIM_TO_CASTER.put(victim.getUUID(), caster.getUUID());
		ServerPlayNetworking.send(victim, new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_ON, mode, scale));
		return true;
	}

	/**
	 * Mode-switch ability: advance the caster's active warp to the next MODE/J in
	 * the cycle and push it to the victim (SWITCH = re-apply without re-snapshot).
	 *
	 * @return false when the caster has no active Mirror Dimension session.
	 */
	public static boolean cycleMode(ServerPlayer caster) {
		Session session = SESSIONS.get(caster.getUUID());
		if (session == null) {
			return false;
		}
		int next = (indexOfMode(session.mode) + 1) % MODE_CYCLE.length;
		session.mode = MODE_CYCLE[next];
		session.scale = SCALE_CYCLE[next];
		ServerPlayer victim = caster.server.getPlayerList().getPlayer(session.victim);
		if (victim != null) {
			ServerPlayNetworking.send(victim,
					new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_SWITCH, session.mode, session.scale));
		}
		caster.displayClientMessage(
				Component.translatable("ability.superheroes.mirror_mode_cycle.switched", session.mode).withStyle(ChatFormatting.LIGHT_PURPLE), true);
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

	/** Stops the caster's active session (if any) and tells the victim to restore shaders. */
	public static void stop(ServerPlayer caster, boolean notifyCaster) {
		Session session = SESSIONS.remove(caster.getUUID());
		if (session == null) {
			return;
		}
		VICTIM_TO_CASTER.remove(session.victim);
		ServerPlayer victim = caster.server.getPlayerList().getPlayer(session.victim);
		if (victim != null) {
			ServerPlayNetworking.send(victim, new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
		}
		if (notifyCaster) {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.released").withStyle(ChatFormatting.GRAY), true);
		}
	}

	public static void handleStatus(ServerPlayer victim, int status) {
		UUID casterId = VICTIM_TO_CASTER.get(victim.getUUID());
		ServerPlayer caster = casterId == null ? null : victim.server.getPlayerList().getPlayer(casterId);
		if (status == MirrorDimensionStatusC2SPayload.OK_APPLIED && casterId != null) {
			Session session = SESSIONS.get(casterId);
			if (session != null) {
				session.applied = true;
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
			// actionbar (true), не чат — по запросу убрать спам Зеркального измерения из чата.
			caster.displayClientMessage(
					Component.translatable(key, victim.getName()).withStyle(color), true);
		}
		boolean failed = status == MirrorDimensionStatusC2SPayload.NO_IRIS
				|| status == MirrorDimensionStatusC2SPayload.NO_PACK
				|| status == MirrorDimensionStatusC2SPayload.IRIS_API_FAIL;
		if (failed && caster != null) {
			// Don't burn the caster's mana on an effect the victim can't render.
			AbilityRouter.deactivate(caster, AbilityIds.MIRROR_DIMENSION);
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
			ServerPlayer victim = server.getPlayerList().getPlayer(session.victim);
			if (!sessionValid(caster, victim)) {
				it.remove();
				VICTIM_TO_CASTER.remove(session.victim);
				if (victim != null) {
					ServerPlayNetworking.send(victim,
							new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
				}
				if (caster != null) {
					AbilityRouter.deactivate(caster, AbilityIds.MIRROR_DIMENSION);
				}
				continue;
			}
			if (session.applied) {
				enforceZone(victim, session);
			}
			if (keepalive) {
				ServerPlayNetworking.send(victim,
						new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_KEEPALIVE, 0, 0));
			}
		}
	}

	/**
	 * Spatial trap: the victim cannot leave a {@link #ZONE_RADIUS}-block sphere
	 * around where they were caught. Crossing the border spins them around (yaw
	 * +180) and yanks them back inside so they get lost in the dimension.
	 */
	private static void enforceZone(ServerPlayer victim, Session session) {
		if (session.yankCooldown > 0) {
			session.yankCooldown--;
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
		session.yankCooldown = YANK_COOLDOWN_TICKS;
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

	private static float Mth360(float yaw) {
		yaw %= 360f;
		if (yaw < -180f) {
			yaw += 360f;
		} else if (yaw > 180f) {
			yaw -= 360f;
		}
		return yaw;
	}

	private static boolean sessionValid(ServerPlayer caster, ServerPlayer victim) {
		if (caster == null || caster.isRemoved() || caster.isDeadOrDying()) {
			return false;
		}
		if (victim == null || victim.isRemoved() || victim.isDeadOrDying() || victim.isSpectator()) {
			return false;
		}
		HeroData data = caster.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !PandoraHero.ID.equals(data.heroId())) {
			return false;
		}
		return data.isActive(AbilityIds.MIRROR_DIMENSION);
	}
}
