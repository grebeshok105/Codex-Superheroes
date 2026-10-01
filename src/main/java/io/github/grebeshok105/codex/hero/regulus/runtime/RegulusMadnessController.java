package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.mechanic.flight.FlightController;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.net.FxBroadcast;
import io.github.grebeshok105.codex.core.net.ScreenShakeS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.mechanic.effect.EffectRefresh;
import io.github.grebeshok105.codex.mechanic.flight.FlightController;
import io.github.grebeshok105.codex.mechanic.flight.FlightAbilityState;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.mechanic.falls.FallDamageHandlers;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.mechanic.world.WorldDestructionPolicy;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class RegulusMadnessController {
	private static final ResourceLocation REGULUS_ID = ModId.of("regulus");
	private static final ResourceLocation EVANGELION_ID = ModId.of("evangelion");

	/** The Evangelion channel: 60 game ticks of vanilla item use. */
	public static final int RITUAL_TICKS = 60;
	/** Damage at or above this aborts the ritual (interrupt, not block — the hit still lands). */
	public static final float RITUAL_INTERRUPT_DAMAGE = 4.0f;
	/** Moving more than this far from the ritual's anchor aborts it (0.5 blocks, squared). */
	private static final double RITUAL_MOVE_LIMIT_SQR = 0.25;
	/** Item cooldown armed on every early ritual abort. */
	public static final int INTERRUPT_COOLDOWN_TICKS = 400;
	/** Madness lasts 900 game ticks (45 s). */
	public static final int MADNESS_DURATION_TICKS = 900;
	/** The blood price: 0.6 HP of true damage every 20 ticks while mad. */
	public static final float BLOOD_PRICE_AMOUNT = 0.6f;
	private static final int BLOOD_PRICE_PERIOD_TICKS = 20;
	/** Tick inside the ritual when the authored evangelium_major VFX event fires (~1.66 s). */
	private static final int EVANGELIUM_MAJOR_TICK = 34;

	private static final int COUNTER_LIFT_TICKS = 20;
	private static final double COUNTER_LIFT_HEIGHT = 30.0;
	private static final int COUNTER_ARRIVE_TICKS = 20;
	/** The counter-attack clip fires this many ticks before ARRIVE ends so its 0.32 s impact meets the slam. */
	private static final int COUNTER_CLIP_LEAD_TICKS = 6;
	private static final int COUNTER_SLAM_TICKS = 120;
	/** One-hit slam formula: flat 15 + 15% of the victim's max health, hard cap 45. */
	private static final float COUNTER_DAMAGE_FLAT = 15f;
	private static final float COUNTER_DAMAGE_RATIO = 0.15f;
	private static final float COUNTER_DAMAGE_CAP = 45f;
	private static final double CRATER_RADIUS = 3.0;
	private static final int CRATER_DEPTH = 8;
	/** How far below the impact the victim is dropped — the old crater-depth reuse, now its own number. */
	private static final int SLAM_DROP_DEPTH = 20;
	/** The target must be a recorded damager no older than this (12 s) and inside {@link #COUNTER_SEARCH_RANGE}. */
	private static final double COUNTER_SEARCH_RANGE = 40.0;
	/** Energy lock armed by the final slam (8 s). */
	private static final int COUNTER_ENERGY_LOCK_TICKS = 8 * 20;
	/** Radius of the screen-shake audience around the slam impact. */
	private static final double SLAM_SHAKE_RADIUS = 24.0;
	private static final int SLAM_SHAKE_TICKS = 12;
	// ClearOn.EMPTY: a dropped counter must run restoreOnAbort inside clearMadness, not silently.
	private static final OwnedSessionMap<UUID, CounterState> COUNTERS = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private static final OwnedSessionMap<UUID, UUID> LAST_DAMAGER = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));
	private static final OwnedSessionMap<UUID, Long> LAST_DAMAGER_TICK = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));
	private static final int LAST_DAMAGER_TIMEOUT_TICKS = 240;

	/** Where each reader stood when the ritual began — the >0.5-block movement cancel anchor. */
	private static final OwnedSessionMap<UUID, Vec3> RITUAL_ANCHORS = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));

	private static final int MADNESS_EFFECT_TICKS = 60;
	private static final int MADNESS_BUFF_AMPLIFIER = 2;

	private RegulusMadnessController() {
	}

	public static void register(HeroModuleContext ctx) {
		FallDamageHandlers.registerImmunitySuppressor(RegulusMadnessController::isCounterInvolved);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}
			RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
			// The ritual is interruptible, not blocking: a heavy enough hit releases the
			// channel (releaseUsingItem → Item#releaseUsing → interruptRitual — NOT
			// stopUsingItem, which would skip the abort logic) and the damage still lands.
			if (state.isReading(player.level().getGameTime()) && amount >= RITUAL_INTERRUPT_DAMAGE) {
				player.releaseUsingItem();
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
				LAST_DAMAGER.put(player.getUUID(), player.getUUID(), damager.getUUID());
				LAST_DAMAGER_TICK.put(player.getUUID(), player.getUUID(), player.level().getGameTime());
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
	 * The counter's only target: the recorded last damager, still in reach. There is
	 * deliberately no {@code getLastHurtByMob} or nearest-hostile fallback — without a
	 * real attacker inside {@link #COUNTER_SEARCH_RANGE} the ability stays silent.
	 */
	public static LivingEntity findCounterTarget(ServerPlayer player) {
		LivingEntity tracked = getLastDamager(player);
		if (tracked != null && tracked.distanceTo(player) <= COUNTER_SEARCH_RANGE) {
			return tracked;
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
		for (Map.Entry<UUID, CounterState> e : COUNTERS) {
			CounterState counter = e.getValue();
			if (counter.playerId.equals(id) || counter.attackerId.equals(id)) {
				return true;
			}
		}
		return false;
	}

	/** World shutdown — counters, cooldowns and damager memory die with the world. */
	public static void resetAll() {
		COUNTERS.clear();
		LAST_DAMAGER.clear();
		LAST_DAMAGER_TICK.clear();
	}

	private static void stripFlight(LivingEntity target) {
		if (!(target instanceof ServerPlayer sp)) return;
		HeroData data = sp.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		for (ResourceLocation abilityId : new ArrayList<>(data.activeAbilities())) {
			if (!FlightAbilityState.isFlightAbility(abilityId)) {
				continue;
			}
			try {
				AbilityRouter.deactivate(sp, abilityId);
			} catch (Throwable ignored) {
			}
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
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
		long now = player.level().getGameTime();
		if (state.isReading(now)) {
			// Slowness II while the book is open — movement is allowed but beyond the
			// anchor limit it aborts the channel (checked in Item#onUseTick).
			EffectRefresh.refresh(player, MobEffects.MOVEMENT_SLOWDOWN, 8, 1, true, false, false);
			// A use-state dropped without releaseUsing (slot swap, item loss, stopUsingItem)
			// never reaches Item#releaseUsing — treat it as an abort here.
			if (!player.isUsingItem()) {
				interruptRitual(player);
				state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
			}
			ServerLevel level = (ServerLevel) player.level();
			if (now == state.ritualUntilTick() - RITUAL_TICKS + EVANGELIUM_MAJOR_TICK) {
				Vec3 p = player.position();
				VfxFx.event(player, RegulusVfxIds.EVANGELIUM_MAJOR, p, p, 1f);
			}
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
		} else if (state.ritualUntilTick() > 0L && now > state.ritualUntilTick() && !state.madness()) {
			// Deadline lapsed without finishUsingItem ever running — resolve as an abort.
			interruptRitual(player);
		}
		if (state.madness() && isRegulus(player)) {
			tickMadnessAmbient(player);
			if (state.madnessUntilTick() > 0L) {
				long elapsed = now - (state.madnessUntilTick() - MADNESS_DURATION_TICKS);
				if (now >= state.madnessUntilTick()) {
					clearMadness(player);
				} else if (elapsed > 0L && elapsed % BLOOD_PRICE_PERIOD_TICKS == 0) {
					player.hurt(RegulusDamageTypes.bloodPrice((ServerLevel) player.level()),
							BLOOD_PRICE_AMOUNT);
				}
			}
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
			// SOUL wisps — the rune-glyph ambient that replaced the old LAVA sparkle.
			level.sendParticles(ParticleTypes.SOUL,
					player.getX(), player.getY() + 0.3, player.getZ(),
					2, 0.3, 0.3, 0.3, 0.0);
		}
		if (t % 20 == 0) {
			// A ring of ENCHANT glyph-letters at chest height — runes circling the mad
			// reader (replaces the old ANGRY_VILLAGER sparkle; the real Veil rune is
			// Task 9's territory).
			for (int i = 0; i < 8; i++) {
				double a = (Math.PI / 4.0) * i;
				level.sendParticles(ParticleTypes.ENCHANT,
						player.getX() + Math.cos(a) * 0.9,
						player.getY() + 1.1,
						player.getZ() + Math.sin(a) * 0.9,
						1, 0.0, 0.05, 0.0, 0.0);
			}
		}
		if (t % 30 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
		}
	}

	public static boolean isRegulus(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.hasHero() && REGULUS_ID.equals(data.heroId());
	}

	public static void beginReading(ServerPlayer player) {
		long now = player.level().getGameTime();
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT)
				.withRitual(now + RITUAL_TICKS);
		player.setAttached(RegulusMadnessState.ATTACHMENT, state);
		RITUAL_ANCHORS.put(player.getUUID(), player.getUUID(), player.position());
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.4f, 0.6f);
		Vec3 p = player.position();
		VfxFx.event(player, RegulusVfxIds.ANIM_EVANGELIUM_ACTIVATION, p, p, 1f);
	}

	/**
	 * @return whether the player drifted more than 0.5 blocks from the ritual anchor
	 *         (checked from {@code Item#onUseTick} while the channel runs).
	 */
	public static boolean ritualMovedTooFar(ServerPlayer player) {
		Vec3 anchor = RITUAL_ANCHORS.get(player.getUUID());
		return anchor != null && player.position().distanceToSqr(anchor) > RITUAL_MOVE_LIMIT_SQR;
	}

	/** Every early exit from the ritual: releases the channel, clears the deadline, arms the cooldown. */
	public static void interruptRitual(ServerPlayer player) {
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
		if (state.ritualUntilTick() <= 0L) {
			return;
		}
		RITUAL_ANCHORS.remove(player.getUUID());
		player.setAttached(RegulusMadnessState.ATTACHMENT, state.withRitual(0L));
		Item book = BuiltInRegistries.ITEM.get(EVANGELION_ID);
		player.getCooldowns().addCooldown(book, INTERRUPT_COOLDOWN_TICKS);
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0f, 0.8f);
	}

	/** Natural completion of the 60-tick channel — Item#finishUsingItem calls this. */
	public static void completeRitual(ServerPlayer player) {
		long now = player.level().getGameTime();
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT)
				.withRitual(0L)
				.withMadness(true)
				.withMadnessUntil(now + MADNESS_DURATION_TICKS);
		player.setAttached(RegulusMadnessState.ATTACHMENT, state);
		// The single write-site for the bonus-life grant.
		player.setAttached(RegulusBonusLife.ATTACHMENT, Boolean.TRUE);
		RITUAL_ANCHORS.remove(player.getUUID());

		RegulusModifiers.REGULUS_MADNESS.apply(player);
		player.setHealth(player.getMaxHealth());
		applyMadnessEffects(player);

		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.5f, 0.7f);
		level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
				80, 1.5, 2.0, 1.5, 0.05);
	}

	public static void clearMadness(ServerPlayer player) {
		RegulusModifiers.REGULUS_MADNESS.remove(player);
		// audit B12: only drop instances that look madness-applied — this hook runs on
		// join/death/respawn for EVERY player and used to wipe potion, beacon and hero
		// passive effects of the same holders
		removeMadnessEffect(player, MobEffects.MOVEMENT_SPEED, MADNESS_BUFF_AMPLIFIER);
		removeMadnessEffect(player, MobEffects.DAMAGE_BOOST, MADNESS_BUFF_AMPLIFIER);
		removeMadnessEffect(player, MobEffects.JUMP, MADNESS_BUFF_AMPLIFIER);
		removeMadnessEffect(player, MobEffects.DAMAGE_RESISTANCE, 0);
		LAST_DAMAGER.remove(player.getUUID());
		LAST_DAMAGER_TICK.remove(player.getUUID());
		CounterState counter = COUNTERS.remove(player.getUUID());
		if (counter != null && player.level() instanceof ServerLevel sl) {
			counter.restoreOnAbort(sl);
		}
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
		boolean wasMad = state.madness();
		player.setAttached(RegulusMadnessState.ATTACHMENT, RegulusMadnessState.EMPTY);
		player.setAttached(RegulusBonusLife.ATTACHMENT, Boolean.FALSE);
		if (wasMad) {
			Vec3 p = player.position();
			VfxFx.event(player, RegulusVfxIds.ANIM_EVANGELIUM_DEACTIVATION, p, p, 1f);
		}
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
		Boolean has = player.getAttachedOrCreate(RegulusBonusLife.ATTACHMENT);
		if (has == null || !has) {
			return false;
		}
		player.setAttached(RegulusBonusLife.ATTACHMENT, Boolean.FALSE);
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.8f);
		level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
				120, 1.0, 2.0, 1.0, 0.1);
		return true;
	}

	public static void triggerCounter(ServerPlayer player, LivingEntity attacker) {
		stripFlight(attacker);

		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 1.4f);
		level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
				SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.7f, 1.0f);
		level.sendParticles(ParticleTypes.FLASH, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 3, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
				40, 0.8, 1.0, 0.8, 0.1);
		Vec3 at = attacker.position();
		VfxFx.event(player, RegulusVfxIds.COUNTER_LIFT, at, at, 1f);

		COUNTERS.put(player.getUUID(), player.getUUID(), new CounterState(player.getUUID(), attacker.getUUID(), level.dimension()));
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
						io.github.grebeshok105.codex.core.lifecycle.EntityControlLock.acquire(
								attacker, io.github.grebeshok105.codex.core.model.ControlLockKind.NO_AI, player);
						io.github.grebeshok105.codex.core.lifecycle.EntityControlLock.acquire(
								attacker, io.github.grebeshok105.codex.core.model.ControlLockKind.NO_GRAVITY, player);
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
						io.github.grebeshok105.codex.core.lifecycle.EntityControlLock.acquire(
								player, io.github.grebeshok105.codex.core.model.ControlLockKind.NO_GRAVITY, player);
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
					if (tick == COUNTER_ARRIVE_TICKS - COUNTER_CLIP_LEAD_TICKS) {
						// The 0.90 s counter_attack clip starts here so its authored
						// 0.32 s impact frame (~6.4 t later) lands on the contact.
						Vec3 origin = player.position();
						VfxFx.event(player, RegulusVfxIds.ANIM_COUNTER_ATTACK, origin, origin, 1f);
					}
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
						// The single counter hit — lift and slam together: 15 flat + 15% of
						// max health capped at 45, scaled by the owner's hearts.
						float damage = Math.min(COUNTER_DAMAGE_CAP,
								COUNTER_DAMAGE_FLAT + COUNTER_DAMAGE_RATIO * attacker.getMaxHealth())
								* RegulusHearts.damageScale(player);
						attacker.hurt(RegulusDamageTypes.counterStrike(level, player), damage);
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
				io.github.grebeshok105.codex.core.lifecycle.EntityControlLock.release(
						attacker, io.github.grebeshok105.codex.core.model.ControlLockKind.NO_AI, playerId);
				io.github.grebeshok105.codex.core.lifecycle.EntityControlLock.release(
						attacker, io.github.grebeshok105.codex.core.model.ControlLockKind.NO_GRAVITY, playerId);
			}
			if (player != null) {
				io.github.grebeshok105.codex.core.lifecycle.EntityControlLock.release(
						player, io.github.grebeshok105.codex.core.model.ControlLockKind.NO_GRAVITY, playerId);
			}
		}

		boolean finalSlam(ServerLevel level, ServerPlayer player, LivingEntity attacker) {
			BlockPos impact = attacker.blockPosition();
			// Strictly visual: no level.explode — even ExplosionInteraction.NONE hurts
			// entities, which would break the one-hit formula and catch bystanders.
			RegulusMadnessController.carveCrater(level, impact, player);
			attacker.teleportTo(impact.getX() + 0.5, impact.getY() - SLAM_DROP_DEPTH + 1, impact.getZ() + 0.5);
			EnergyLocks.lockTicks(player, COUNTER_ENERGY_LOCK_TICKS);
			level.playSound(null, impact.getX(), impact.getY(), impact.getZ(),
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.4f);
			level.playSound(null, impact.getX(), impact.getY(), impact.getZ(),
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.8f);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
					impact.getX(), impact.getY(), impact.getZ(), 3, 1.0, 0.5, 1.0, 0);
			level.sendParticles(ParticleTypes.LARGE_SMOKE,
					impact.getX(), impact.getY(), impact.getZ(), 80, 3.0, 1.0, 3.0, 0.1);
			Vec3 center = Vec3.atCenterOf(impact);
			VfxFx.event(player, RegulusVfxIds.COUNTER_SLAM_IMPACT, center, center, 1f);
			for (ServerPlayer nearby : FxBroadcast.aroundAudience(level, center, SLAM_SHAKE_RADIUS)) {
				float intensity = (float) Math.max(0.0,
						1.0 - nearby.position().distanceTo(center) / SLAM_SHAKE_RADIUS);
				if (intensity > 0.05f) {
					ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, SLAM_SHAKE_TICKS));
				}
			}
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
		for (Map.Entry<UUID, CounterState> e : COUNTERS) {
			if (e.getValue().tick(server)) {
				done.add(e.getKey());
			}
		}
		for (UUID id : done) {
			COUNTERS.remove(id);
		}
	}

}
