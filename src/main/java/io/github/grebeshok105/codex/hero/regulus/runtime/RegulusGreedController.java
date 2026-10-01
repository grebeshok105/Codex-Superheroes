package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.MinecraftServer;

public final class RegulusGreedController {
	private static final ResourceLocation MANIA_ID = ModId.of("mania_of_greed");
	/** Players thaw fast, mobs hold the old 10 s lock. */
	private static final int FREEZE_TICKS_PLAYER = 80;
	private static final int FREEZE_TICKS_MOB = 200;
	/** Energy paid to turn a released magnet into a freeze; unpaid → the victim just walks. */
	private static final float FREEZE_ENERGY_COST = 150f;
	/** Aggregate release damage caps as a fraction of the victim's max health. */
	private static final float RELEASE_CAP_PLAYER = 0.40f;
	private static final float RELEASE_CAP_MOB = 0.60f;
	/** Duration of the caster's magnet-channel debuffs (re-pinned while the magnet lives). */
	private static final int MAGNET_DEBUFF_TICKS = 200;
	private static final double PULL_STRENGTH = 0.6;
	private static final double MAX_MAGNET_DISTANCE = 110.0;

	private static final OwnedSessionMap<UUID, MagnetState> MAGNETS = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));
	// ClearOn.EMPTY for both freeze maps: removal runs release() (control lock + queued
	// damage) inside onPlayerGone's ordering, never silently.
	private static final OwnedSessionMap<UUID, FreezeState> FREEZES = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));
	private static final OwnedSessionMap<UUID, Long> CASTER_FREEZE_UNTIL = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private RegulusGreedController() {
	}

	public static void register(HeroModuleContext ctx) {

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			// Stasis first: a dome-held victim queues into the dome and never reaches the
			// freeze queue — no double-queueing across the two systems.
			if (entity instanceof LivingEntity living
					&& GreedStasisController.tryQueue(living, source, amount)) {
				return false;
			}
			FreezeState st = FREEZES.get(entity.getUUID());
			if (st == null) {
				return true;
			}
			st.queuedDamage.add(new QueuedDamage(source, amount));
			return false;
		});

		ctx.lifecycle().onLeave(RegulusGreedController::onPlayerGone);
		ctx.lifecycle().onDeath(RegulusGreedController::onPlayerGone);
		ctx.lifecycle().onServerStopped(server -> RegulusGreedController.resetAll());
	}

	public static boolean isFrozen(LivingEntity entity) {
		return FREEZES.containsKey(entity.getUUID());
	}

	/** A live magnet channel is pulling a victim for this caster right now. */
	public static boolean hasMagnet(ServerPlayer player) {
		return MAGNETS.containsKey(player.getUUID());
	}

	/**
	 * The per-tick energy drain, run from the ability's {@code onTickActive} because
	 * {@code costPerTick()} is parameterless and cannot read the magnet state: nothing
	 * is charged before the authored fire tick grabbed a victim. Unpaid → deactivate.
	 */
	public static void drainMagnet(ServerPlayer player, float amount) {
		if (!MAGNETS.containsKey(player.getUUID())) {
			return;
		}
		if (ResourceController.charge(player, MANIA_ID, amount) == null) {
			AbilityRouter.deactivate(player, MANIA_ID);
		}
	}

	/** Server-thread leave/death hook — drops the caster's greed state and frees their victims. */
	public static void onPlayerGone(ServerPlayer player) {
		UUID id = player.getUUID();
		MAGNETS.remove(id);
		CASTER_FREEZE_UNTIL.remove(id);
		var server = player.server;
		for (Iterator<Map.Entry<UUID, FreezeState>> it = FREEZES.iterator(); it.hasNext();) {
			FreezeState st = it.next().getValue();
			if (!st.casterId.equals(id)) {
				continue;
			}
			st.release(server);
			it.remove();
		}
	}

	/** World shutdown — all greed state dies with the world. */
	public static void resetAll() {
		MAGNETS.clear();
		FREEZES.clear();
		CASTER_FREEZE_UNTIL.clear();
	}

	public static void startMagnet(ServerPlayer player, LivingEntity victim) {
		MAGNETS.put(player.getUUID(), player.getUUID(), new MagnetState(victim.getUUID(), player.tickCount));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MAGNET_DEBUFF_TICKS, 250, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, MAGNET_DEBUFF_TICKS, -50, true, false, false));
	}

	public static void tickMagnet(ServerPlayer player) {
		MagnetState m = MAGNETS.get(player.getUUID());
		if (m == null) {
			return;
		}
		LivingEntity victim = (LivingEntity) ((ServerLevel) player.level()).getEntity(m.victimId);
		if (victim == null || !victim.isAlive() || victim.distanceTo(player) > MAX_MAGNET_DISTANCE) {
			player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			player.removeEffect(MobEffects.JUMP);
			MAGNETS.remove(player.getUUID());
			return;
		}
		Vec3 dir = player.position().subtract(victim.position()).normalize().scale(PULL_STRENGTH);
		victim.setDeltaMovement(dir);
		victim.hurtMarked = true;
		ServerLevel sl = (ServerLevel) player.level();
		if (player.tickCount % 4 == 0) {
			Vec3 mid = player.position().add(victim.position()).scale(0.5);
			sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y + 1.0, mid.z, 4, 0.3, 0.3, 0.3, 0.02);
		}
		if (player.tickCount % 20 == 0) {
			sl.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.2f);
		}
	}

	public static void releaseAndFreeze(ServerPlayer player) {
		MagnetState pending = MAGNETS.get(player.getUUID());
		if (pending != null) {
			Entity candidate = ((ServerLevel) player.level()).getEntity(pending.victimId);
			// A victim already held by another caster (or by a stasis dome) refuses the
			// freeze before any side effect lands: no steroids, no locks, no freeze state
			// — only the channel itself is torn down (a leaked magnet entry roots the
			// caster forever via tickPlayer's hasMagnet check).
			if (candidate instanceof LivingEntity
					&& (FREEZES.containsKey(candidate.getUUID())
							|| GreedStasisController.inStasis(candidate))) {
				player.displayClientMessage(Component.translatable("superheroes.regulus.already_greed"), true);
				MAGNETS.remove(player.getUUID());
				player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				player.removeEffect(MobEffects.JUMP);
				return;
			}
		}
		MagnetState m = MAGNETS.remove(player.getUUID());
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.removeEffect(MobEffects.JUMP);
		if (m == null) {
			return;
		}
		LivingEntity victim = (LivingEntity) ((ServerLevel) player.level()).getEntity(m.victimId);
		if (victim == null || !victim.isAlive()) {
			return;
		}
		// The freeze costs 150 energy: unpaid, the victim is released without a lock.
		if (ResourceController.charge(player, MANIA_ID, FREEZE_ENERGY_COST) == null) {
			return;
		}
		int freezeTicks = victim instanceof ServerPlayer ? FREEZE_TICKS_PLAYER : FREEZE_TICKS_MOB;
		CASTER_FREEZE_UNTIL.put(player.getUUID(), player.getUUID(), player.level().getGameTime() + freezeTicks);

		FreezeState st = new FreezeState(victim.getUUID(), player.getUUID(),
				victim.getX(), victim.getY(), victim.getZ(), freezeTicks);
		EntityControlLock.acquire(victim, ControlLockKind.NO_AI, player);
		FREEZES.put(victim.getUUID(), player.getUUID(), st);
		ServerLevel sl = (ServerLevel) player.level();
		sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.4f);
		sl.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0, 0, 0, 0);
	}

	private record MagnetState(UUID victimId, int startTick) {
	}

	private static final class FreezeState {
		final UUID victimId;
		final UUID casterId;
		final double lockX, lockY, lockZ;
		int remaining;
		final List<QueuedDamage> queuedDamage = new ArrayList<>();

		FreezeState(UUID victimId, UUID casterId, double x, double y, double z, int remaining) {
			this.victimId = victimId;
			this.casterId = casterId;
			this.lockX = x;
			this.lockY = y;
			this.lockZ = z;
			this.remaining = remaining;
		}

		LivingEntity victim(net.minecraft.server.MinecraftServer server) {
			for (ServerLevel level : server.getAllLevels()) {
				Entity e = level.getEntity(victimId);
				if (e instanceof LivingEntity le) {
					return le;
				}
			}
			return null;
		}

		void release(net.minecraft.server.MinecraftServer server) {
			LivingEntity v = victim(server);
			if (v == null) return;
			EntityControlLock.release(v, ControlLockKind.NO_AI, casterId);
			// One release budget per thaw: capped at 40% (players) / 60% (mobs) of max
			// health across the whole aggregate, walked in insertion order.
			float cap = v.getMaxHealth() * (v instanceof ServerPlayer ? RELEASE_CAP_PLAYER : RELEASE_CAP_MOB);
			float spent = 0f;
			for (AggregatedDamage agg : aggregate(queuedDamage).values()) {
				if (!v.isAlive() || spent >= cap) break;
				float amount = Math.min(agg.total, cap - spent);
				spent += amount;
				v.invulnerableTime = 0;
				v.hurt(agg.representative, amount);
			}
			ServerLevel sl = (ServerLevel) v.level();
			sl.playSound(null, v.getX(), v.getY(), v.getZ(),
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.8f);
			sl.sendParticles(ParticleTypes.EXPLOSION, v.getX(), v.getY() + 1.0, v.getZ(), 1, 0, 0, 0, 0);
		}
	}

	/** The greed damage queue record — shared with the stasis dome's release flow. */
	record QueuedDamage(DamageSource source, float amount) {
	}

	/** Groups queued hits by representative source, insertion order kept. */
	static LinkedHashMap<SourceKey, AggregatedDamage> aggregate(List<QueuedDamage> queued) {
		LinkedHashMap<SourceKey, AggregatedDamage> aggregated = new LinkedHashMap<>();
		for (QueuedDamage q : queued) {
			SourceKey key = SourceKey.of(q.source());
			aggregated.merge(key, new AggregatedDamage(q.source(), q.amount()), AggregatedDamage::merge);
		}
		return aggregated;
	}

	record SourceKey(UUID directEntityId, UUID causingEntityId, ResourceLocation typeId) {
		static SourceKey of(DamageSource source) {
			Entity direct = source.getDirectEntity();
			Entity causing = source.getEntity();
			ResourceLocation typeId = source.typeHolder().unwrapKey()
					.map(k -> k.location()).orElse(null);
			return new SourceKey(
					direct != null ? direct.getUUID() : null,
					causing != null ? causing.getUUID() : null,
					typeId);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof SourceKey k)) return false;
			return Objects.equals(directEntityId, k.directEntityId)
					&& Objects.equals(causingEntityId, k.causingEntityId)
					&& Objects.equals(typeId, k.typeId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(directEntityId, causingEntityId, typeId);
		}
	}

	static final class AggregatedDamage {
		final DamageSource representative;
		float total;

		AggregatedDamage(DamageSource representative, float total) {
			this.representative = representative;
			this.total = total;
		}

		AggregatedDamage merge(AggregatedDamage other) {
			this.total += other.total;
			return this;
		}
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		UUID uid = player.getUUID();
		boolean hasMagnet = MAGNETS.containsKey(uid);
		Long freezeUntil = CASTER_FREEZE_UNTIL.get(uid);
		boolean hasFreeze = freezeUntil != null && player.level().getGameTime() < freezeUntil;
		if (hasMagnet || hasFreeze) {
			Vec3 dm = player.getDeltaMovement();
			player.setDeltaMovement(0, Math.min(0, dm.y), 0);
			player.hurtMarked = true;
		} else if (freezeUntil != null) {
			CASTER_FREEZE_UNTIL.remove(uid);
		}
	}

	public static void tickFreezes(MinecraftServer server) {
		List<UUID> toRelease = new ArrayList<>();
		for (Map.Entry<UUID, FreezeState> e : FREEZES) {
			FreezeState st = e.getValue();
			LivingEntity victim = st.victim(server);
			if (victim == null || !victim.isAlive()) {
				toRelease.add(e.getKey());
				continue;
			}
			victim.setDeltaMovement(Vec3.ZERO);
			victim.hurtMarked = true;
			if (victim instanceof ServerPlayer sp) {
				sp.connection.teleport(st.lockX, st.lockY, st.lockZ, sp.getYRot(), sp.getXRot());
			} else {
				victim.teleportTo(st.lockX, st.lockY, st.lockZ);
			}
			if (victim.tickCount % 4 == 0) {
				ServerLevel sl = (ServerLevel) victim.level();
				sl.sendParticles(ParticleTypes.END_ROD,
						victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
						3, 0.4, 0.4, 0.4, 0.0);
			}
			if (--st.remaining <= 0) {
				toRelease.add(e.getKey());
			}
		}
		for (UUID id : toRelease) {
			FreezeState st = FREEZES.remove(id);
			if (st != null) {
				st.release(server);
			}
		}
	}

}
