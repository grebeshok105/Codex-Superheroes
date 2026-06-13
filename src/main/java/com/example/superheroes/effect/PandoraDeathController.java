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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pandora — scripted «she never dies» revival cut-scene (reworked V4).
 *
 * <p>The instant a hit would drop Pandora to {@code <= 0.5} HP the damage is cancelled and a short
 * cut-scene plays for the whole scene (no shaderpack, no flying, no POV swap, no lightning):
 * <ol>
 *   <li>Everyone freezes — camera + mouse/keyboard input are locked client-side.</li>
 *   <li>Her recorded voice line plays ({@code pandora.vanity_revive}).</li>
 *   <li>A title appears at the top of the screen: «Ты думал меня так легко убить?».</li>
 *   <li>After the line she reappears right behind the killer — <b>no</b> child giggle.</li>
 * </ol>
 *
 * <p>After this revival Pandora permanently loses her hitbox: she can no longer be damaged by any
 * means (the cut-scene therefore never re-triggers). The permanent state is cleared when she drops
 * the hero or disconnects.
 */
public final class PandoraDeathController {

	/** Cut-scene length, in ticks. Matches the ~4.8s revive voice clip. */
	private static final int DURATION_TICKS = 96;
	/** HP threshold that triggers the cut-scene instead of death. */
	private static final float TRIGGER_HP = 0.5f;

	private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();
	/** Pandoras that already revived — permanently un-hittable until they drop the hero / leave. */
	private static final Set<UUID> PERMA_INVULNERABLE = ConcurrentHashMap.newKeySet();

	private PandoraDeathController() {
	}

	private static final class Session {
		final UUID pandoraId;
		UUID killerId;
		int tick;
		final double anchorX;
		final double anchorY;
		final double anchorZ;
		final float anchorYaw;
		final float anchorPitch;

		Session(ServerPlayer pandora) {
			this.pandoraId = pandora.getUUID();
			this.anchorX = pandora.getX();
			this.anchorY = pandora.getY();
			this.anchorZ = pandora.getZ();
			this.anchorYaw = pandora.getYRot();
			this.anchorPitch = pandora.getXRot();
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
		// Already revived once → her hitbox is gone, nothing can touch her ever again.
		if (PERMA_INVULNERABLE.contains(pandora.getUUID())) {
			return false;
		}
		// Mid cut-scene → untouchable.
		if (ACTIVE.containsKey(pandora.getUUID())) {
			return false;
		}
		// Would this hit drop her to the death threshold? Then start the cut-scene instead.
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
		if (!PERMA_INVULNERABLE.contains(player.getUUID()) && !ACTIVE.containsKey(player.getUUID())) {
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

		// Keep her alive, pinned and untouchable for the whole sequence.
		pandora.setHealth(pandora.getMaxHealth());
		pandora.clearFire();
		pandora.removeAllEffects();
		pandora.setInvulnerable(true);
		pandora.setDeltaMovement(Vec3.ZERO);
		pandora.hurtMarked = true;

		ServerLevel level = pandora.serverLevel();
		// Her recorded line — played non-positionally to everyone so both principals hear it clearly.
		for (ServerPlayer viewer : level.players()) {
			viewer.playNotifySound(ModSounds.PANDORA_VANITY_REVIVE, SoundSource.HOSTILE, 1.0f, 1.0f);
		}

		broadcast(level, PandoraCinematicS2CPayload.PHASE_START, pandora, killer, session);
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
			tickCinematic(pandora, session);
		}
	}

	private static void tickCinematic(ServerPlayer pandora, Session session) {
		// Hold her health pinned and freeze her in place at the anchor — no rise, no drift.
		pandora.setHealth(pandora.getMaxHealth());
		pandora.setDeltaMovement(Vec3.ZERO);
		pandora.connection.teleport(session.anchorX, session.anchorY, session.anchorZ,
				session.anchorYaw, session.anchorPitch);
		pandora.fallDistance = 0f;
	}

	private static void endCinematic(MinecraftServer server, ServerPlayer pandora, Session session) {
		ServerLevel level = pandora.serverLevel();
		ServerPlayer killer = session.killerId == null ? null
				: server.getPlayerList().getPlayer(session.killerId);

		// Reappear right behind the killer, facing their back.
		Vec3 dest;
		float yaw = session.anchorYaw;
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

		pandora.setHealth(pandora.getMaxHealth());
		pandora.setDeltaMovement(Vec3.ZERO);
		pandora.fallDistance = 0f;
		pandora.connection.teleport(dest.x, dest.y, dest.z, yaw, 0f);

		// Permanent revival: her hitbox is gone — nothing can ever damage her again.
		PERMA_INVULNERABLE.add(pandora.getUUID());
		pandora.setInvulnerable(true);

		// NO child giggle on revival (cut per request).
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

	/** Re-pick of a hero clears any leftover session AND the permanent invulnerability. */
	public static void resetOnHeroTaken(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
		if (PERMA_INVULNERABLE.remove(player.getUUID())) {
			player.setInvulnerable(false);
		}
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
		PERMA_INVULNERABLE.remove(player.getUUID());
	}
}
