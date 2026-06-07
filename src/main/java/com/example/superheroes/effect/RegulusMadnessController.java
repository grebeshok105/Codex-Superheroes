package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.network.MadnessSyncS2CPayload;
import com.example.superheroes.network.MadnessVisualS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegulusMadnessController {
	private static final long READING_DURATION_MS = 10000L;
	private static final int COUNTER_LIFT_TICKS = 20;
	private static final double COUNTER_LIFT_HEIGHT = 30.0;
	private static final int COUNTER_ARRIVE_TICKS = 20;
	private static final int COUNTER_SLAM_TICKS = 120;
	private static final double CRATER_RADIUS = 4.0;
	private static final int CRATER_DEPTH = 20;
	private static final int DODGE_COOLDOWN_TICKS = 60;

	private static final Map<UUID, Long> DODGE_COOLDOWN = new ConcurrentHashMap<>();

	private static final Map<UUID, CounterState> COUNTERS = new ConcurrentHashMap<>();

	private static final Map<UUID, UUID> LAST_DAMAGER = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_DAMAGER_TICK = new ConcurrentHashMap<>();
	private static final int LAST_DAMAGER_TIMEOUT_TICKS = 200;

	private RegulusMadnessController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
			List<UUID> done = new ArrayList<>();
			for (Map.Entry<UUID, CounterState> e : COUNTERS.entrySet()) {
				if (e.getValue().tick(server)) {
					done.add(e.getKey());
				}
			}
			for (UUID id : done) {
				COUNTERS.remove(id);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			clearMadness(player);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				clearMadness(player);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			clearMadness(newPlayer);
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}
			RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
			if (state.isReading()) {
				return false;
			}
			if (isRegulus(player)) {
				Entity cause = source.getEntity();
				Entity direct = source.getDirectEntity();
				LivingEntity damager = null;
				if (cause instanceof LivingEntity le && le != player) {
					damager = le;
				} else if (direct instanceof LivingEntity le && le != player) {
					damager = le;
				}
				if (damager != null) {
					LAST_DAMAGER.put(player.getUUID(), damager.getUUID());
					LAST_DAMAGER_TICK.put(player.getUUID(), player.level().getGameTime());
				}
			}
			return true;
		});
	}

	public static LivingEntity getLastDamager(ServerPlayer player) {
		UUID damagerId = LAST_DAMAGER.get(player.getUUID());
		if (damagerId == null) return null;
		Long tick = LAST_DAMAGER_TICK.get(player.getUUID());
		if (tick == null || player.level().getGameTime() - tick > LAST_DAMAGER_TIMEOUT_TICKS) {
			LAST_DAMAGER.remove(player.getUUID());
			LAST_DAMAGER_TICK.remove(player.getUUID());
			return null;
		}
		Entity found = ((ServerLevel) player.level()).getEntity(damagerId);
		if (found instanceof LivingEntity le && le.isAlive()) {
			return le;
		}
		return null;
	}

	public static boolean isAnyCounterActive() {
		return !COUNTERS.isEmpty();
	}

	private static void stripFlight(LivingEntity target) {
		if (!(target instanceof ServerPlayer sp)) return;
		try {
			com.example.superheroes.ability.AbilityRouter.deactivate(sp, com.example.superheroes.ability.AbilityIds.FLIGHT);
		} catch (Throwable ignored) {
		}
		try {
			com.example.superheroes.ability.AbilityRouter.deactivate(sp, com.example.superheroes.ability.AbilityIds.IRON_MAN_FLIGHT);
		} catch (Throwable ignored) {
		}
		try {
			com.example.superheroes.ability.AbilityRouter.deactivate(sp, com.example.superheroes.ability.AbilityIds.SUPERSONIC);
		} catch (Throwable ignored) {
		}
		FlightController.stop(sp);
		net.minecraft.world.entity.player.Abilities a = sp.getAbilities();
		a.flying = false;
		if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.CREATIVE) {
			a.mayfly = false;
		}
		sp.onUpdateAbilities();
		sp.stopFallFlying();
	}

	private static void tickPlayer(ServerPlayer player) {
		RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
		if (state.isReading()) {
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 8, 4, true, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 250, true, false, false));
			ServerLevel level = (ServerLevel) player.level();
			if (player.tickCount % 2 == 0) {
				level.sendParticles(ParticleTypes.END_ROD,
						player.getX(), player.getY() + 1.0, player.getZ(),
						6, 0.8, 1.2, 0.8, 0.05);
				level.sendParticles(ParticleTypes.ENCHANT,
						player.getX(), player.getY() + 1.5, player.getZ(),
						12, 1.0, 1.5, 1.0, 0.4);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
						player.getX(), player.getY() + 1.0, player.getZ(),
						3, 0.6, 1.0, 0.6, 0.02);
			}
			if (player.tickCount % 30 == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
			}
		} else if (state.readingUntilMs() > 0L && !state.madness()) {
			finishReading(player);
		}
		if (state.madness() && isRegulus(player)) {
			tickMadnessAmbient(player);
		}
	}

	private static void tickMadnessAmbient(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		int t = player.tickCount;
		if (t % 40 == 0) {
			applyMadnessEffects(player);
		}
		if (t % 2 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					player.getX(), player.getY() + 0.2, player.getZ(),
					2, 0.4, 0.2, 0.4, 0.01);
			level.sendParticles(ParticleTypes.SMOKE,
					player.getX(), player.getY() + 0.4, player.getZ(),
					1, 0.3, 0.2, 0.3, 0.0);
		}
		if (t % 6 == 0) {
			level.sendParticles(ParticleTypes.ENCHANT,
					player.getX(), player.getY() + 1.2, player.getZ(),
					6, 0.8, 1.0, 0.8, 0.4);
			level.sendParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY() + 1.0, player.getZ(),
					2, 0.6, 0.8, 0.6, 0.02);
			level.sendParticles(ParticleTypes.LAVA,
					player.getX(), player.getY() + 0.1, player.getZ(),
					1, 0.3, 0.05, 0.3, 0.0);
		}
		if (t % 20 == 0) {
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
					player.getX(), player.getY() + 1.9, player.getZ(),
					2, 0.3, 0.1, 0.3, 0.0);
		}
		if (t % 30 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
		}
	}

	public static boolean isRegulus(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && RegulusHero.ID.equals(data.heroId());
	}

	public static void startReading(ServerPlayer player) {
		long now = System.currentTimeMillis();
		RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS)
				.withReading(now + READING_DURATION_MS);
		player.setAttached(ModAttachments.REGULUS_MADNESS, state);
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.4f, 0.6f);
		sync(player);
	}

	private static void finishReading(ServerPlayer player) {
		RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS)
				.withReading(0L)
				.withMadness(true);
		player.setAttached(ModAttachments.REGULUS_MADNESS, state);
		player.setAttached(ModAttachments.REGULUS_BONUS_LIFE, Boolean.TRUE);

		HeroAttributes.REGULUS_MADNESS.apply(player);
		player.setHealth(player.getMaxHealth());
		applyMadnessEffects(player);

		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.5f, 0.7f);
		level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
				80, 1.5, 2.0, 1.5, 0.05);
		ServerPlayNetworking.send(player, new MadnessVisualS2CPayload(MadnessVisualS2CPayload.EVENT_ENTER));
		sync(player);
	}

	public static void clearMadness(ServerPlayer player) {
		HeroAttributes.REGULUS_MADNESS.remove(player);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.JUMP);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		LAST_DAMAGER.remove(player.getUUID());
		LAST_DAMAGER_TICK.remove(player.getUUID());
		DODGE_COOLDOWN.remove(player.getUUID());
		COUNTERS.remove(player.getUUID());
		player.setAttached(ModAttachments.REGULUS_MADNESS, RegulusMadnessState.EMPTY);
		ServerPlayNetworking.send(player, new MadnessVisualS2CPayload(MadnessVisualS2CPayload.EVENT_EXIT));
		sync(player);
	}

	private static void applyMadnessEffects(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
	}

	public static boolean consumeBonusLife(ServerPlayer player) {
		Boolean has = player.getAttachedOrCreate(ModAttachments.REGULUS_BONUS_LIFE);
		if (has == null || !has) {
			return false;
		}
		player.setAttached(ModAttachments.REGULUS_BONUS_LIFE, Boolean.FALSE);
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.8f);
		level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
				120, 1.0, 2.0, 1.0, 0.1);
		sync(player);
		return true;
	}

	public static void sync(ServerPlayer player) {
		RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
		Boolean bonusLife = player.getAttachedOrCreate(ModAttachments.REGULUS_BONUS_LIFE);
		ServerPlayNetworking.send(player, new MadnessSyncS2CPayload(
				state.madness(),
				bonusLife != null && bonusLife,
				state.readingUntilMs(),
				state.manaRegenLockUntilMs()
		));
	}

	public static void triggerCounter(ServerPlayer player, LivingEntity attacker) {
		DODGE_COOLDOWN.put(player.getUUID(), player.level().getGameTime() + DODGE_COOLDOWN_TICKS);
		stripFlight(attacker);

		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 1.4f);
		level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
				SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.7f, 1.0f);
		level.sendParticles(ParticleTypes.FLASH, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 3, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
				40, 0.8, 1.0, 0.8, 0.1);

		COUNTERS.put(player.getUUID(), new CounterState(player.getUUID(), attacker.getUUID(), level.dimension()));
		sync(player);
	}

	private static final class CounterState {
		final UUID playerId;
		final UUID attackerId;
		final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim;
		int tick = 0;
		Phase phase = Phase.LIFT;
		boolean attackerWasNoAi;
		boolean attackerWasNoGravity;
		boolean playerWasNoGravity;
		double liftStartY;

		CounterState(UUID playerId, UUID attackerId, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {
			this.playerId = playerId;
			this.attackerId = attackerId;
			this.dim = dim;
		}

		boolean tick(net.minecraft.server.MinecraftServer server) {
			ServerLevel level = server.getLevel(dim);
			if (level == null) return true;
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			Entity ae = level.getEntity(attackerId);
			if (player == null || !(ae instanceof LivingEntity attacker) || !attacker.isAlive()) {
				restoreOnAbort(level);
				return true;
			}
			tick++;
			switch (phase) {
				case LIFT -> {
					if (tick == 1) {
						liftStartY = attacker.getY();
						if (attacker instanceof Mob mob) {
							attackerWasNoAi = mob.isNoAi();
							mob.setNoAi(true);
						}
						attackerWasNoGravity = attacker.isNoGravity();
						attacker.setNoGravity(true);
						playerWasNoGravity = player.isNoGravity();
					}
					double liftStep = COUNTER_LIFT_HEIGHT / (double) COUNTER_LIFT_TICKS;
					double targetY = Math.min(liftStartY + tick * liftStep, liftStartY + COUNTER_LIFT_HEIGHT);
					attacker.setDeltaMovement(0, 0, 0);
					attacker.teleportTo(attacker.getX(), targetY, attacker.getZ());
					attacker.hurtMarked = true;
					if (tick % 3 == 0) {
						level.sendParticles(ParticleTypes.END_ROD,
								attacker.getX(), attacker.getY() + 0.5, attacker.getZ(),
								5, 0.5, 0.8, 0.5, 0.05);
						level.sendParticles(ParticleTypes.GLOW,
								attacker.getX(), attacker.getY() + 0.5, attacker.getZ(),
								3, 0.4, 0.6, 0.4, 0.02);
					}
					if (tick >= COUNTER_LIFT_TICKS) {
						phase = Phase.ARRIVE;
						tick = 0;
						player.setNoGravity(true);
						player.setDeltaMovement(0, 0, 0);
						Vec3 look = attacker.getViewVector(1.0f);
						double bx = attacker.getX() - look.x * 1.2;
						double bz = attacker.getZ() - look.z * 1.2;
						float yaw = (float) Math.toDegrees(Math.atan2(look.x, -look.z)) + 180f;
						player.connection.teleport(bx, attacker.getY(), bz, yaw, 10f);
						level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
								SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.7f);
						level.sendParticles(ParticleTypes.REVERSE_PORTAL,
								player.getX(), player.getY() + 1.0, player.getZ(),
								40, 0.5, 1.0, 0.5, 0.2);
					}
				}
				case ARRIVE -> {
					attacker.setDeltaMovement(0, 0, 0);
					player.setDeltaMovement(0, 0, 0);
					player.setNoGravity(true);
					if (tick % 2 == 0) {
						level.sendParticles(ParticleTypes.FLASH,
								attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
								1, 0, 0, 0, 0);
					}
					if (tick >= COUNTER_ARRIVE_TICKS) {
						phase = Phase.SLAM;
						tick = 0;
						if (attacker instanceof Mob mob) {
							mob.setNoAi(attackerWasNoAi);
						}
						attacker.setNoGravity(attackerWasNoGravity);
						attacker.setDeltaMovement(0, -3.5, 0);
						attacker.hurtMarked = true;
						attacker.hurt(ModDamageTypes.counterStrike(level, player), 30f);
						player.setNoGravity(playerWasNoGravity);
						level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
								SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4f, 0.9f);
					}
				}
				case SLAM -> {
					if (!attacker.onGround() && !attacker.verticalCollision) {
						if (attacker.getDeltaMovement().y > -3.0) {
							attacker.setDeltaMovement(attacker.getDeltaMovement().x, -3.5, attacker.getDeltaMovement().z);
							attacker.hurtMarked = true;
						}
						level.sendParticles(ParticleTypes.LARGE_SMOKE,
								attacker.getX(), attacker.getY() + 0.2, attacker.getZ(),
								3, 0.3, 0.1, 0.3, 0.0);
					}
					if (attacker.onGround() || attacker.verticalCollision || tick >= COUNTER_SLAM_TICKS) {
						return finalSlam(level, player, attacker);
					}
				}
			}
			return false;
		}

		void restoreOnAbort(ServerLevel level) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
			if (player != null) player.setNoGravity(playerWasNoGravity);
			Entity ae = level.getEntity(attackerId);
			if (ae instanceof LivingEntity attacker) {
				attacker.setNoGravity(attackerWasNoGravity);
				if (attacker instanceof Mob mob) mob.setNoAi(attackerWasNoAi);
			}
		}

		boolean finalSlam(ServerLevel level, ServerPlayer player, LivingEntity attacker) {
			BlockPos impact = attacker.blockPosition();
			level.explode(player, impact.getX(), impact.getY(), impact.getZ(), 6.0f, Level.ExplosionInteraction.NONE);
			carveCrater(level, impact);
			attacker.teleportTo(impact.getX() + 0.5, impact.getY() - CRATER_DEPTH + 1, impact.getZ() + 0.5);
			attacker.hurt(ModDamageTypes.counterStrike(level, player), 27f);
			com.example.superheroes.resource.EnergyLocks.lockTicks(player, 15 * 20);
			level.playSound(null, impact.getX(), impact.getY(), impact.getZ(),
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.4f);
			level.playSound(null, impact.getX(), impact.getY(), impact.getZ(),
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.8f);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
					impact.getX(), impact.getY(), impact.getZ(), 3, 1.0, 0.5, 1.0, 0);
			level.sendParticles(ParticleTypes.LARGE_SMOKE,
					impact.getX(), impact.getY(), impact.getZ(), 80, 3.0, 1.0, 3.0, 0.1);
			return true;
		}

		void carveCrater(ServerLevel level, BlockPos impact) {
			int r = (int) CRATER_RADIUS;
			for (int dy = 0; dy < CRATER_DEPTH; dy++) {
				int radius = r - (dy * r / CRATER_DEPTH);
				if (radius < 1) radius = 1;
				int radiusSq = radius * radius;
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (dx * dx + dz * dz > radiusSq) continue;
						BlockPos p = impact.offset(dx, -dy, dz);
						if (p.getY() <= level.getMinBuildHeight()) continue;
						level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
					}
				}
			}
		}

		enum Phase { LIFT, ARRIVE, SLAM }
	}
}
