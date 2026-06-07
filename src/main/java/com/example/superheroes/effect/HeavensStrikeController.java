package com.example.superheroes.effect;

import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.sound.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeavensStrikeController {
	public static final int WINDUP_TICKS = 80;
	private static final int FREEZE_REFRESH_TICKS = 18;

	public record Variant(int depth, int radius, float shakeIntensity, float damage,
	                      float pitch, double pillarRadius, double pillarHeight) {
		public static final Variant REINHARD = new Variant(15, 7, 6.0f, 1500f, 0.55f, 1.6, 90.0);
		public static final Variant RAIDEN = new Variant(8, 5, 3.5f, 60f, 0.78f, 0.9, 65.0);
	}

	public record Pending(Vec3 target, long startTick, long impactTick, Variant variant, Vec3 lockPos, float lockYaw, float lockPitch) {}

	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	private static final Set<Block> UNBREAKABLE = Set.of(
			Blocks.BEDROCK,
			Blocks.BARRIER,
			Blocks.END_PORTAL,
			Blocks.END_PORTAL_FRAME,
			Blocks.END_GATEWAY,
			Blocks.NETHER_PORTAL,
			Blocks.COMMAND_BLOCK,
			Blocks.CHAIN_COMMAND_BLOCK,
			Blocks.REPEATING_COMMAND_BLOCK,
			Blocks.STRUCTURE_BLOCK,
			Blocks.STRUCTURE_VOID,
			Blocks.JIGSAW,
			Blocks.LIGHT,
			Blocks.IRON_BLOCK,
			Blocks.NETHERITE_BLOCK,
			Blocks.OBSIDIAN,
			Blocks.CRYING_OBSIDIAN,
			Blocks.RESPAWN_ANCHOR,
			Blocks.REINFORCED_DEEPSLATE,
			Blocks.ANCIENT_DEBRIS,
			Blocks.BEACON
	);

	private HeavensStrikeController() {}

	public static boolean isCharging(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	public static boolean start(ServerPlayer player, Variant variant) {
		if (PENDING.containsKey(player.getUUID())) return false;
		ServerLevel level = player.serverLevel();
		long now = level.getGameTime();
		Vec3 target = findGroundTarget(player);
		Vec3 lockPos = player.position();
		PENDING.put(player.getUUID(), new Pending(target, now, now + WINDUP_TICKS, variant,
				lockPos, player.getYRot(), player.getXRot()));
		applyFreeze(player);
		level.playSound(null, target.x, target.y, target.z,
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.6f);
		level.playSound(null, target.x, target.y, target.z,
				SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.6f, 0.5f);
		level.sendParticles(ParticleTypes.FLASH,
				target.x, target.y + variant.pillarHeight, target.z, 4, 0, 0, 0, 0);
		return true;
	}

	public static void cancel(UUID id) {
		PENDING.remove(id);
	}

	private static void applyFreeze(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				FREEZE_REFRESH_TICKS + 6, 6, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
				FREEZE_REFRESH_TICKS + 6, 4, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
				FREEZE_REFRESH_TICKS + 6, 4, true, false, false));
	}

	private static void clearFreeze(ServerPlayer player) {
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.removeEffect(MobEffects.WEAKNESS);
		player.removeEffect(MobEffects.DIG_SLOWDOWN);
	}

	private static void enforceLock(ServerPlayer player, Pending p, long now) {
		long elapsed = now - p.startTick;
		if (elapsed > 0 && elapsed % FREEZE_REFRESH_TICKS == 0) {
			applyFreeze(player);
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.hasImpulse = true;
		player.fallDistance = 0f;
		Vec3 cur = player.position();
		double dx = cur.x - p.lockPos.x;
		double dy = cur.y - p.lockPos.y;
		double dz = cur.z - p.lockPos.z;
		if (dx * dx + dy * dy + dz * dz > 0.04) {
			player.teleportTo(p.lockPos.x, p.lockPos.y, p.lockPos.z);
			player.setYRot(p.lockYaw);
			player.setXRot(p.lockPitch);
			player.hurtMarked = true;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING.isEmpty()) return;
			Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Pending> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				if (player == null) {
					it.remove();
					continue;
				}
				Pending p = e.getValue();
				long now = player.serverLevel().getGameTime();
				enforceLock(player, p, now);
				if (now >= p.impactTick) {
					clearFreeze(player);
					impact(player, p);
					it.remove();
				} else {
					windupTick(player, p, now);
				}
			}
		});
	}

	private static Vec3 findGroundTarget(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos start = player.blockPosition();
		for (int dy = 0; dy < 40; dy++) {
			BlockPos bp = start.below(dy);
			if (!level.getBlockState(bp).isAir()) {
				return Vec3.atCenterOf(bp.above());
			}
		}
		return player.position();
	}

	private static void windupTick(ServerPlayer player, Pending p, long now) {
		ServerLevel level = player.serverLevel();
		Variant v = p.variant;
		long ticksDone = now - p.startTick;
		double progress = (double) ticksDone / WINDUP_TICKS;
		double swordY = p.target.y + v.pillarHeight * (1.0 - progress);

		RandomSource r = level.random;
		for (int i = 0; i < 5; i++) {
			level.sendParticles(ParticleTypes.END_ROD,
					p.target.x + (r.nextDouble() - 0.5) * v.pillarRadius * 1.4,
					swordY + i * 1.6,
					p.target.z + (r.nextDouble() - 0.5) * v.pillarRadius * 1.4,
					1, 0, 0.05, 0, 0.0);
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				p.target.x, swordY, p.target.z,
				6, v.pillarRadius * 0.6, 0.4, v.pillarRadius * 0.6, 0.02);
		level.sendParticles(ParticleTypes.FLAME,
				p.target.x, swordY - 0.5, p.target.z,
				4, v.pillarRadius * 0.4, 0.2, v.pillarRadius * 0.4, 0.0);

		if (now % 2 == 0) {
			int steps = (int) v.pillarHeight;
			for (int dy = 0; dy < steps; dy += 3) {
				level.sendParticles(ParticleTypes.END_ROD,
						p.target.x, p.target.y + dy, p.target.z,
						1, 0.08, 0.0, 0.08, 0.0);
			}
		}

		if (now % 10 == 0) {
			level.sendParticles(ParticleTypes.FLASH,
					p.target.x, swordY, p.target.z, 1, 0, 0, 0, 0);
			level.playSound(null, p.target.x, p.target.y, p.target.z,
					SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
					1.4f, 0.4f + (float) progress * 0.8f);
		}

		if (ticksDone == WINDUP_TICKS / 2) {
			level.playSound(null, p.target.x, p.target.y, p.target.z,
					SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 2.0f, 0.5f);
		}

		long ticksLeft = p.impactTick - now;
		if (ticksLeft == 60 || ticksLeft == 40 || ticksLeft == 20 || ticksLeft == 5) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.heavens_strike.charging",
							String.format("%.1f", ticksLeft / 20.0)),
					true);
		}
	}

	private static void impact(ServerPlayer player, Pending p) {
		ServerLevel level = player.serverLevel();
		Vec3 t = p.target;
		Variant v = p.variant;

		level.playSound(null, t.x, t.y, t.z, ModSounds.HOMELANDER_HAND_CLAP,
				SoundSource.PLAYERS, 4.0f, v.pitch);
		level.playSound(null, t.x, t.y, t.z, SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.PLAYERS, 4.0f, 0.4f);
		level.playSound(null, t.x, t.y, t.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
				SoundSource.WEATHER, 3.5f, 0.5f);
		level.playSound(null, t.x, t.y, t.z, SoundEvents.NETHERITE_BLOCK_HIT,
				SoundSource.PLAYERS, 3.0f, 0.4f);

		level.sendParticles(ParticleTypes.FLASH, t.x, t.y + 1, t.z, 8, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, t.x, t.y + 1, t.z,
				4, v.radius * 0.5, 1.0, v.radius * 0.5, 0);
		level.sendParticles(ParticleTypes.EXPLOSION, t.x, t.y + 0.5, t.z,
				24, v.radius, 1.0, v.radius, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, t.x, t.y + 1, t.z,
				120, v.radius * 0.8, 1.5, v.radius * 0.8, 0.15);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, t.x, t.y + 1, t.z,
				60, v.radius, 2.0, v.radius, 0.05);

		for (ServerPlayer near : PlayerLookup.around(level, t, 100.0)) {
			double dist = near.position().distanceTo(t);
			float intensity = (float) Math.max(0.05, 1.0 - dist / 100.0) * v.shakeIntensity;
			ServerPlayNetworking.send(near, new ScreenShakeS2CPayload(intensity, 36));
		}

		double dmgRange = v.radius + 2;
		AABB box = new AABB(
				t.x - dmgRange, t.y - 3, t.z - dmgRange,
				t.x + dmgRange, t.y + 6, t.z + dmgRange);
		DamageSource src = level.damageSources().playerAttack(player);
		double r2 = dmgRange * dmgRange;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& e.position().distanceToSqr(t) <= r2)) {
			le.invulnerableTime = 0;
			le.hurt(src, v.damage);
			Vec3 push = le.position().subtract(t);
			double horiz = Math.max(0.01, Math.sqrt(push.x * push.x + push.z * push.z));
			le.setDeltaMovement(push.x / horiz * 1.2, 0.7, push.z / horiz * 1.2);
			le.hurtMarked = true;
		}

		carveCrater(level, t, v.radius, v.depth);
	}

	private static void carveCrater(ServerLevel level, Vec3 center, int topRadius, int depth) {
		RandomSource r = level.random;
		BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
		int cx = (int) Math.floor(center.x);
		int cy = (int) Math.floor(center.y);
		int cz = (int) Math.floor(center.z);

		for (int dy = 0; dy <= depth + 2; dy++) {
			double tDepth = (double) dy / Math.max(1, depth);
			double ringRadius = topRadius * (1.0 - tDepth * 0.7) + r.nextDouble() * 0.6;
			int ir = Math.max(1, (int) Math.round(ringRadius));
			int rsq = ir * ir;
			int innerSq = Math.max(0, (ir - 1) * (ir - 1));
			for (int dx = -ir; dx <= ir; dx++) {
				for (int dz = -ir; dz <= ir; dz++) {
					int distSq = dx * dx + dz * dz;
					if (distSq > rsq) continue;
					if (distSq >= innerSq && r.nextFloat() < 0.45f) continue;
					if (dy == depth + 1 && r.nextFloat() < 0.5f) continue;
					if (dy == depth + 2 && r.nextFloat() < 0.75f) continue;
					mp.set(cx + dx, cy - dy, cz + dz);
					BlockState s = level.getBlockState(mp);
					if (s.isAir()) continue;
					if (UNBREAKABLE.contains(s.getBlock())) continue;
					if (s.getDestroySpeed(level, mp) < 0) continue;
					level.destroyBlock(mp, false);
				}
			}
		}

		for (int i = 0; i < 16; i++) {
			int dx = r.nextInt(topRadius * 2 + 1) - topRadius;
			int dz = r.nextInt(topRadius * 2 + 1) - topRadius;
			int dy = 1 + r.nextInt(Math.max(1, depth - 2));
			mp.set(cx + dx, cy - dy, cz + dz);
			BlockState s = level.getBlockState(mp);
			if (s.isAir() || UNBREAKABLE.contains(s.getBlock())) continue;
			if (s.getDestroySpeed(level, mp) < 0) continue;
			if (r.nextBoolean()) level.destroyBlock(mp, false);
		}
	}
}
