package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.attachment.ModAttachments;
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
	private static final float MANUAL_DRAIN_PER_TICK = MAX_DEMONISM / (22f * 20f);
	private static final int BUFF_TICKS = 80;
	private static final int CRATER_WINDUP_TICKS = 30;
	private static final int CRATER_COOLDOWN_TICKS = 14 * 20;
	private static final double CRATER_RANGE = 10.0;
	private static final double CRATER_RADIUS = 2.5;
	private static final int CRATER_DEPTH = 5;
	private static final float CRATER_DAMAGE = 28.0f;

	private static final Map<UUID, Float> CHARGE = new HashMap<>();
	private static final Set<UUID> ACTIVE = new HashSet<>();
	private static final Set<UUID> PERMANENT = new HashSet<>();
	private static final Map<UUID, CraterWindup> CRATER_WINDUPS = new HashMap<>();

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

	public static void giveMace(ServerPlayer player) {
		if (player.getMainHandItem().is(ModItems.REM_MORNING_STAR)) return;
		if (player.getOffhandItem().is(ModItems.REM_MORNING_STAR)) return;
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

	public static void removeMace(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModItems.REM_MORNING_STAR)) {
				player.getInventory().setItem(i, ItemStack.EMPTY);
			}
		}
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
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 4, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6, 3, true, false, false));
		Vec3 hand = player.getEyePosition().add(windup.forward.scale(0.9)).add(0.0, -0.45, 0.0);
		level.sendParticles(ParticleTypes.CRIT, hand.x, hand.y, hand.z, 8, 0.25, 0.25, 0.25, 0.08);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hand.x, hand.y, hand.z, 5, 0.2, 0.2, 0.2, 0.04);
		if (elapsed < CRATER_WINDUP_TICKS) {
			return false;
		}
		impactCrater(player, windup);
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.REM_MACE_CRATER, CRATER_COOLDOWN_TICKS);
		return true;
	}

	private static void impactCrater(ServerPlayer player, CraterWindup windup) {
		ServerLevel level = player.serverLevel();
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
			Vec3 push = target.position().subtract(player.position()).normalize().scale(1.6).add(0.0, 0.55, 0.0);
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
		player.setHealth(player.getMaxHealth());
		giveMace(player);
		ensureAbilityActive(player);
		playActivationFx(player, true);
		sync(player);
		return false;
	}

	private static void applyDemonEffects(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, BUFF_TICKS, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_TICKS, 1, true, false, true));
	}

	private static void removeDemonEffects(ServerPlayer player) {
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
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
}
