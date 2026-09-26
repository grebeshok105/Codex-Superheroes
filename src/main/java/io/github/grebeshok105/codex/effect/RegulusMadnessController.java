package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.AbilityScopedModifiers;
import io.github.grebeshok105.codex.hero.RegulusHero;
import io.github.grebeshok105.codex.network.MadnessSyncS2CPayload;
import io.github.grebeshok105.codex.network.MadnessVisualS2CPayload;
import io.github.grebeshok105.codex.transform.HeroData;
import io.github.grebeshok105.codex.world.WorldDestructionPolicy;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import io.github.grebeshok105.codex.damage.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

public final class RegulusMadnessController {
	private static final long READING_DURATION_TICKS = 200L;
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

	private static final int MADNESS_EFFECT_TICKS = 60;
	private static final int MADNESS_BUFF_AMPLIFIER = 2;

	private RegulusMadnessController() {
	}

	public static void register(HeroModuleContext ctx) {

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}
			RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
			if (state.isReading(player.level().getGameTime())) {
				return false;
			}
			return true;
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player) || !isRegulus(player)) {
				return;
			}
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
		});

		ctx.lifecycle().onJoin(RegulusMadnessController::clearMadness);
		ctx.lifecycle().onLeave(RegulusMadnessController::clearMadness);
		ctx.lifecycle().onDeath(RegulusMadnessController::clearMadness);
		ctx.lifecycle().onRespawn(RegulusMadnessController::clearMadness);
		ctx.lifecycle().onHeroClear(RegulusMadnessController::clearMadness);
		ctx.lifecycle().onServerStopped(server -> RegulusMadnessController.resetAll());
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

	/**
	 * Audit B16: the counter suppresses fall immunity only for its participants (the Regulus
	 * owner being held mid-air and the attacker being slammed) — not for every hero while any
	 * counter runs anywhere.
	 */
	public static boolean isCounterInvolved(Entity entity) {
		UUID id = entity.getUUID();
		for (CounterState counter : COUNTERS.values()) {
			if (counter.playerId.equals(id) || counter.attackerId.equals(id)) {
				return true;
			}
		}
		return false;
	}

	/** World shutdown — counters, cooldowns and damager memory die with the world. */
	public static void resetAll() {
		COUNTERS.clear();
		DODGE_COOLDOWN.clear();
		LAST_DAMAGER.clear();
		LAST_DAMAGER_TICK.clear();
	}

	private static void stripFlight(LivingEntity target) {
		if (!(target instanceof ServerPlayer sp)) return;
		try {
			io.github.grebeshok105.codex.ability.AbilityRouter.deactivate(sp, io.github.grebeshok105.codex.ability.AbilityIds.FLIGHT);
		} catch (Throwable ignored) {
		}
		try {
			io.github.grebeshok105.codex.ability.AbilityRouter.deactivate(sp, io.github.grebeshok105.codex.ability.AbilityIds.IRON_MAN_FLIGHT);
		} catch (Throwable ignored) {
		}
		try {
			io.github.grebeshok105.codex.ability.AbilityRouter.deactivate(sp, io.github.grebeshok105.codex.ability.AbilityIds.SUPERSONIC);
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
		if (state.isReading(player.level().getGameTime())) {
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
			EffectRefresh.refresh(player, MobEffects.DAMAGE_RESISTANCE, 8, 4, true, false, false);
			EffectRefresh.refresh(player, MobEffects.MOVEMENT_SLOWDOWN, 8, 250, true, false, false);
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
		} else if (state.readingUntilTick() > 0L && !state.madness()) {
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
		long now = player.level().getGameTime();
		RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS)
				.withReading(now + READING_DURATION_TICKS);
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

		AbilityScopedModifiers.REGULUS_MADNESS.apply(player);
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
		AbilityScopedModifiers.REGULUS_MADNESS.remove(player);
		// audit B12: only drop instances that look madness-applied — this hook runs on
		// join/death/respawn for EVERY player and used to wipe potion, beacon and hero
		// passive effects of the same holders
		removeMadnessEffect(player, MobEffects.MOVEMENT_SPEED, MADNESS_BUFF_AMPLIFIER);
		removeMadnessEffect(player, MobEffects.DAMAGE_BOOST, MADNESS_BUFF_AMPLIFIER);
		removeMadnessEffect(player, MobEffects.JUMP, MADNESS_BUFF_AMPLIFIER);
		removeMadnessEffect(player, MobEffects.DAMAGE_RESISTANCE, 0);
		LAST_DAMAGER.remove(player.getUUID());
		LAST_DAMAGER_TICK.remove(player.getUUID());
		DODGE_COOLDOWN.remove(player.getUUID());
		CounterState counter = COUNTERS.remove(player.getUUID());
		if (counter != null && player.level() instanceof ServerLevel sl) {
			counter.restoreOnAbort(sl);
		}
		player.setAttached(ModAttachments.REGULUS_MADNESS, RegulusMadnessState.EMPTY);
		ServerPlayNetworking.send(player, new MadnessVisualS2CPayload(MadnessVisualS2CPayload.EVENT_EXIT));
		sync(player);
	}

	private static void applyMadnessEffects(ServerPlayer player) {
		player.addEffect(madnessEffect(MobEffects.MOVEMENT_SPEED, MADNESS_BUFF_AMPLIFIER));
		player.addEffect(madnessEffect(MobEffects.DAMAGE_BOOST, MADNESS_BUFF_AMPLIFIER));
		player.addEffect(madnessEffect(MobEffects.JUMP, MADNESS_BUFF_AMPLIFIER));
		player.addEffect(madnessEffect(MobEffects.REGENERATION, 0));
		player.addEffect(madnessEffect(MobEffects.DAMAGE_RESISTANCE, 0));
	}

	private static MobEffectInstance madnessEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int amplifier) {
		return new MobEffectInstance(effect, MADNESS_EFFECT_TICKS, amplifier, true, false, true);
	}

	/**
	 * Madness instances are short ({@value #MADNESS_EFFECT_TICKS} ticks, refreshed every 40),
	 * ambient, icon-only and carry the fixed amplifiers from {@link #applyMadnessEffects}.
	 * Anything else on the same holder — a potion, a beacon, an infinite hero passive — is not
	 * ours to remove.
	 */
	private static void removeMadnessEffect(ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		MobEffectInstance instance = player.getEffect(effect);
		if (instance == null) {
			return;
		}
		boolean madnessOwned = instance.getDuration() > 0
				&& instance.getDuration() <= MADNESS_EFFECT_TICKS
				&& instance.getAmplifier() == amplifier
				&& instance.isAmbient()
				&& !instance.isVisible()
				&& instance.showIcon();
		if (madnessOwned) {
			player.removeEffect(effect);
		}
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
		long now = player.level().getGameTime();
		ServerPlayNetworking.send(player, new MadnessSyncS2CPayload(
				state.madness(),
				bonusLife != null && bonusLife,
				Math.max(0L, state.readingUntilTick() - now) * 50L,
				Math.max(0L, state.manaRegenLockUntilTick() - now) * 50L
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
						io.github.grebeshok105.codex.lifecycle.EntityControlLock.acquire(
								attacker, io.github.grebeshok105.codex.lifecycle.ControlLockKind.NO_AI, player);
						io.github.grebeshok105.codex.lifecycle.EntityControlLock.acquire(
								attacker, io.github.grebeshok105.codex.lifecycle.ControlLockKind.NO_GRAVITY, player);
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
						io.github.grebeshok105.codex.lifecycle.EntityControlLock.acquire(
								player, io.github.grebeshok105.codex.lifecycle.ControlLockKind.NO_GRAVITY, player);
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
					if (tick % 2 == 0) {
						level.sendParticles(ParticleTypes.FLASH,
								attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
								1, 0, 0, 0, 0);
					}
					if (tick >= COUNTER_ARRIVE_TICKS) {
						phase = Phase.SLAM;
						tick = 0;
						releaseLocks(attacker, player);
						attacker.setDeltaMovement(0, -3.5, 0);
						attacker.hurtMarked = true;
						attacker.hurt(ModDamageTypes.counterStrike(level, player), 30f);
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
			Entity ae = level.getEntity(attackerId);
			LivingEntity attacker = ae instanceof LivingEntity le ? le : null;
			releaseLocks(attacker, player);
		}

		private void releaseLocks(LivingEntity attacker, ServerPlayer player) {
			if (attacker != null) {
				io.github.grebeshok105.codex.lifecycle.EntityControlLock.release(
						attacker, io.github.grebeshok105.codex.lifecycle.ControlLockKind.NO_AI, playerId);
				io.github.grebeshok105.codex.lifecycle.EntityControlLock.release(
						attacker, io.github.grebeshok105.codex.lifecycle.ControlLockKind.NO_GRAVITY, playerId);
			}
			if (player != null) {
				io.github.grebeshok105.codex.lifecycle.EntityControlLock.release(
						player, io.github.grebeshok105.codex.lifecycle.ControlLockKind.NO_GRAVITY, playerId);
			}
		}

		boolean finalSlam(ServerLevel level, ServerPlayer player, LivingEntity attacker) {
			BlockPos impact = attacker.blockPosition();
			level.explode(player, impact.getX(), impact.getY(), impact.getZ(), 6.0f, Level.ExplosionInteraction.NONE);
			RegulusMadnessController.carveCrater(level, impact, player);
			attacker.teleportTo(impact.getX() + 0.5, impact.getY() - CRATER_DEPTH + 1, impact.getZ() + 0.5);
			attacker.hurt(ModDamageTypes.counterStrike(level, player), 27f);
			io.github.grebeshok105.codex.resource.EnergyLocks.lockTicks(player, 15 * 20);
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

		enum Phase { LIFT, ARRIVE, SLAM }
	}

	public static void carveCrater(ServerLevel level, BlockPos impact, @Nullable Entity cause) {
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
					WorldDestructionPolicy.tryCarve(level, p, cause);
				}
			}
		}
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		tickPlayer(player);
	}

	public static void tickCounters(MinecraftServer server) {
		List<UUID> done = new ArrayList<>();
		for (Map.Entry<UUID, CounterState> e : COUNTERS.entrySet()) {
			if (e.getValue().tick(server)) {
				done.add(e.getKey());
			}
		}
		for (UUID id : done) {
			COUNTERS.remove(id);
		}
	}

}
