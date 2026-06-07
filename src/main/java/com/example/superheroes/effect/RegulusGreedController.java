package com.example.superheroes.effect;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegulusGreedController {
	private static final int FREEZE_TICKS = 200;
	private static final double PULL_STRENGTH = 0.6;
	private static final double MAX_MAGNET_DISTANCE = 110.0;

	private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("superheroes", "greed_knockback");
	private static final AttributeModifier KNOCKBACK_MODIFIER =
			new AttributeModifier(KNOCKBACK_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE);

	private static final Map<UUID, MagnetState> MAGNETS = new ConcurrentHashMap<>();
	private static final Map<UUID, FreezeState> FREEZES = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> CASTER_FREEZE_UNTIL = new ConcurrentHashMap<>();

	private RegulusGreedController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer caster : server.getPlayerList().getPlayers()) {
				UUID uid = caster.getUUID();
				boolean hasMagnet = MAGNETS.containsKey(uid);
				Long freezeUntil = CASTER_FREEZE_UNTIL.get(uid);
				boolean hasFreeze = freezeUntil != null && caster.level().getGameTime() < freezeUntil;
				if (hasMagnet || hasFreeze) {
					Vec3 dm = caster.getDeltaMovement();
					caster.setDeltaMovement(0, Math.min(0, dm.y), 0);
					caster.hurtMarked = true;
				} else if (freezeUntil != null) {
					removeKnockback(caster);
					CASTER_FREEZE_UNTIL.remove(uid);
				}
			}

			List<UUID> toRelease = new ArrayList<>();
			for (Map.Entry<UUID, FreezeState> e : FREEZES.entrySet()) {
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
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			FreezeState st = FREEZES.get(entity.getUUID());
			if (st == null) {
				return true;
			}
			st.queuedDamage.add(new QueuedDamage(source, amount));
			return false;
		});
	}

	public static boolean isFrozen(LivingEntity entity) {
		return FREEZES.containsKey(entity.getUUID());
	}

	public static void startMagnet(ServerPlayer player, LivingEntity victim) {
		MAGNETS.put(player.getUUID(), new MagnetState(victim.getUUID(), player.tickCount));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, 250, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, FREEZE_TICKS, -50, true, false, false));
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
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, FREEZE_TICKS, 4, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FREEZE_TICKS, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, FREEZE_TICKS, 1, true, false, true));
		applyKnockback(player);
		CASTER_FREEZE_UNTIL.put(player.getUUID(), player.level().getGameTime() + FREEZE_TICKS);

		FreezeState st = new FreezeState(victim.getUUID(), victim.getX(), victim.getY(), victim.getZ(), FREEZE_TICKS);
		boolean wasNoAi = false;
		if (victim instanceof Mob mob) {
			wasNoAi = mob.isNoAi();
			mob.setNoAi(true);
			st.restoreNoAi = wasNoAi;
		}
		FREEZES.put(victim.getUUID(), st);
		ServerLevel sl = (ServerLevel) player.level();
		sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.4f);
		sl.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0, 0, 0, 0);
	}

	private static void applyKnockback(ServerPlayer player) {
		AttributeInstance attr = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
		if (attr == null) return;
		if (!attr.hasModifier(KNOCKBACK_MODIFIER_ID)) {
			attr.addTransientModifier(KNOCKBACK_MODIFIER);
		}
	}

	private static void removeKnockback(ServerPlayer player) {
		AttributeInstance attr = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
		if (attr == null) return;
		attr.removeModifier(KNOCKBACK_MODIFIER_ID);
	}

	private record MagnetState(UUID victimId, int startTick) {
	}

	private static final class FreezeState {
		final UUID victimId;
		final double lockX, lockY, lockZ;
		int remaining;
		boolean restoreNoAi = false;
		final List<QueuedDamage> queuedDamage = new ArrayList<>();

		FreezeState(UUID victimId, double x, double y, double z, int remaining) {
			this.victimId = victimId;
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
			if (v instanceof Mob mob) {
				mob.setNoAi(restoreNoAi);
			}
			LinkedHashMap<SourceKey, AggregatedDamage> aggregated = new LinkedHashMap<>();
			for (QueuedDamage q : queuedDamage) {
				SourceKey key = SourceKey.of(q.source);
				aggregated.merge(key, new AggregatedDamage(q.source, q.amount), AggregatedDamage::merge);
			}
			for (AggregatedDamage agg : aggregated.values()) {
				if (!v.isAlive()) break;
				v.invulnerableTime = 0;
				v.hurt(agg.representative, agg.total);
			}
			ServerLevel sl = (ServerLevel) v.level();
			sl.playSound(null, v.getX(), v.getY(), v.getZ(),
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.8f);
			sl.sendParticles(ParticleTypes.EXPLOSION, v.getX(), v.getY() + 1.0, v.getZ(), 1, 0, 0, 0, 0);
		}
	}

	private record QueuedDamage(DamageSource source, float amount) {
	}

	private record SourceKey(UUID directEntityId, UUID causingEntityId, ResourceLocation typeId) {
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

	private static final class AggregatedDamage {
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
}
