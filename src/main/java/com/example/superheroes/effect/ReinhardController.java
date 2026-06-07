package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Главный мозг Рейнхарда. Объединяет:
 *  - фазы (1→5 по полученному урону)
 *  - регенерацию (Regen II passive + 30s instant heal)
 *  - суперрефлексы (15% dodge + counter)
 *  - feniks resurrection (1 раз / жизнь)
 *  - адаптация урона (Wish — иммунитет к типам)
 *  - метка Суждения (свечение для Рейнхарда + бонус-урон)
 *  - отслеживание worthy-opponent (для логики достойности — на будущее)
 */
public final class ReinhardController {
	private static final int INSTA_REGEN_INTERVAL = 600; // 30 сек
	private static final float[] PHASE_THRESHOLDS = {0f, 30f, 70f, 130f, 220f}; // вход в фазу 2/3/4/5
	private static final float PHASE_ACCUM_PER_HIT_CAP = 15f;
	private static final float PHASE_DECAY_PER_TICK = 0.05f; // 1.0 in 20 ticks (~1/sec)
	private static final int PHASE_DECAY_DELAY_TICKS = 200; // 10s without damage
	private static final int RECENT_DAMAGE_LIMIT = 5;
	private static final float SUPER_REFLEX_DODGE_CHANCE = 0.15f;
	private static final float COUNTER_DAMAGE = 12.0f;
	private static final int COUNTER_LOCKOUT_TICKS = 40;

	private static final java.util.concurrent.ConcurrentHashMap<UUID, Long> COUNTER_LOCKOUT = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<UUID, Long> LAST_DAMAGE_TICK = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Set<UUID> DAMAGE_REENTRY_GUARD = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
	private static final Set<ResourceKey<DamageType>> NON_TRACKED = Set.of(
			DamageTypes.STARVE,
			DamageTypes.IN_WALL,
			DamageTypes.DROWN,
			DamageTypes.GENERIC_KILL,
			DamageTypes.OUTSIDE_BORDER
	);

	private static final Set<ResourceKey<DamageType>> NON_ADAPTABLE = Set.of(
			DamageTypes.MOB_ATTACK,
			DamageTypes.MOB_ATTACK_NO_AGGRO,
			DamageTypes.PLAYER_ATTACK
	);

