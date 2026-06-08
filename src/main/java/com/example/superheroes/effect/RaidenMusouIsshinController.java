package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.item.MusouNoHitotachiItem;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RaidenMusouIsshinController {
	private static final int WINDUP_TICKS = 3 * 20;
	private static final int FREEZE_RADIUS = 50;
	private static final int FREEZE_REFRESH_TICKS = 10;
	private static final int SLASH_LENGTH = 45;
	private static final int SLASH_WIDTH = 5;
	private static final int SLASH_DEPTH = 6;
	private static final float SLASH_DAMAGE_PLAYER = 42f;
	private static final float SLASH_DAMAGE_MOB = 28f;
	private static final double IMPACT_RADIUS = 10.0;
	private static final float IMPACT_DAMAGE = 16f;

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
			Blocks.OBSIDIAN,
			Blocks.CRYING_OBSIDIAN,
			Blocks.REINFORCED_DEEPSLATE
	);

	private record Pending(Vec3 origin, Vec3 dir, Vec3 perp, Vec3 lockPos, float lockYaw, float lockPitch,
	                       long startTick, long impactTick) {
	}

	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	private RaidenMusouIsshinController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING.isEmpty()) return;
			Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Pending> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				if (player == null || !isRaiden(player) || !hasYamato(player)) {
					it.remove();
					continue;
				}
				Pending p = e.getValue();
				long now = player.serverLevel().getGameTime();
				enforceCasterLock(player, p, now);
				freezeNearby(player, p.origin, now, false);
				if (now >= p.impactTick) {
					impact(player, p);
					it.remove();
				} else {
					windupTick(player, p, now);
				}
			}
		});
	}

	public static boolean isCharging(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	public static boolean start(ServerPlayer player) {
		if (isCharging(player)) return false;
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 0.001) {
			flat = new Vec3(1, 0, 0);
		} else {
			flat = flat.normalize();
		}
		Vec3 origin = player.position();
		long now = player.serverLevel().getGameTime();
		PENDING.put(player.getUUID(), new Pending(origin, flat, new Vec3(-flat.z, 0, flat.x),
				origin, player.getYRot(), player.getXRot(), now, now + WINDUP_TICKS));
		ServerLevel level = player.serverLevel();
		applyCasterFreeze(player);
		freezeNearby(player, origin, now, true);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.45f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.4f, 0.7f);
		level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y + 1.4, origin.z, 2, 0, 0, 0, 0);
		level.sendParticles(ModParticles.MOONVEIL, origin.x, origin.y + 1.2, origin.z,
				28, 0.6, 0.8, 0.6, 0.08);
		return true;
	}

	private static void windupTick(ServerPlayer player, Pending p, long now) {
		ServerLevel level = player.serverLevel();
		long elapsed = now - p.startTick;
		double progress = (double) elapsed / WINDUP_TICKS;
		RandomSource r = level.random;

		if (now % 2 == 0) {
			for (int i = 2; i <= SLASH_LENGTH; i += 3) {
				Vec3 point = p.origin.add(p.dir.scale(i));
				int sy = findSurface(level, (int) Math.floor(point.x), (int) p.origin.y, (int) Math.floor(point.z));
				double pulse = Math.sin((progress * 10.0) + i * 0.4) * 0.5 + 0.5;
				level.sendParticles(ModParticles.ANOMALY_SLICE,
						point.x, sy + 1.0 + pulse, point.z,
						2, 0.25, 0.15, 0.25, 0.02);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						point.x, sy + 1.0, point.z,
						3, 0.6, 0.2, 0.6, 0.08);
			}
		}

		for (int i = 0; i < 10; i++) {
			double angle = r.nextDouble() * Math.PI * 2.0;
			double radius = 2.0 + r.nextDouble() * (FREEZE_RADIUS * 0.45);
			level.sendParticles(ModParticles.JIWALD_EFFECT,
					p.origin.x + Math.cos(angle) * radius,
					p.origin.y + 0.4 + r.nextDouble() * 2.5,
					p.origin.z + Math.sin(angle) * radius,
					1, 0.2, 0.2, 0.2, 0.02);
		}

		if (now % 10 == 0) {
			long ticksLeft = p.impactTick - now;
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_musou_isshin.charging",
							String.format("%.1f", ticksLeft / 20.0)), true);
			level.playSound(null, p.origin.x, p.origin.y, p.origin.z,
					SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
					1.8f, 0.35f + (float) progress * 0.9f);
		}

		if (elapsed == WINDUP_TICKS - 12) {
			player.swing(InteractionHand.MAIN_HAND, true);
			level.playSound(null, p.origin.x, p.origin.y, p.origin.z,
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.55f);
		}
	}

	private static void impact(ServerPlayer player, Pending p) {
		ServerLevel level = player.serverLevel();
		player.swing(InteractionHand.MAIN_HAND, true);
		carveSlash(level, p);
		damageSlash(level, player, p);
		spawnImpactFx(level, p);
		shake(level, p.origin);
	}

	private static void carveSlash(ServerLevel level, Pending p) {
		int halfWidth = SLASH_WIDTH / 2;
		for (int i = 2; i <= SLASH_LENGTH; i++) {
			Vec3 point = p.origin.add(p.dir.scale(i));
			for (int w = -halfWidth; w <= halfWidth; w++) {
				Vec3 offset = p.perp.scale(w);
				int ox = (int) Math.floor(point.x + offset.x);
				int oz = (int) Math.floor(point.z + offset.z);
				int sy = findSurface(level, ox, (int) p.origin.y, oz);
				for (int dy = 0; dy < SLASH_DEPTH; dy++) {
					BlockPos pos = new BlockPos(ox, sy - dy, oz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || UNBREAKABLE.contains(state.getBlock())) continue;
					if (state.getDestroySpeed(level, pos) < 0) continue;
					level.destroyBlock(pos, false);
				}
			}
		}
	}

	private static void damageSlash(ServerLevel level, ServerPlayer player, Pending p) {
		Vec3 end = p.origin.add(p.dir.scale(SLASH_LENGTH));
		AABB slashBox = normalizeAABB(new AABB(
				p.origin.x - 4, p.origin.y - SLASH_DEPTH, p.origin.z - 4,
				end.x + 4, p.origin.y + 6, end.z + 4));
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, slashBox,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player targetPlayer && targetPlayer.getUUID().equals(player.getUUID()))
						&& isInSlashPath(e.position(), p.origin, p.dir, p.perp));
		for (LivingEntity le : targets) {
			float dmg = (le instanceof Player) ? SLASH_DAMAGE_PLAYER : SLASH_DAMAGE_MOB;
			le.invulnerableTime = 0;
			le.hurt(level.damageSources().playerAttack(player), dmg);
			Vec3 push = le.position().subtract(p.origin);
			double horiz = Math.max(0.01, Math.sqrt(push.x * push.x + push.z * push.z));
			le.setDeltaMovement(push.x / horiz * 1.1, 0.55, push.z / horiz * 1.1);
			le.hurtMarked = true;
		}

		double r2 = IMPACT_RADIUS * IMPACT_RADIUS;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(end.x - IMPACT_RADIUS, end.y - 4, end.z - IMPACT_RADIUS,
						end.x + IMPACT_RADIUS, end.y + 7, end.z + IMPACT_RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator() && e.position().distanceToSqr(end) <= r2)) {
			le.invulnerableTime = 0;
			le.hurt(level.damageSources().playerAttack(player), IMPACT_DAMAGE);
		}
	}

	private static void spawnImpactFx(ServerLevel level, Pending p) {
		Vec3 mid = p.origin.add(p.dir.scale(SLASH_LENGTH * 0.5));
		Vec3 end = p.origin.add(p.dir.scale(SLASH_LENGTH));
		for (int i = 0; i < 4; i++) {
			Vec3 point = p.origin.add(p.dir.scale(8 + i * 10));
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
			if (bolt != null) {
				bolt.moveTo(point.x, findSurface(level, (int) point.x, (int) p.origin.y, (int) point.z) + 1.0, point.z);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}

		for (int i = 2; i <= SLASH_LENGTH; i += 2) {
			Vec3 point = p.origin.add(p.dir.scale(i));
			int sy = findSurface(level, (int) Math.floor(point.x), (int) p.origin.y, (int) Math.floor(point.z));
			level.sendParticles(ModParticles.ANOMALY_SLICE, point.x, sy + 1.2, point.z,
					8, 0.7, 0.4, 0.7, 0.08);
			level.sendParticles(ModParticles.MOONVEIL, point.x, sy + 1.5, point.z,
					4, 0.5, 0.6, 0.5, 0.1);
			level.sendParticles(ModParticles.SPARKS, point.x, sy + 0.8, point.z,
					20, 0.9, 0.5, 0.9, 0.2);
			if (i % 6 == 0) {
				level.sendParticles(ParticleTypes.EXPLOSION, point.x, sy + 0.8, point.z,
						1, 0.2, 0.1, 0.2, 0.0);
			}
		}

		level.sendParticles(ParticleTypes.FLASH, p.origin.x, p.origin.y + 1.4, p.origin.z, 4, 0, 0, 0, 0);
		level.sendParticles(ModParticles.WHITE_BOOM, mid.x, mid.y + 1.5, mid.z, 2, 0, 0, 0, 0);
		level.sendParticles(ModParticles.SWORD_EXPLOSION, mid.x, mid.y + 1.2, mid.z,
				90, 3.0, 1.2, 3.0, 0.25);
		level.sendParticles(ModParticles.SHAMAK, end.x, end.y + 1.0, end.z,
				60, 3.0, 0.8, 3.0, 0.15);
		level.sendParticles(ModParticles.JIWALD_EFFECT, mid.x, mid.y + 1.2, mid.z,
				260, 4.0, 1.8, 4.0, 0.8);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y + 1.0, end.z,
				2, 1.0, 0.3, 1.0, 0);

		level.playSound(null, p.origin.x, p.origin.y, p.origin.z,
				ModSounds.LIGHTNING_THUNDER_ANIME, SoundSource.PLAYERS, 2.8f, 0.65f);
		level.playSound(null, p.origin.x, p.origin.y, p.origin.z,
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.2f, 0.45f);
		level.playSound(null, mid.x, mid.y, mid.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.2f, 0.65f);
		level.playSound(null, end.x, end.y, end.z,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.4f, 0.55f);
		level.playSound(null, end.x, end.y, end.z,
				ModSounds.LIGHTNING_THUNDER_LOUD, SoundSource.WEATHER, 2.6f, 0.55f);
	}

	private static void freezeNearby(ServerPlayer caster, Vec3 center, long now, boolean force) {
		if (!force && now % FREEZE_REFRESH_TICKS != 0) return;
		ServerLevel level = caster.serverLevel();
		double r2 = FREEZE_RADIUS * FREEZE_RADIUS;
		AABB box = new AABB(center.x - FREEZE_RADIUS, center.y - 18, center.z - FREEZE_RADIUS,
				center.x + FREEZE_RADIUS, center.y + 18, center.z + FREEZE_RADIUS);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != caster && e.isAlive() && !e.isSpectator()
						&& e.position().distanceToSqr(center) <= r2)) {
			le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					FREEZE_REFRESH_TICKS + 12, 9, true, false, false));
			le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
					FREEZE_REFRESH_TICKS + 12, 3, true, false, false));
			le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
					FREEZE_REFRESH_TICKS + 12, 4, true, false, false));
			if (now % (FREEZE_REFRESH_TICKS * 2L) == 0) {
				level.sendParticles(ModParticles.BLUE_FLAME,
						le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(),
						3, 0.2, 0.4, 0.2, 0.02);
			}
		}
	}

	private static void applyCasterFreeze(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				WINDUP_TICKS + 8, 8, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
				WINDUP_TICKS + 8, 4, true, false, false));
	}

	private static void enforceCasterLock(ServerPlayer player, Pending p, long now) {
		if (now % FREEZE_REFRESH_TICKS == 0) {
			applyCasterFreeze(player);
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

	private static void shake(ServerLevel level, Vec3 center) {
		for (ServerPlayer near : PlayerLookup.around(level, center, 100.0)) {
			double dist = near.position().distanceTo(center);
			float intensity = (float) Math.max(0.08, 1.0 - dist / 100.0) * 4.5f;
			ServerPlayNetworking.send(near, new ScreenShakeS2CPayload(intensity, 42));
		}
	}

	private static int findSurface(ServerLevel level, int x, int originY, int z) {
		for (int y = originY + 8; y >= originY - 14; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
				return y;
			}
		}
		return originY;
	}

	private static boolean isInSlashPath(Vec3 entityPos, Vec3 origin, Vec3 dir, Vec3 perp) {
		Vec3 diff = entityPos.subtract(origin);
		double along = diff.x * dir.x + diff.z * dir.z;
		if (along < -3 || along > SLASH_LENGTH + 3) return false;
		double across = Math.abs(diff.x * perp.x + diff.z * perp.z);
		return across < SLASH_WIDTH + 1.5;
	}

	private static AABB normalizeAABB(AABB box) {
		return new AABB(
				Math.min(box.minX, box.maxX), Math.min(box.minY, box.maxY), Math.min(box.minZ, box.maxZ),
				Math.max(box.minX, box.maxX), Math.max(box.minY, box.maxY), Math.max(box.minZ, box.maxZ)
		);
	}

	private static boolean isRaiden(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && RaidenHero.ID.equals(data.heroId());
	}

	private static boolean hasYamato(ServerPlayer player) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		return main.getItem() instanceof MusouNoHitotachiItem || off.getItem() instanceof MusouNoHitotachiItem;
	}
}
