package com.example.superheroes.effect;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.AttributeModifierSet;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.network.RemDemonismS2CPayload;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RemDemonismController {
	public static final float MAX_DEMONISM = 100f;
	private static final float TAKEN_PER_DAMAGE = 0.55f;
	private static final float DEALT_PER_DAMAGE = 0.85f;
	private static final float PASSIVE_GAIN_PER_SECOND = 0.45f;
	private static final float MANUAL_DRAIN_PER_TICK = MAX_DEMONISM / (25f * 20f);
	private static final int BUFF_TICKS = 80;
	private static final int ABSORPTION_TICKS = 30 * 20;
	private static final int CRATER_WINDUP_TICKS = 30;
	private static final int CRATER_COOLDOWN_TICKS = 14 * 20;
	private static final double CRATER_RANGE = 10.0;
	private static final double CRATER_RADIUS = 2.5;
	private static final int CRATER_DEPTH = 5;
	private static final float CRATER_DAMAGE = 28.0f;
	private static final int ICE_SPIKE_COOLDOWN_TICKS = 9 * 20;
	private static final int ICE_WAVE_BANDS = 18;
	private static final double ICE_WAVE_RANGE = 22.0;
	private static final double ICE_WAVE_MAX_WIDTH = 5.0;
	private static final float ICE_SPIKE_DAMAGE = 13.0f;
	private static final int MORNING_STAR_PULL_TICKS = 20;
	private static final double MORNING_STAR_STOP_DISTANCE = 1.45;
	private static final double MORNING_STAR_MAX_PULL_SPEED = 1.35;
	private static final double MORNING_STAR_MAX_PULL_LIFT = 0.12;

	private static final AttributeModifierSet DEMON_ATTRIBUTES = AttributeModifierSet.builder()
			// Баланс: кулаки в демонизме били ~26 (голем за 3-4 удара) — снижено вдвое
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/rem/demon_damage"), 4.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/rem/demon_damage_mult"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/rem/demon_attack_speed"), 1.2, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, ModId.of("modifiers/rem/demon_speed"), 0.24, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ARMOR, ModId.of("modifiers/rem/demon_armor"), 22.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/rem/demon_toughness"), 14.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, ModId.of("modifiers/rem/demon_health"), 38.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, ModId.of("modifiers/rem/demon_kb"), 0.65, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, ModId.of("modifiers/rem/demon_reach"), 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.BLOCK_INTERACTION_RANGE, ModId.of("modifiers/rem/demon_block_reach"), 0.8, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, ModId.of("modifiers/rem/demon_step"), 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, ModId.of("modifiers/rem/demon_jump"), 0.08, AttributeModifier.Operation.ADD_VALUE)
			.build();

	private static final Map<UUID, Float> CHARGE = new HashMap<>();
	private static final Set<UUID> ACTIVE = new HashSet<>();
	private static final Set<UUID> PERMANENT = new HashSet<>();
	private static final Map<UUID, CraterWindup> CRATER_WINDUPS = new HashMap<>();
	private static final Map<UUID, IceSpikeWave> ICE_WAVES = new HashMap<>();
	private static final Map<UUID, MorningStarPull> MORNING_STAR_PULLS = new HashMap<>();

	private RemDemonismController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer victim && isRem(victim) && !isActive(victim)) {
				addCharge(victim, amount * TAKEN_PER_DAMAGE);
			}
			Entity attackerEntity = source.getEntity();
			if (attackerEntity instanceof ServerPlayer attacker && entity != attacker
					&& isRem(attacker) && !isActive(attacker)) {
				addCharge(attacker, amount * DEALT_PER_DAMAGE);
			}
			return true;
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isRem(player)) {
				return tryDeathSave(player, source);
			}
			return true;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Map.Entry<UUID, MorningStarPull>> pullIt = MORNING_STAR_PULLS.entrySet().iterator();
			while (pullIt.hasNext()) {
				Map.Entry<UUID, MorningStarPull> entry = pullIt.next();
				MorningStarPull pull = entry.getValue();
				ServerPlayer player = server.getPlayerList().getPlayer(pull.ownerId);
				if (player == null || !isRem(player) || !isActive(player)) {
					pullIt.remove();
					continue;
				}
				Entity entity = player.serverLevel().getEntity(entry.getKey());
				if (!(entity instanceof LivingEntity target) || !isValidTarget(player, target)) {
					pullIt.remove();
					continue;
				}
				long elapsed = player.serverLevel().getGameTime() - pull.startTick;
				if (tickMorningStarPull(player, target) || elapsed >= MORNING_STAR_PULL_TICKS) {
					stopMorningStarPull(target);
					pullIt.remove();
				}
			}
			Iterator<Map.Entry<UUID, IceSpikeWave>> iceIt = ICE_WAVES.entrySet().iterator();
			while (iceIt.hasNext()) {
				Map.Entry<UUID, IceSpikeWave> entry = iceIt.next();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null || !isRem(player) || !isActive(player)) {
					iceIt.remove();
					continue;
				}
				if (tickIceSpikeWave(player, entry.getValue())) {
					iceIt.remove();
				}
			}
			Iterator<Map.Entry<UUID, CraterWindup>> craterIt = CRATER_WINDUPS.entrySet().iterator();
			while (craterIt.hasNext()) {
				Map.Entry<UUID, CraterWindup> entry = craterIt.next();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null || !isRem(player) || !isActive(player)) {
					craterIt.remove();
					continue;
				}
				if (tickCraterWindup(player, entry.getValue())) {
					craterIt.remove();
				}
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (isRem(player)) {
					tickRem(player);
				} else if (CHARGE.containsKey(player.getUUID()) || ACTIVE.contains(player.getUUID())) {
					clear(player);
				}
			}
		});
	}

	public static boolean tryActivate(ServerPlayer player) {
		if (getCharge(player) < MAX_DEMONISM - 0.001f) {
			return false;
		}
		UUID id = player.getUUID();
		ACTIVE.add(id);
		PERMANENT.remove(id);
		CHARGE.put(id, MAX_DEMONISM);
		giveMace(player);
		applyDemonEffects(player);
		applyActivationAbsorption(player);
		player.heal(18f);
		playActivationFx(player, false);
		sync(player);
		return true;
	}

	public static void stopManual(ServerPlayer player) {
		UUID id = player.getUUID();
		if (PERMANENT.contains(id)) {
			ensureAbilityActive(player);
			sync(player);
			return;
		}
		ACTIVE.remove(id);
		CHARGE.put(id, 0f);
		CRATER_WINDUPS.remove(id);
		ICE_WAVES.remove(id);
		removeMorningStarPulls(id);
		removeMace(player);
		removeDemonEffects(player);
		sync(player);
	}

	public static void clear(ServerPlayer player) {
		UUID id = player.getUUID();
		ACTIVE.remove(id);
		PERMANENT.remove(id);
		CHARGE.remove(id);
		CRATER_WINDUPS.remove(id);
		ICE_WAVES.remove(id);
		removeMorningStarPulls(id);
		removeMace(player);
		removeDemonEffects(player);
		sync(player);
	}

	public static void onRespawn(ServerPlayer player) {
		clear(player);
	}

	public static boolean isActive(Player player) {
		return player != null && ACTIVE.contains(player.getUUID());
	}

	public static boolean isPermanent(Player player) {
		return player != null && PERMANENT.contains(player.getUUID());
	}

	public static float getCharge(ServerPlayer player) {
		return CHARGE.getOrDefault(player.getUUID(), 0f);
	}

	public static boolean startCraterAttack(ServerPlayer player) {
		if (!isActive(player) || CRATER_WINDUPS.containsKey(player.getUUID())
				|| AbilityCooldowns.isOnCooldown(player, AbilityIds.REM_MACE_CRATER)) {
			return false;
		}
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}
		CRATER_WINDUPS.put(player.getUUID(), new CraterWindup(
				player.position(), forward, player.serverLevel().getGameTime()));
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.2f, 1.35f);
		return true;
	}

	public static boolean isCraterWinding(ServerPlayer player) {
		return CRATER_WINDUPS.containsKey(player.getUUID());
	}

	public static boolean isIceWaveActive(ServerPlayer player) {
		return ICE_WAVES.containsKey(player.getUUID());
	}

	public static boolean startIceSpikeWave(ServerPlayer player) {
		if (!isActive(player) || ICE_WAVES.containsKey(player.getUUID())
				|| AbilityCooldowns.isOnCooldown(player, AbilityIds.REM_HUMA_ICE_SPIKES)) {
			return false;
		}
		Vec3 look = player.getViewVector(1f);
		Vec3 forward = new Vec3(look.x, 0.0, look.z);
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		} else {
			forward = forward.normalize();
		}
		Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
		ICE_WAVES.put(player.getUUID(), new IceSpikeWave(
				player.position(), forward, right, player.serverLevel().getGameTime()));
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.REM_HUMA_ICE_SPIKES, ICE_SPIKE_COOLDOWN_TICKS);
		ServerLevel level = player.serverLevel();
		player.swing(InteractionHand.MAIN_HAND, true);
		level.sendParticles(ParticleTypes.SNOWFLAKE,
				player.getX() + forward.x * 1.2, player.getY() + 0.15, player.getZ() + forward.z * 1.2,
				32, 0.8, 0.18, 0.8, 0.08);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.45f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.75f);
		return true;
	}

	public static void giveMace(ServerPlayer player) {
		removeExtraMaces(player, true);
		if (hasMace(player)) return;
		ItemStack stack = new ItemStack(ModItems.REM_MORNING_STAR);
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		} else if (player.getOffhandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.OFF_HAND, stack);
		} else if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	public static void startMorningStarPull(ServerPlayer player, LivingEntity target) {
		if (player == null || target == null || !isActive(player) || !isValidTarget(player, target)) {
			return;
		}
		MORNING_STAR_PULLS.put(target.getUUID(), new MorningStarPull(
				player.getUUID(), player.serverLevel().getGameTime()));
		if (tickMorningStarPull(player, target)) {
			stopMorningStarPull(target);
			MORNING_STAR_PULLS.remove(target.getUUID());
		}
	}

	public static void removeMace(ServerPlayer player) {
		removeExtraMaces(player, false);
	}

	private static void tickRem(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!ACTIVE.contains(id)) {
			if (player.tickCount % 20 == 0) {
				addCharge(player, PASSIVE_GAIN_PER_SECOND);
			}
			if (player.tickCount % 100 == 0) {
				sync(player);
			}
			return;
		}
		giveMace(player);
		applyDemonEffects(player);
		if (player.tickCount % 4 == 0) {
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.CHERRY_LEAVES,
					player.getX(), player.getY() + 1.45, player.getZ(),
					3, 0.18, 0.18, 0.18, 0.02);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					player.getX(), player.getY() + 1.0, player.getZ(),
					5, 0.35, 0.55, 0.35, 0.03);
		}
		if (PERMANENT.contains(id)) {
			CHARGE.put(id, MAX_DEMONISM);
			ensureAbilityActive(player);
			if (player.tickCount % 20 == 0) sync(player);
			return;
		}
		float next = Math.max(0f, CHARGE.getOrDefault(id, 0f) - MANUAL_DRAIN_PER_TICK);
		CHARGE.put(id, next);
		if (next <= 0f) {
			ACTIVE.remove(id);
			removeMace(player);
			removeDemonEffects(player);
			clearActiveAbility(player);
		}
		if (player.tickCount % 5 == 0) {
			sync(player);
		}
	}

	private static boolean tickCraterWindup(ServerPlayer player, CraterWindup windup) {
		ServerLevel level = player.serverLevel();
		long elapsed = level.getGameTime() - windup.startTick;
		double progress = Math.max(0.0, Math.min(1.0, elapsed / (double) CRATER_WINDUP_TICKS));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 6, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6, 5, true, false, false));
		Vec3 right = new Vec3(-windup.forward.z, 0.0, windup.forward.x);
		if (right.lengthSqr() < 1.0e-4) {
			right = new Vec3(1.0, 0.0, 0.0);
		} else {
			right = right.normalize();
		}
		Vec3 hand = player.getEyePosition()
				.add(windup.forward.scale(0.8 + progress * 0.35))
				.add(right.scale(Math.sin(progress * Math.PI) * 0.85))
				.add(0.0, 0.45 - progress * 0.85, 0.0);
		level.sendParticles(ParticleTypes.CRIT, hand.x, hand.y, hand.z, 12, 0.22, 0.22, 0.22, 0.10);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hand.x, hand.y, hand.z, 8, 0.18, 0.18, 0.18, 0.05);
		level.sendParticles(ParticleTypes.CHERRY_LEAVES, hand.x, hand.y, hand.z, 4, 0.15, 0.15, 0.15, 0.03);
		if (elapsed == 2 || elapsed == 14 || elapsed == CRATER_WINDUP_TICKS - 4) {
			player.swing(InteractionHand.MAIN_HAND, true);
			level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.65f + (float) progress * 0.45f);
		}
		if (elapsed % 4 == 0) {
			for (int i = 2; i <= CRATER_RANGE; i += 2) {
				Vec3 point = player.position().add(windup.forward.scale(i));
				int sy = findSurface(level, (int) Math.floor(point.x), (int) player.getY(), (int) Math.floor(point.z));
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
						point.x, sy + 0.15, point.z, 3, 0.25, 0.05, 0.25, 0.03);
				level.sendParticles(ParticleTypes.LARGE_SMOKE,
						point.x, sy + 0.1, point.z, 2, 0.2, 0.04, 0.2, 0.01);
			}
		}
		if (elapsed < CRATER_WINDUP_TICKS) {
			return false;
		}
		impactCrater(player, windup);
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.REM_MACE_CRATER, CRATER_COOLDOWN_TICKS);
		return true;
	}

	private static void impactCrater(ServerPlayer player, CraterWindup windup) {
		ServerLevel level = player.serverLevel();
		player.swing(InteractionHand.MAIN_HAND, true);
		Vec3 impact = player.position().add(windup.forward.scale(CRATER_RANGE));
		BlockPos center = BlockPos.containing(impact.x, player.getY() - 0.1, impact.z);
		DamageSource source = level.damageSources().playerAttack(player);
		AABB hitBox = new AABB(
				impact.x - CRATER_RADIUS, player.getY() - 1.0, impact.z - CRATER_RADIUS,
				impact.x + CRATER_RADIUS, player.getY() + 2.5, impact.z + CRATER_RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitBox, target -> isValidTarget(player, target))) {
			double dist = target.position().distanceTo(impact);
			if (dist > CRATER_RADIUS + 1.2) continue;
			float falloff = (float) Math.max(0.45, 1.0 - dist / (CRATER_RADIUS + 1.2));
			target.invulnerableTime = 0;
			target.hurt(source, CRATER_DAMAGE * falloff);
			target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 10 * 20, 0, false, true, true));
			Vec3 push = target.position().subtract(player.position()).normalize().scale(1.6).add(0.0, 0.18, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}
		carveCrater(level, center, player);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, center.getY() + 0.35, impact.z, 2, 0.4, 0.2, 0.4, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, impact.x, center.getY() + 0.25, impact.z, 80, 1.4, 0.35, 1.4, 0.12);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, impact.x, center.getY() + 0.8, impact.z, 30, 1.0, 0.4, 1.0, 0.12);
		level.playSound(null, impact.x, center.getY(), impact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.55f);
		level.playSound(null, impact.x, center.getY(), impact.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 0.75f);
		for (ServerPlayer nearby : PlayerLookup.around(level, impact, 34.0)) {
			ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(1.8f, 24));
		}
	}

	private static void carveCrater(ServerLevel level, BlockPos center, ServerPlayer player) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				double horizontal = Math.sqrt(dx * dx + dz * dz);
				if (horizontal > CRATER_RADIUS) continue;
				int depth = Math.max(1, CRATER_DEPTH - (int) Math.floor(horizontal));
				for (int dy = 0; dy > -depth; dy--) {
					pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)) {
						continue;
					}
					float hardness = state.getDestroySpeed(level, pos);
					if (hardness >= 0f && hardness <= 50f) {
						level.destroyBlock(pos.immutable(), true, player);
					}
				}
			}
		}
	}

	private static boolean tickIceSpikeWave(ServerPlayer player, IceSpikeWave wave) {
		ServerLevel level = player.serverLevel();
		long elapsed = level.getGameTime() - wave.startTick;
		int targetBand = Math.min(ICE_WAVE_BANDS, (int) (elapsed / 2L) + 1);
		while (wave.lastBand < targetBand) {
			wave.lastBand++;
			spawnIceBand(level, player, wave, wave.lastBand);
		}
		return wave.lastBand >= ICE_WAVE_BANDS && elapsed > ICE_WAVE_BANDS * 2L + 8L;
	}

	private static boolean tickMorningStarPull(ServerPlayer player, LivingEntity target) {
		Vec3 playerPos = player.position();
		Vec3 targetPos = target.position();
		Vec3 horizontalToPlayer = new Vec3(playerPos.x - targetPos.x, 0.0, playerPos.z - targetPos.z);
		double horizontalDistance = horizontalToPlayer.length();
		if (horizontalDistance <= MORNING_STAR_STOP_DISTANCE) {
			return true;
		}
		Vec3 direction = horizontalToPlayer.scale(1.0 / horizontalDistance);
		double speed = Math.min(MORNING_STAR_MAX_PULL_SPEED,
				(horizontalDistance - MORNING_STAR_STOP_DISTANCE) * 0.42);
		double lift = Math.min(MORNING_STAR_MAX_PULL_LIFT,
				Math.max(0.0, (playerPos.y - targetPos.y) * 0.08 + 0.035));
		setMorningStarPullMotion(target, direction.scale(speed).add(0.0, lift, 0.0));
		return false;
	}

	private static void stopMorningStarPull(LivingEntity target) {
		Vec3 current = target.getDeltaMovement();
		setMorningStarPullMotion(target, new Vec3(0.0, Math.min(current.y, MORNING_STAR_MAX_PULL_LIFT), 0.0));
	}

	private static void setMorningStarPullMotion(LivingEntity target, Vec3 motion) {
		target.setDeltaMovement(motion);
		target.hurtMarked = true;
		if (target instanceof ServerPlayer targetPlayer) {
			targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
		}
	}

	private static void spawnIceBand(ServerLevel level, ServerPlayer player, IceSpikeWave wave, int band) {
		double distance = 1.4 + band * (ICE_WAVE_RANGE / ICE_WAVE_BANDS);
		double width = Math.min(ICE_WAVE_MAX_WIDTH, 1.2 + distance * 0.18);
		Vec3 center = wave.origin.add(wave.forward.scale(distance));
		for (double offset = -width; offset <= width + 0.001; offset += 1.15) {
			Vec3 point = center.add(wave.right.scale(offset));
			int sy = findSurface(level, (int) Math.floor(point.x), (int) Math.floor(wave.origin.y), (int) Math.floor(point.z));
			spawnRisingIceSpike(level, point.x, sy, point.z, band, Math.abs(offset) / Math.max(1.0, width));
		}
		damageIceBand(level, player, wave, distance, width);
		if (band % 3 == 1) {
			level.playSound(null, center.x, center.y, center.z,
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.55f, 1.25f + band * 0.025f);
		}
	}

	private static void spawnRisingIceSpike(ServerLevel level, double x, int y, double z, int band, double edge) {
		double height = 0.7 + (band % 4) * 0.16 + (1.0 - edge) * 0.45;
		level.sendParticles(ParticleTypes.CLOUD, x, y + 0.05, z, 5, 0.22, 0.03, 0.22, 0.02);
		level.sendParticles(ParticleTypes.POOF, x, y + 0.12, z, 3, 0.16, 0.04, 0.16, 0.01);
		for (int i = 0; i < 4; i++) {
			double py = y + 0.12 + height * (i + 1) / 4.0;
			double spread = Math.max(0.03, 0.18 - i * 0.035);
			level.sendParticles(ParticleTypes.SNOWFLAKE, x, py, z, 5 - i,
					spread, 0.08, spread, 0.035);
			level.sendParticles(ParticleTypes.END_ROD, x, py + 0.03, z, 2,
					spread * 0.55, 0.05, spread * 0.55, 0.012);
		}
	}

	private static void damageIceBand(ServerLevel level, ServerPlayer player, IceSpikeWave wave,
			double distance, double width) {
		Vec3 center = wave.origin.add(wave.forward.scale(distance));
		AABB box = new AABB(center.x - width - 1.2, center.y - 3.0, center.z - width - 1.2,
				center.x + width + 1.2, center.y + 4.0, center.z + width + 1.2);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
				target -> isValidTarget(player, target))) {
			if (!wave.hitTargets.add(target.getUUID())) {
				continue;
			}
			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 rel = targetCenter.subtract(wave.origin);
			double forwardDist = rel.dot(wave.forward);
			double lateral = Math.abs(rel.dot(wave.right));
			if (Math.abs(forwardDist - distance) > 1.6 || lateral > width + 1.0) {
				wave.hitTargets.remove(target.getUUID());
				continue;
			}
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), ICE_SPIKE_DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 3, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, true, true, true));
			Vec3 push = wave.forward.scale(0.45).add(0.0, 0.12, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					targetCenter.x, targetCenter.y, targetCenter.z, 28, 0.4, 0.45, 0.4, 0.08);
		}
	}

	private static int findSurface(ServerLevel level, int x, int baseY, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int top = Math.min(level.getMaxBuildHeight() - 2, baseY + 7);
		int bottom = Math.max(level.getMinBuildHeight() + 1, baseY - 10);
		for (int y = top; y >= bottom; y--) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
				return y + 1;
			}
		}
		return baseY;
	}

	private static boolean tryDeathSave(ServerPlayer player, DamageSource source) {
		UUID id = player.getUUID();
		if (PERMANENT.contains(id)) {
			return true;
		}
		ACTIVE.add(id);
		PERMANENT.add(id);
		CHARGE.put(id, MAX_DEMONISM);
		player.setHealth(1f);
		player.removeAllEffects();
		applyDemonEffects(player);
		applyActivationAbsorption(player);
		player.setHealth(player.getMaxHealth());
		giveMace(player);
		ensureAbilityActive(player);
		playActivationFx(player, true);
		sync(player);
		return false;
	}

	private static void applyDemonEffects(ServerPlayer player) {
		DEMON_ATTRIBUTES.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, BUFF_TICKS, 0, true, false, true));
	}

	private static void applyActivationAbsorption(ServerPlayer player) {
		player.removeEffect(MobEffects.ABSORPTION);
		player.setAbsorptionAmount(0f);
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, 1, true, false, true));
	}

	private static void removeDemonEffects(ServerPlayer player) {
		DEMON_ATTRIBUTES.remove(player);
		player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		player.removeEffect(MobEffects.ABSORPTION);
	}

	private static void addCharge(ServerPlayer player, float amount) {
		if (amount <= 0f) return;
		UUID id = player.getUUID();
		float current = CHARGE.getOrDefault(id, 0f);
		float next = Math.min(MAX_DEMONISM, current + amount);
		if (next != current) {
			CHARGE.put(id, next);
			sync(player);
		}
	}

	private static void playActivationFx(ServerPlayer player, boolean deathSave) {
		ServerLevel level = player.serverLevel();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		level.sendParticles(ParticleTypes.CHERRY_LEAVES, x, y + 0.35, z, 120, 0.8, 1.0, 0.8, 0.18);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 90, 0.7, 0.9, 0.7, 0.16);
		level.sendParticles(ParticleTypes.FLASH, x, y, z, deathSave ? 4 : 1, 0, 0, 0, 0);
		level.playSound(null, x, y, z, deathSave ? SoundEvents.TOTEM_USE : SoundEvents.BEACON_ACTIVATE,
				SoundSource.PLAYERS, deathSave ? 1.9f : 1.2f, deathSave ? 1.05f : 1.45f);
		level.playSound(null, x, y, z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.3f, 1.55f);
	}

	private static void ensureAbilityActive(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.isActive(AbilityIds.REM_ONI_RAGE)) {
			HeroData updated = data.withActive(AbilityIds.REM_ONI_RAGE, true);
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncHeroData(player, updated);
		}
	}

	private static void clearActiveAbility(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.isActive(AbilityIds.REM_ONI_RAGE)) {
			HeroData updated = data.withActive(AbilityIds.REM_ONI_RAGE, false);
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncHeroData(player, updated);
		}
	}

	private static boolean isRem(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return RemHero.ID.equals(data.heroId());
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}

	private static boolean hasMace(ServerPlayer player) {
		return hasMace(player.getInventory().items)
				|| hasMace(player.getInventory().offhand)
				|| hasMace(player.getInventory().armor)
				|| isMace(player.containerMenu.getCarried());
	}

	private static void removeExtraMaces(ServerPlayer player, boolean keepOne) {
		boolean kept = !keepOne;
		kept = removeExtraMaces(player.getInventory().items, kept);
		kept = removeExtraMaces(player.getInventory().offhand, kept);
		kept = removeExtraMaces(player.getInventory().armor, kept);
		ItemStack carried = player.containerMenu.getCarried();
		if (isMace(carried)) {
			if (!kept) {
				carried.setCount(1);
			} else {
				player.containerMenu.setCarried(ItemStack.EMPTY);
			}
		}
	}

	private static boolean hasMace(NonNullList<ItemStack> slots) {
		for (ItemStack stack : slots) {
			if (isMace(stack)) {
				return true;
			}
		}
		return false;
	}

	private static boolean removeExtraMaces(NonNullList<ItemStack> slots, boolean kept) {
		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i);
			if (!isMace(stack)) {
				continue;
			}
			if (!kept) {
				stack.setCount(1);
				kept = true;
				continue;
			}
			slots.set(i, ItemStack.EMPTY);
		}
		return kept;
	}

	private static boolean isMace(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ModItems.REM_MORNING_STAR);
	}

	private static void removeMorningStarPulls(UUID ownerId) {
		MORNING_STAR_PULLS.entrySet().removeIf(entry -> ownerId.equals(entry.getValue().ownerId));
	}

	private static void sync(ServerPlayer player) {
		RemDemonismS2CPayload payload = new RemDemonismS2CPayload(
				player.getUUID(), getCharge(player), isActive(player), isPermanent(player));
		ServerPlayNetworking.send(player, payload);
		for (ServerPlayer observer : PlayerLookup.tracking(player)) {
			if (observer != player) {
				ServerPlayNetworking.send(observer, payload);
			}
		}
	}

	private record CraterWindup(Vec3 origin, Vec3 forward, long startTick) {
	}

	private record MorningStarPull(UUID ownerId, long startTick) {
	}

	private static final class IceSpikeWave {
		private final Vec3 origin;
		private final Vec3 forward;
		private final Vec3 right;
		private final long startTick;
		private final Set<UUID> hitTargets = new HashSet<>();
		private int lastBand;

		private IceSpikeWave(Vec3 origin, Vec3 forward, Vec3 right, long startTick) {
			this.origin = origin;
			this.forward = forward;
			this.right = right;
			this.startTick = startTick;
		}
	}
}
