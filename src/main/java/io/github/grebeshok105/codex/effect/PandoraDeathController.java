package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.PandoraHero;
import io.github.grebeshok105.codex.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.network.PandoraCinematicS2CPayload;
import io.github.grebeshok105.codex.sound.ModSounds;
import io.github.grebeshok105.codex.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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
 * means (the cut-scene therefore never re-triggers). The revived state lives in the persistent
 * {@link ModAttachments#PANDORA_REVIVED} attachment and the {@code Invulnerable} entity flag is
 * held through {@link EntityControlLock} — both survive a relog coherently (audit B4: the old
 * in-memory set forgot the flag while the NBT-written {@code Invulnerable} byte persisted, and
 * in the other direction the revived protection silently vanished on relog).
 */
public final class PandoraDeathController {

	/** Cut-scene length, in ticks. Matches the ~4.8s revive voice clip. */
	private static final int DURATION_TICKS = 96;
	/** HP threshold that triggers the cut-scene instead of death. */
	private static final float TRIGGER_HP = 0.5f;

	private static final OwnedSessionMap<UUID, Session> ACTIVE =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	private PandoraDeathController() {
	}

	/** Pandora's own listeners — registered from {@code PandoraModule} (bootstrap runs once). */
	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			// Pandora never dies — lethal hits trigger her cinematic instead (#7).
			return PandoraDeathController.allowDamage(entity, source, amount);
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer serverPlayer) {
				return PandoraDeathController.allowDeath(serverPlayer, source);
			}
			return true;
		});

		ctx.lifecycle().onHeroClear(PandoraDeathController::resetOnHeroTaken);
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

	private static boolean isRevived(ServerPlayer player) {
		Boolean revived = player.getAttached(ModAttachments.PANDORA_REVIVED);
		return revived != null && revived;
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
		if (isRevived(pandora)) {
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
		if (!isRevived(player) && !ACTIVE.containsKey(player.getUUID())) {
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
		ACTIVE.put(pandora.getUUID(), pandora.getUUID(), session);

		// Keep her alive, pinned and untouchable for the whole sequence.
		pandora.setHealth(pandora.getMaxHealth());
		pandora.clearFire();
		pandora.removeAllEffects();
		EntityControlLock.acquire(pandora, ControlLockKind.INVULNERABLE, pandora);
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
		if (ACTIVE.size() == 0) {
			return;
		}
		Iterator<Map.Entry<UUID, Session>> it = ACTIVE.iterator();
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

		// Permanent revival: her hitbox is gone — nothing can ever damage her again. The lock
		// acquired at cinematic start stays held; the attachment makes the state survive relog.
		pandora.setAttached(ModAttachments.PANDORA_REVIVED, Boolean.TRUE);
		EntityControlLock.acquire(pandora, ControlLockKind.INVULNERABLE, pandora);

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
		player.setAttached(ModAttachments.PANDORA_REVIVED, null);
		EntityControlLock.release(player, ControlLockKind.INVULNERABLE, player.getUUID());
	}

	/**
	 * Re-applies the revived invulnerability after relog/respawn — the entity flag is not
	 * guaranteed on the fresh entity while the attachment state persists. Called from
	 * {@link PandoraHero#applyPassives}.
	 */
	public static void reapplyState(ServerPlayer player) {
		if (isRevived(player)) {
			EntityControlLock.acquire(player, ControlLockKind.INVULNERABLE, player);
		}
	}

}
