package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController.AggregatedDamage;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController.QueuedDamage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Greed's Embrace stasis dome (replaces the lightning cage). The cast locks an
 * acquire anchor at 13 ticks; on the 18-tick fire a dome of radius
 * {@link #DOME_RADIUS} opens at the anchor and lives {@link #DOME_TICKS} ticks.
 * Mobs inside borrow NO_AI + NO_GRAVITY control locks, players get a positional
 * teleport lock — the same pin {@code RegulusGreedController} uses for freezes.
 * Incoming damage is queued per victim through the single ALLOW_DAMAGE arbiter
 * in {@link RegulusGreedController} and released, capped at
 * {@link #RELEASE_CAP_FRACTION} of max health, when the dome closes.
 */
public final class GreedStasisController {
	static final int ACQUIRE_TICKS = 13;
	static final int DOME_TICKS = 80;
	static final double DOME_RADIUS = 8.0;
	static final float RELEASE_CAP_FRACTION = 0.35f;
	private static final double AIM_RANGE = 40.0;
	private static final double IMPULSE = 1.1;
	private static final double IMPULSE_UP = 0.5;
	private static final ResourceLocation EMBRACE_ID = ModId.of("greeds_embrace");

	/** Casts awaiting anchor capture / fire — ClearOn drops them silently (no side effects). */
	private static final OwnedSessionMap<UUID, PendingCast> PENDING = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));
	/** Live domes keyed by caster. No ClearOn — removal must run the release flow first. */
	private static final OwnedSessionMap<UUID, StasisDome> DOMES = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private GreedStasisController() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.ticks().global(GreedStasisController::tickDomes);
		ctx.ticks().player(GreedStasisController::tickPlayer);
		ctx.lifecycle().onLeave(GreedStasisController::onCasterGone);
		ctx.lifecycle().onDeath(GreedStasisController::onCasterGone);
		ctx.lifecycle().onHeroClear(GreedStasisController::onCasterGone);
	}

	/** Cast start: records the pending cast so the acquire anchor can be captured at 13t. */
	public static void beginCast(ServerPlayer player) {
		PENDING.put(player.getUUID(), player.getUUID(),
				new PendingCast(player.level().getGameTime()));
	}

	/** Cast {@code onFire}: resolves the acquire-end anchor and opens the dome. */
	public static void tryOpen(ServerPlayer caster) {
		PendingCast pending = PENDING.remove(caster.getUUID());
		Vec3 anchor = pending != null && pending.anchor != null ? pending.anchor : aimPoint(caster);
		tryOpen(caster, anchor);
	}

	/** {@code onInterrupt} — drops the pending cast without side effects. */
	public static void cancelPending(ServerPlayer player) {
		PENDING.remove(player.getUUID());
	}

	/** True while {@code entity} is held inside a non-closing dome. */
	public static boolean inStasis(Entity entity) {
		for (Map.Entry<UUID, StasisDome> e : DOMES) {
			StasisDome dome = e.getValue();
			if (!dome.closing && dome.held.containsKey(entity.getUUID())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Single-arbiter branch of {@code RegulusGreedController}'s ALLOW_DAMAGE hook:
	 * queues the hit onto the holding dome. A dome in {@code closing} no longer
	 * queues — the release flow's own {@code hurt} must land, not re-queue.
	 */
	static boolean tryQueue(LivingEntity victim, DamageSource source, float amount) {
		for (Map.Entry<UUID, StasisDome> e : DOMES) {
			StasisDome dome = e.getValue();
			if (dome.closing) {
				continue;
			}
			Held held = dome.held.get(victim.getUUID());
			if (held != null) {
				held.queued.add(new QueuedDamage(source, amount));
				return true;
			}
		}
		return false;
	}

	/** Opens a dome at a fixed center; a live dome of the same caster is released first. */
	public static void tryOpen(ServerPlayer caster, Vec3 anchor) {
		MinecraftServer server = caster.server;
		StasisDome previous = DOMES.remove(caster.getUUID());
		if (previous != null) {
			releaseDome(server, previous);
		}
		StasisDome dome = new StasisDome(caster.getUUID(),
				caster.level().dimension(), anchor, caster.level().getGameTime());
		ServerLevel level = server.getLevel(dome.dimension);
		if (level != null) {
			captureVictims(caster, level, dome);
			level.playSound(null, anchor.x, anchor.y, anchor.z,
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.2f);
			level.sendParticles(ParticleTypes.END_ROD, anchor.x, anchor.y + 0.5, anchor.z,
					40, DOME_RADIUS * 0.5, 1.0, DOME_RADIUS * 0.5, 0.02);
			level.sendParticles(ParticleTypes.FLASH, anchor.x, anchor.y + 1.0, anchor.z, 1, 0, 0, 0, 0);
		}
		DOMES.put(caster.getUUID(), caster.getUUID(), dome);
		VfxFx.channel(caster, RegulusVfxIds.CHANNEL_GREED_STASIS,
				VfxChannelS2CPayload.START, anchor);
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		PendingCast pending = PENDING.get(player.getUUID());
		if (pending == null) {
			return;
		}
		if (!RegulusCastState.isCasting(player, EMBRACE_ID)) {
			PENDING.remove(player.getUUID());
			return;
		}
		if (pending.anchor == null
				&& player.level().getGameTime() >= pending.castStartTick + ACQUIRE_TICKS) {
			pending.anchor = aimPoint(player);
		}
	}

	public static void tickDomes(MinecraftServer server) {
		if (DOMES.size() == 0) {
			return;
		}
		List<UUID> toRelease = new ArrayList<>();
		for (Map.Entry<UUID, StasisDome> e : DOMES) {
			StasisDome dome = e.getValue();
			ServerLevel level = server.getLevel(dome.dimension);
			ServerPlayer caster = server.getPlayerList().getPlayer(dome.casterId);
			if (level == null || caster == null) {
				toRelease.add(e.getKey());
				continue;
			}
			tickDome(level, caster, dome);
			// Channel keepalive: the client-side dome closes on its own after 10t of
			// silence, so refresh well under that while the dome lives.
			if ((level.getGameTime() - dome.openTick) % 6 == 0) {
				VfxFx.channel(caster, RegulusVfxIds.CHANNEL_GREED_STASIS,
						VfxChannelS2CPayload.UPDATE, dome.center);
			}
			if (level.getGameTime() >= dome.openTick + DOME_TICKS) {
				toRelease.add(e.getKey());
			}
		}
		for (UUID id : toRelease) {
			StasisDome dome = DOMES.remove(id);
			if (dome != null) {
				releaseDome(server, dome);
			}
		}
	}

	private static void tickDome(ServerLevel level, ServerPlayer caster, StasisDome dome) {
		Iterator<Map.Entry<UUID, Held>> it = dome.held.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Held> en = it.next();
			Entity raw = level.getEntity(en.getKey());
			if (raw == null) {
				// Unloaded chunk — keep the entry; the lock shadow self-heals on reload.
				continue;
			}
			if (!(raw instanceof LivingEntity victim) || !victim.isAlive()) {
				releaseLocks(raw, dome.casterId);
				it.remove();
				continue;
			}
			pinHeld(victim, en.getValue());
		}
		captureVictims(caster, level, dome);
		spawnDomeParticles(level, dome);
	}

	private static void captureVictims(ServerPlayer caster, ServerLevel level, StasisDome dome) {
		AABB area = new AABB(dome.center, dome.center).inflate(DOME_RADIUS);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e.isAlive() && !dome.held.containsKey(e.getUUID())
						&& !RegulusGreedController.isFrozen(e)
						&& TargetFilters.hostileTo(caster).test(e)
						&& e.position().distanceToSqr(dome.center) <= DOME_RADIUS * DOME_RADIUS);
		for (LivingEntity victim : targets) {
			Held held = new Held(victim.getX(), victim.getY(), victim.getZ(),
					victim instanceof ServerPlayer);
			dome.held.put(victim.getUUID(), held);
			if (victim instanceof Mob) {
				EntityControlLock.acquire(victim, ControlLockKind.NO_AI, caster);
				EntityControlLock.acquire(victim, ControlLockKind.NO_GRAVITY, caster);
			}
			victim.setDeltaMovement(Vec3.ZERO);
			victim.hurtMarked = true;
		}
	}

	private static void pinHeld(LivingEntity victim, Held held) {
		if (held.player && victim instanceof ServerPlayer sp) {
			sp.connection.teleport(held.lockX, held.lockY, held.lockZ, sp.getYRot(), sp.getXRot());
		} else {
			victim.teleportTo(held.lockX, held.lockY, held.lockZ);
		}
		victim.setDeltaMovement(Vec3.ZERO);
		victim.hurtMarked = true;
	}

	/**
	 * Strict close order: mark {@code closing} so the arbiter lets release damage
	 * through, release the control locks, apply the capped aggregate, hurt, then
	 * impulse away from the center. Every dome teardown path lands here.
	 */
	private static void releaseDome(MinecraftServer server, StasisDome dome) {
		dome.closing = true;
		ServerPlayer caster = server.getPlayerList().getPlayer(dome.casterId);
		if (caster != null) {
			VfxFx.channel(caster, RegulusVfxIds.CHANNEL_GREED_STASIS,
					VfxChannelS2CPayload.STOP, dome.center);
		}
		ServerLevel level = server.getLevel(dome.dimension);
		for (Map.Entry<UUID, Held> en : dome.held.entrySet()) {
			Entity raw = findEntity(server, en.getKey());
			if (!(raw instanceof LivingEntity victim)) {
				continue;
			}
			releaseLocks(victim, dome.casterId);
			applyQueuedDamage(victim, en.getValue());
			applyImpulse(victim, dome.center);
		}
		if (level != null) {
			level.playSound(null, dome.center.x, dome.center.y, dome.center.z,
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.7f);
			level.sendParticles(ParticleTypes.EXPLOSION, dome.center.x, dome.center.y + 1.0, dome.center.z,
					4, DOME_RADIUS * 0.4, 1.0, DOME_RADIUS * 0.4, 0.0);
		}
	}

	/** The victim may have crossed dimensions while held — look past the dome's level. */
	private static Entity findEntity(MinecraftServer server, UUID entityId) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(entityId);
			if (entity != null) {
				return entity;
			}
		}
		return null;
	}

	private static void releaseLocks(Entity victim, UUID casterId) {
		EntityControlLock.release(victim, ControlLockKind.NO_AI, casterId);
		EntityControlLock.release(victim, ControlLockKind.NO_GRAVITY, casterId);
	}

	private static void applyQueuedDamage(LivingEntity victim, Held held) {
		if (held.queued.isEmpty()) {
			return;
		}
		float cap = victim.getMaxHealth() * RELEASE_CAP_FRACTION;
		float spent = 0f;
		for (AggregatedDamage agg : RegulusGreedController.aggregate(held.queued).values()) {
			if (!victim.isAlive() || spent >= cap) {
				break;
			}
			float amount = Math.min(agg.total, cap - spent);
			spent += amount;
			victim.invulnerableTime = 0;
			victim.hurt(agg.representative, amount);
		}
	}

	private static void applyImpulse(LivingEntity victim, Vec3 center) {
		Vec3 flat = new Vec3(victim.getX() - center.x, 0, victim.getZ() - center.z);
		Vec3 push = flat.lengthSqr() < 0.0025
				? new Vec3(0, IMPULSE_UP, 0)
				: flat.normalize().scale(IMPULSE).add(0, IMPULSE_UP, 0);
		victim.push(push.x, push.y, push.z);
		victim.hurtMarked = true;
	}

	private static void spawnDomeParticles(ServerLevel level, StasisDome dome) {
		if ((level.getGameTime() - dome.openTick) % 4 != 0) {
			return;
		}
		Vec3 c = dome.center;
		for (int i = 0; i < 16; i++) {
			double angle = i * (Math.PI * 2.0 / 16.0);
			level.sendParticles(ParticleTypes.END_ROD,
					c.x + Math.cos(angle) * DOME_RADIUS, c.y + 0.15, c.z + Math.sin(angle) * DOME_RADIUS,
					1, 0, 0, 0, 0);
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.0, c.z,
				3, DOME_RADIUS * 0.6, 1.2, DOME_RADIUS * 0.6, 0.0);
	}

	private static Vec3 aimPoint(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 farEnd = eye.add(player.getViewVector(1f).normalize().scale(AIM_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(
				eye, farEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 anchor = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : farEnd;
		return new Vec3(anchor.x, anchor.y + 0.1, anchor.z);
	}

	/** Leave / death / hero-clear of a caster releases their dome through the full flow. */
	private static void onCasterGone(ServerPlayer player) {
		StasisDome dome = DOMES.remove(player.getUUID());
		if (dome != null) {
			releaseDome(player.server, dome);
		}
	}

	private static final class PendingCast {
		final long castStartTick;
		Vec3 anchor;

		PendingCast(long castStartTick) {
			this.castStartTick = castStartTick;
		}
	}

	private static final class StasisDome {
		final UUID casterId;
		final ResourceKey<Level> dimension;
		final Vec3 center;
		final long openTick;
		boolean closing;
		final Map<UUID, Held> held = new LinkedHashMap<>();

		StasisDome(UUID casterId, ResourceKey<Level> dimension, Vec3 center, long openTick) {
			this.casterId = casterId;
			this.dimension = dimension;
			this.center = center;
			this.openTick = openTick;
		}
	}

	private static final class Held {
		final double lockX;
		final double lockY;
		final double lockZ;
		final boolean player;
		final List<QueuedDamage> queued = new ArrayList<>();

		Held(double lockX, double lockY, double lockZ, boolean player) {
			this.lockX = lockX;
			this.lockY = lockY;
			this.lockZ = lockZ;
			this.player = player;
		}
	}
}
