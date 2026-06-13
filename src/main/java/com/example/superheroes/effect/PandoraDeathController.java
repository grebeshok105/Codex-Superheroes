package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.network.PandoraCinematicS2CPayload;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pandora — scripted "she never dies" death cinematic (#6–#10).
 *
 * <p>Pandora can NOT die. The instant a hit would drop her to {@code <= 0.5} HP the damage is
 * cancelled and a 5-second cinematic plays for the whole scene:
 * <ol>
 *   <li>Everyone freezes — camera + mouse/keyboard input are locked client-side (#7, #10).</li>
 *   <li>The world is repainted through a dedicated Iris shaderpack: every block/entity becomes
 *       an absolutely black silhouette, the sky a red→white gradient (#6).</li>
 *   <li>Both principals get forced-F1 (all HUD gone) and Pandora's POV is swapped to her killer's
 *       eyes via {@link ServerPlayer#setCamera} (#8).</li>
 *   <li>Pandora rises 2 blocks; soundless lightning (rendered black) strikes around her (#9).</li>
 *   <li>After 5s it abruptly ends: Pandora reappears right behind the killer, giggles, full heal,
 *       everything restores (#10).</li>
 * </ol>
 *
 * <p>The visual layer is entirely client-side (shaderpack swap + render); the server only
 * orchestrates the timeline and broadcasts START/END.
 */
public final class PandoraDeathController {

	/** Cinematic length: 5 seconds. */
	private static final int DURATION_TICKS = 100;
	/** HP threshold that triggers the cinematic instead of death. */
	private static final float TRIGGER_HP = 0.5f;
	/** How high Pandora floats, in blocks. */
	private static final double RISE_BLOCKS = 2.0;

	private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

	private PandoraDeathController() {
	}

	private static final class Session {
		final UUID pandoraId;
		UUID killerId;
		int tick;
		double anchorX;
		double anchorY;
		double anchorZ;

		Session(ServerPlayer pandora) {
			this.pandoraId = pandora.getUUID();
			this.anchorX = pandora.getX();
			this.anchorY = pandora.getY();
			this.anchorZ = pandora.getZ();
		}
	}

	private static boolean isPandora(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) {
			return false;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && PandoraHero.ID.equals(data.heroId());
	}

	public static boolean isInCinematic(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	/**
	 * Hook for {@code ServerLivingEntityEvents.ALLOW_DAMAGE}.
	 *
	 * @return {@code true} to let the damage through, {@code false} to cancel it.
	 */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!isPandora(entity)) {
			return true;
		}
		ServerPlayer pandora = (ServerPlayer) entity;
		// Already mid-cinematic → she is untouchable.
		if (ACTIVE.containsKey(pandora.getUUID())) {
			return false;
		}
		// Would this hit drop her to the death threshold? Then start the cinematic instead.
		if (pandora.getHealth() - amount <= TRIGGER_HP) {
			startCinematic(pandora, source);
			return false;
		}
		return true;
	}

	/**
	 * Hook for {@code ServerLivingEntityEvents.ALLOW_DEATH}. Pandora simply never dies — this is a
	 * hard backstop in case some damage path bypasses {@link #allowDamage}.
	 */
	public static boolean allowDeath(ServerPlayer player, DamageSource source) {
		if (!isPandora(player)) {
			return true;
		}
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.removeAllEffects();
		if (!ACTIVE.containsKey(player.getUUID())) {
			startCinematic(player, source);
		}
		return false;
	}

	private static void startCinematic(ServerPlayer pandora, DamageSource source) {
		Session session = new Session(pandora);
		ServerPlayer killer = resolveKiller(source, pandora);
		if (killer != null && killer.serverLevel() == pandora.serverLevel()) {
			session.killerId = killer.getUUID();
		}
		ACTIVE.put(pandora.getUUID(), session);

		// Keep her alive and untouchable for the whole sequence.
		pandora.setHealth(pandora.getMaxHealth());
		pandora.clearFire();
		pandora.removeAllEffects();
		pandora.setInvulnerable(true);
		pandora.setNoGravity(true);
		pandora.setDeltaMovement(Vec3.ZERO);
		pandora.hurtMarked = true;

		// POV swap: Pandora watches through the killer's eyes (#8).
		if (killer != null) {
			pandora.setCamera(killer);
		}

		broadcast(pandora.serverLevel(), PandoraCinematicS2CPayload.PHASE_START, pandora, killer, session);
	}

	/** Driven from the server END tick. */
	public static void serverTick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Session>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Session session = it.next().getValue();
			ServerPlayer pandora = server.getPlayerList().getPlayer(session.pandoraId);
			if (pandora == null) {
				it.remove();
				continue;
			}
			session.tick++;
			if (session.tick >= DURATION_TICKS) {
				endCinematic(server, pandora, session);
				it.remove();
				continue;
			}
			tickCinematic(server, pandora, session);
		}
	}

	private static void tickCinematic(MinecraftServer server, ServerPlayer pandora, Session session) {
		ServerLevel level = pandora.serverLevel();

		// Hold her health pinned and keep her floating to the target rise height.
		pandora.setHealth(pandora.getMaxHealth());
		double targetY = session.anchorY + RISE_BLOCKS;
		double newY = pandora.getY() + (targetY - pandora.getY()) * 0.20;
		pandora.setDeltaMovement(Vec3.ZERO);
		pandora.connection.teleport(session.anchorX, newY, session.anchorZ, pandora.getYRot(), pandora.getXRot());
		pandora.fallDistance = 0f;

		// Re-assert the POV swap (a respawn/dimension quirk could drop it).
		ServerPlayer killer = session.killerId == null ? null
				: server.getPlayerList().getPlayer(session.killerId);
		if (killer != null && pandora.getCamera() != killer) {
			pandora.setCamera(killer);
		}

		// Soundless lightning (#9): visual-only bolts struck around her; the client mutes
		// their thunder during the cinematic and the shaderpack renders them pure black.
		if (session.tick % 11 == 3) {
			spawnSilentBolt(level, session);
		}
	}

	private static void spawnSilentBolt(ServerLevel level, Session session) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt == null) {
			return;
		}
		double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
		double dist = 1.5 + level.getRandom().nextDouble() * 2.5;
		double bx = session.anchorX + Math.cos(angle) * dist;
		double bz = session.anchorZ + Math.sin(angle) * dist;
		bolt.moveTo(bx, session.anchorY, bz);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
	}

	private static void endCinematic(MinecraftServer server, ServerPlayer pandora, Session session) {
		ServerLevel level = pandora.serverLevel();
		ServerPlayer killer = session.killerId == null ? null
				: server.getPlayerList().getPlayer(session.killerId);

		// Reset POV back to herself.
		pandora.setCamera(pandora);

		// Reappear right behind the killer, facing their back (#10).
		Vec3 dest;
		float yaw = pandora.getYRot();
		if (killer != null && killer.serverLevel() == level) {
			float yawRad = (float) Math.toRadians(killer.getYRot());
			dest = new Vec3(
					killer.getX() + Math.sin(yawRad) * 1.4,
					killer.getY(),
					killer.getZ() - Math.cos(yawRad) * 1.4);
			yaw = killer.getYRot();
		} else {
			dest = new Vec3(session.anchorX, session.anchorY, session.anchorZ);
		}

		pandora.setNoGravity(false);
		pandora.setInvulnerable(false);
		pandora.setHealth(pandora.getMaxHealth());
		pandora.setDeltaMovement(Vec3.ZERO);
		pandora.fallDistance = 0f;
		pandora.connection.teleport(dest.x, dest.y, dest.z, yaw, 0f);

		// Child's giggle — this one DOES play (only the lightning is silent).
		level.playSound(null, dest.x, dest.y, dest.z,
				ModSounds.PANDORA_CHILD_GIGGLE, SoundSource.HOSTILE, 1.0f, 1.0f);

		broadcast(level, PandoraCinematicS2CPayload.PHASE_END, pandora, killer, session);
	}

	private static void broadcast(ServerLevel level, int phase, ServerPlayer pandora,
			ServerPlayer killer, Session session) {
		int killerId = killer != null ? killer.getId() : -1;
		PandoraCinematicS2CPayload payload = new PandoraCinematicS2CPayload(
				phase, pandora.getId(), killerId,
				session.anchorX, session.anchorY, session.anchorZ);
		for (ServerPlayer viewer : level.players()) {
			if (ServerPlayNetworking.canSend(viewer, PandoraCinematicS2CPayload.TYPE)) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
	}

	private static ServerPlayer resolveKiller(DamageSource source, ServerPlayer pandora) {
		Entity attacker = source.getEntity();
		if (attacker instanceof ServerPlayer sp && sp != pandora) {
			return sp;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof ServerPlayer sp && sp != pandora) {
			return sp;
		}
		return null;
	}

	/** Re-pick of a hero clears any leftover session (e.g. de-transform mid-cinematic). */
	public static void resetOnHeroTaken(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}
}