	private ReinhardController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!isReinhard(player)) continue;
				tickReinhard(player);
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isReinhard(player)) {
				return onIncomingDamage(player, source, amount);
			}
			return true;
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isReinhard(player)) {
				return tryPhoenix(player, source);
			}
			return true;
		});
	}

	public static boolean isReinhard(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return ReinhardHero.ID.equals(data.heroId());
	}

	private static void tickReinhard(ServerPlayer player) {
		long now = player.serverLevel().getGameTime();
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);

		// Permanent Regen II
		if (player.tickCount % 100 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, true, false, true));
		}

		// Second Coming — обновляем длительные баффы каждые 5 секунд (хронически держим эффекты).
		if (state.inSecondComing() && player.tickCount % 100 == 0) {
			refreshSecondComingEffects(player);
		}

		// 30-сек инст-регенерация
		long lastInsta = state.lastInstaRegenTick();
		if (lastInsta == 0L) {
			state = state.withLastInstaRegenTick(now);
			player.setAttached(ModAttachments.REINHARD_STATE, state);
		} else if (now - lastInsta >= INSTA_REGEN_INTERVAL) {
			player.heal(player.getMaxHealth() * 0.4f);
			player.serverLevel().sendParticles(ParticleTypes.HEART,
					player.getX(), player.getY() + 1.0, player.getZ(),
					12, 0.5, 0.5, 0.5, 0.0);
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
			state = state.withLastInstaRegenTick(now);
			player.setAttached(ModAttachments.REINHARD_STATE, state);
		}

		// Mark of Judgment expire + glow
		if (state.judgmentTarget().isPresent()) {
			if (now > state.judgmentExpireTick()) {
				player.setAttached(ModAttachments.REINHARD_STATE,
						state.withJudgmentTarget(Optional.empty(), 0L));
			} else {
				ServerLevel level = player.serverLevel();
				Entity target = level.getEntity(state.judgmentTarget().get());
				if (target instanceof LivingEntity living && living.isAlive()) {
					// Glow тик (только для Рейнхарда — но Glowing видят все, так что упрощаем — visible to all)
					if (player.tickCount % 20 == 0) {
						living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
						level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
								living.getX(), living.getY() + living.getBbHeight() + 0.6, living.getZ(),
								3, 0.2, 0.1, 0.2, 0.02);
					}
				} else {
					player.setAttached(ModAttachments.REINHARD_STATE,
							state.withJudgmentTarget(Optional.empty(), 0L));
				}
			}
		}

		// Phase 4+ aura
		if (state.phase() >= 4 && player.tickCount % 4 == 0) {
			player.serverLevel().sendParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY() + 0.8, player.getZ(),
					4, 0.4, 0.7, 0.4, 0.02);
		}

		// Phase decay — рассасывание накопленного урона если игрок не получает урон
		if (player.tickCount % 20 == 0) {
			Long lastDamage = LAST_DAMAGE_TICK.get(player.getUUID());
			if (lastDamage != null && now - lastDamage >= PHASE_DECAY_DELAY_TICKS && state.accumulatedDamage() > 0f) {
				float decayed = Math.max(0f, state.accumulatedDamage() - PHASE_DECAY_PER_TICK * 20f);
				int newPhase = computePhase(decayed);
				ReinhardState updated = state.withAccumulatedDamage(decayed);
				if (newPhase < state.phase()) {
					for (int p = 1; p <= 5; p++) {
						HeroAttributes.buildReinhardPhaseSet(p).remove(player);
					}
					HeroAttributes.buildReinhardPhaseSet(newPhase).apply(player);
					updated = updated.withPhase(newPhase);
				}
				player.setAttached(ModAttachments.REINHARD_STATE, updated);
			}
		}
	}

	private static int computePhase(float accum) {
		int phase = 1;
		for (int i = PHASE_THRESHOLDS.length - 1; i >= 0; i--) {
			if (accum >= PHASE_THRESHOLDS[i]) {
				phase = i + 1;
				break;
			}
		}
		return phase;
	}

	private static boolean onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
		UUID pid = player.getUUID();
		if (!DAMAGE_REENTRY_GUARD.add(pid)) {
			return true;
		}
		try {
			return onIncomingDamageInner(player, source, amount);
		} finally {
			DAMAGE_REENTRY_GUARD.remove(pid);
		}
	}

	private static boolean onIncomingDamageInner(ServerPlayer player, DamageSource source, float amount) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		String typeId = damageTypeKey(source);
		long nowTick = player.serverLevel().getGameTime();

		// Adapted — иммунитет (skip non-adaptable types like mob_attack)
		if (state.adaptedDamageTypes().contains(typeId)) {
			player.serverLevel().sendParticles(ParticleTypes.GLOW,
					player.getX(), player.getY() + 1.0, player.getZ(),
					6, 0.3, 0.3, 0.3, 0.02);
			return false;
		}

		// Counter Riposte — активное парирование, отражает 200% обратно
		if (state.riposteExpireTick() > nowTick) {
			if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
				attacker.hurt(player.serverLevel().damageSources().playerAttack(player), amount * 2.0f);
				attacker.hurtMarked = true;
			}
			player.serverLevel().sendParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY() + 1.0, player.getZ(),
					24, 0.6, 0.6, 0.6, 0.15);
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.6f, 1.4f);
			return false;
		}

		// Track recent damage type (FIFO 5) — exclude non-adaptable types (mob_attack, player_attack)
		if (typeId != null && isAdaptable(source)) {
			List<String> recent = new ArrayList<>(state.recentDamageTypes());
			recent.remove(typeId);
			recent.add(0, typeId);
			while (recent.size() > RECENT_DAMAGE_LIMIT) recent.remove(recent.size() - 1);
			state = state.withRecentDamageTypes(recent);
			player.setAttached(ModAttachments.REINHARD_STATE, state);
		}

		// Wish penalty: после 3 желаний +30% урон
		float effective = amount;
		if (state.wishesUsed() >= 3) {
			effective *= 1.30f;
		}

		// Super reflexes: 15% уворот + контр-удар (только если атакующий — LivingEntity и не на КД)
		long now = nowTick;
		Long lockUntil = COUNTER_LOCKOUT.get(player.getUUID());
		boolean canCounter = lockUntil == null || now > lockUntil;
		if (canCounter && source.getEntity() instanceof LivingEntity attacker && attacker != player) {
			if (player.getRandom().nextFloat() < SUPER_REFLEX_DODGE_CHANCE) {
				player.serverLevel().sendParticles(ParticleTypes.PORTAL,
						player.getX(), player.getY() + 1.0, player.getZ(),
						20, 0.4, 0.5, 0.4, 0.5);
				player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);
				attacker.hurt(player.serverLevel().damageSources().playerAttack(player), COUNTER_DAMAGE);
				Vec3 push = player.position().subtract(attacker.position()).normalize().scale(-0.7);
				attacker.push(push.x, 0.3, push.z);
				attacker.hurtMarked = true;
				COUNTER_LOCKOUT.put(player.getUUID(), now + COUNTER_LOCKOUT_TICKS);
				return false;
			}
		}

		// Track accumulated damage for phase-up — cap per-hit contribution
		float accumDelta = Math.min(Math.max(effective, 0f), PHASE_ACCUM_PER_HIT_CAP);
		float newAccum = state.accumulatedDamage() + accumDelta;
		int newPhase = state.phase();
		if (newAccum >= PHASE_THRESHOLDS[Math.min(state.phase(), PHASE_THRESHOLDS.length - 1)]
				&& state.phase() < PHASE_THRESHOLDS.length) {
			newPhase = state.phase() + 1;
		}
		if (newPhase > state.phase()) {
			advancePhase(player, newPhase);
		}
		state = state.withAccumulatedDamage(newAccum).withPhase(newPhase);
		LAST_DAMAGE_TICK.put(player.getUUID(), nowTick);

		// Worthy opponent tracker (минимум 4 dmg single hit или 12 cumulative за 6 секунд)
		float worthyAccum = state.worthyAccumulatedDamage();
		long lastWorthy = state.lastWorthyHitTick();
		if (effective >= 4.0f) {
			worthyAccum = effective;
			lastWorthy = now;
		} else if (now - lastWorthy <= 120) {
			worthyAccum += effective;
			lastWorthy = now;
		} else {
			worthyAccum = effective;
			lastWorthy = now;
		}
		state = state.withWorthy(lastWorthy, worthyAccum);

		// Sword-draw gate — track per-attacker accumulated damage
		if (!state.swordDrawn() && source.getEntity() != null && source.getEntity() != player) {
			ReinhardSwordDrawGateController.recordHit(player, source.getEntity(), Math.max(amount, 0f));
		}

		// Phase 5 — God-tier damage reduction (50%)
		if (newPhase >= 5) {
			effective *= 0.5f;
		} else if (newPhase >= 4) {
			effective *= 0.65f;
		} else if (newPhase >= 3) {
			effective *= 0.80f;
		} else if (newPhase >= 2) {
			effective *= 0.92f;
		}

		player.setAttached(ModAttachments.REINHARD_STATE, state);

		if (Math.abs(effective - amount) > 0.001f) {
			if (effective > 0.001f) {
				player.hurt(source, effective);
			}
			return false;
		}
		return true;
	}

	private static void advancePhase(ServerPlayer player, int newPhase) {
		ServerLevel level = player.serverLevel();
		// Снять старые phase-modifiers, поставить новые
		for (int p = 1; p <= 5; p++) {
			HeroAttributes.buildReinhardPhaseSet(p).remove(player);
		}
		HeroAttributes.buildReinhardPhaseSet(newPhase).apply(player);
		player.heal(4f * newPhase);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.6, 1.0, 0.6, 0.15);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.8f + 0.1f * newPhase);
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard.phase_up", newPhase),
				true);
	}

	private static boolean tryPhoenix(ServerPlayer player, DamageSource source) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (state.phoenixUsed()) {
			// Второе пришествие уже было — позволяем умереть по-настоящему.
			HeroAttributes.REINHARD_SECOND_COMING.remove(player);
			return true;
		}
		int nextCount = state.phoenixCount() + 1;
		// Полная регенерация — Второе пришествие
		player.setHealth(1f);
		player.removeAllEffects();
		HeroAttributes.REINHARD_SECOND_COMING.apply(player);
		player.setHealth(player.getMaxHealth());
		applySecondComingEffects(player);

		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				240, 1.2, 1.8, 1.2, 0.3);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				160, 1.0, 1.4, 1.0, 0.25);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(),
				120, 0.8, 1.2, 0.8, 0.20);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
				player.getX(), player.getY() + 1.0, player.getZ(),
				80, 0.7, 1.0, 0.7, 0.4);
		level.sendParticles(ParticleTypes.FLASH,
				player.getX(), player.getY() + 1.0, player.getZ(),
				6, 0, 0, 0, 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.6f, 1.2f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 2.0f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.0f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.8f);
		player.setAttached(ModAttachments.REINHARD_STATE,
				state.withPhoenixUsed(true).withPhoenixCount(nextCount).withInSecondComing(true));
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard.phoenix", nextCount),
				true);
		return false;
	}

	private static void applySecondComingEffects(ServerPlayer player) {
		int duration = 24000; // 20 минут — фактически постоянно для одной жизни
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 1, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 1, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 4, true, false, false));
	}

	private static void refreshSecondComingEffects(ServerPlayer player) {
		applySecondComingEffects(player);
	}

	public static boolean isInSecondComing(ServerPlayer player) {
		if (!isReinhard(player)) return false;
		return player.getAttachedOrCreate(ModAttachments.REINHARD_STATE).inSecondComing();
	}

	public static void onDeath(ServerPlayer player) {
		for (int p = 1; p <= 5; p++) {
			HeroAttributes.buildReinhardPhaseSet(p).remove(player);
		}
		HeroAttributes.REINHARD_DRAW.remove(player);
		HeroAttributes.REINHARD_SECOND_COMING.remove(player);
	}

	public static void clearAdaptations(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		state = state.withAdaptedDamageTypes(List.of())
				.withRecentDamageTypes(List.of())
				.withAccumulatedDamage(0f)
				.withPhase(1)
				.withPhoenixUsed(false)
				.withPhoenixCount(0)
				.withInSecondComing(false);
		player.setAttached(ModAttachments.REINHARD_STATE, state);
		com.example.superheroes.effect.ReinhardSwordDrawCeremonyController.cancelCeremony(player);
		for (int p = 1; p <= 5; p++) {
			HeroAttributes.buildReinhardPhaseSet(p).remove(player);
		}
		HeroAttributes.REINHARD_DRAW.remove(player);
		HeroAttributes.REINHARD_SECOND_COMING.remove(player);
		state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		player.setAttached(ModAttachments.REINHARD_STATE, state.withSwordDrawn(false));
		com.example.superheroes.ability.ReinhardSwordDrawAbility.removeSword(player);
		COUNTER_LOCKOUT.remove(player.getUUID());
		LAST_DAMAGE_TICK.remove(player.getUUID());
	}

	public static void onRespawn(ServerPlayer player) {
		if (!isReinhard(player)) return;
		// На реальном респавне — сбрасываем Второе пришествие, фазы и feniks-флаг,
		// чтобы новая жизнь начиналась чисто и Второе пришествие было снова доступно.
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		state = state.withPhase(1)
				.withAccumulatedDamage(0f)
				.withPhoenixUsed(false)
				.withInSecondComing(false)
				.withSwordDrawn(false);
		player.setAttached(ModAttachments.REINHARD_STATE, state);
		for (int p = 1; p <= 5; p++) {
			HeroAttributes.buildReinhardPhaseSet(p).remove(player);
		}
		HeroAttributes.REINHARD_DRAW.remove(player);
		HeroAttributes.REINHARD_SECOND_COMING.remove(player);
	}

	private static String damageTypeKey(DamageSource source) {
		ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
		return key == null ? null : key.location().toString();
	}

	private static boolean isNonTracked(DamageSource source) {
		ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
		return key != null && NON_TRACKED.contains(key);
	}

	public static boolean isJudgmentTarget(ServerPlayer reinhard, LivingEntity target) {
		ReinhardState state = reinhard.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		return state.judgmentTarget().isPresent() && state.judgmentTarget().get().equals(target.getUUID());
	}

	public static boolean hasWorthyNearby(ServerPlayer player, double radius) {
		for (Entity e : player.serverLevel().getEntities(player,
				player.getBoundingBox().inflate(radius),
				e -> e instanceof LivingEntity le && ReinhardWorthyOpponent.isWorthy(le))) {
			return true;
		}
		return false;
	}

	public static boolean isAdaptable(DamageSource source) {
		ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
		return key != null && !NON_ADAPTABLE.contains(key) && !NON_TRACKED.contains(key);
	}
}
