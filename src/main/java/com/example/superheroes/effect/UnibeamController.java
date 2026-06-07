package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class UnibeamController {
	public static final int CHARGE_TICKS = 200;
	public static final int FIRE_TICKS = 40;
	public static final int STUN_TICKS = 200;
	public static final double PULL_RADIUS = 50.0;
	public static final double DEBUFF_RADIUS = 100.0;
	public static final double BEAM_RANGE = 30.0;
	public static final double BEAM_RADIUS = 1.0;
	public static final int CRATER_LENGTH = 30;
	public static final int CRATER_HALF_WIDTH = 4;
	private static final float DIRECT_HIT_DAMAGE_PER_TICK = 4f;

	private static final Holder<MobEffect>[] CHARGE_DEBUFFS = effects(
			MobEffects.MOVEMENT_SLOWDOWN,
			MobEffects.WEAKNESS,
			MobEffects.DIG_SLOWDOWN,
			MobEffects.DARKNESS
	);
	private static final Holder<MobEffect>[] AOE_DEBUFFS = effects(
			MobEffects.MOVEMENT_SLOWDOWN,
			MobEffects.WEAKNESS,
			MobEffects.DIG_SLOWDOWN,
			MobEffects.HUNGER,
			MobEffects.CONFUSION,
			MobEffects.POISON,
			MobEffects.WITHER,
			MobEffects.DARKNESS
	);

	private static final Map<UUID, ChargeState> charging = new HashMap<>();
	private static final Map<UUID, FireState> firing = new HashMap<>();
	private static final Map<UUID, StunState> stunned = new HashMap<>();

	private UnibeamController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickCharging(player);
				tickFiring(player);
				tickStunned(player);
			}
			charging.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
			firing.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
			stunned.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
		});
	}

	public static boolean startCharge(ServerPlayer player) {
		UUID id = player.getUUID();
		if (charging.containsKey(id) || firing.containsKey(id) || stunned.containsKey(id)) {
			return false;
		}
		Vec3 anchor = findGroundAnchor(player);
		charging.put(id, new ChargeState(anchor));
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6f, 0.5f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.UNIBEAM_CHARGE, SoundSource.PLAYERS, 1.5f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.6f);
		return true;
	}

	public static boolean isBusy(ServerPlayer player) {
		UUID id = player.getUUID();
		return charging.containsKey(id) || firing.containsKey(id) || stunned.containsKey(id);
	}

	public static boolean isCharging(ServerPlayer player) {
		return charging.containsKey(player.getUUID());
	}

	public static boolean isFiring(ServerPlayer player) {
		return firing.containsKey(player.getUUID());
	}

	public static boolean isStunned(ServerPlayer player) {
		return stunned.containsKey(player.getUUID());
	}

	private static void tickCharging(ServerPlayer player) {
		ChargeState state = charging.get(player.getUUID());
		if (state == null) {
			return;
		}
		ServerLevel level = player.serverLevel();
		anchorPlayer(player, state.anchor);
		int t = state.progress;
		float p = (float) t / CHARGE_TICKS;
		Vec3 chest = chestOf(player);

		int rays = Math.max(6, Math.round(8 + 18 * p));
		for (int i = 0; i < rays; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2.0;
			double dist = 0.4 + level.getRandom().nextDouble() * (0.4 + 0.6 * p);
			double dy = (level.getRandom().nextDouble() - 0.5) * 0.7;
			double sx = chest.x + Math.cos(a) * dist;
			double sy = chest.y + dy;
			double sz = chest.z + Math.sin(a) * dist;
			level.sendParticles(ModParticles.UNIBEAM_SPARK,
					sx, sy, sz, 1,
					(chest.x - sx) * 0.5, (chest.y - sy) * 0.5, (chest.z - sz) * 0.5, 0.0);
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				chest.x, chest.y, chest.z,
				Math.round(2 + 6 * p), 0.25, 0.25, 0.25, 0.02);
		level.sendParticles(ParticleTypes.GLOW,
				chest.x, chest.y, chest.z,
				Math.round(1 + 4 * p), 0.4, 0.4, 0.4, 0.05);
		level.sendParticles(ParticleTypes.ENCHANTED_HIT,
				chest.x, chest.y, chest.z,
				Math.round(1 + 3 * p), 0.5, 0.5, 0.5, 0.05);
		if (t % 4 == 0) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					chest.x, chest.y, chest.z,
					Math.round(2 + 4 * p), 0.6, 0.6, 0.6, 0.08);
		}

		if (t % Math.max(2, 18 - (int) (16 * p)) == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
					0.6f + 0.6f * p, 0.6f + 1.4f * p);
		}
		if (t == 60 || t == 120 || t == 170) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.6f, 0.7f + 0.5f * p);
		}

		AABB pullBox = player.getBoundingBox().inflate(PULL_RADIUS);
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, pullBox,
				e -> e != player && e.isAlive() && !e.isSpectator());
		for (LivingEntity target : nearby) {
			Vec3 toPlayer = chest.subtract(target.position().add(0, target.getBbHeight() * 0.5, 0));
			double dist = toPlayer.length();
			if (dist < 0.6 || dist > PULL_RADIUS) {
				continue;
			}
			Vec3 pull = toPlayer.normalize().scale(0.04 + 0.04 * p);
			Vec3 dm = target.getDeltaMovement();
			target.setDeltaMovement(dm.x * 0.05 + pull.x, dm.y * 0.1 + pull.y * 0.5, dm.z * 0.05 + pull.z);
			target.hurtMarked = true;
			target.fallDistance = 0f;
			if (t % 20 == 0) {
				int duration = 60;
				int amplifier = Math.round(2 + 4 * p);
				for (Holder<MobEffect> effect : CHARGE_DEBUFFS) {
					target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
				}
			}
		}

		state.progress = t + 1;
		if (state.progress >= CHARGE_TICKS) {
			Vec3 dir = player.getViewVector(1f);
			Vec3 origin = chestOf(player);
			firing.put(player.getUUID(), new FireState(state.anchor, origin, dir));
			charging.remove(player.getUUID());
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					ModSounds.UNIBEAM_BLAST, SoundSource.PLAYERS, 4.0f, 0.9f);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0f, 0.7f);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					ModSounds.UNIBEAM_BEAM, SoundSource.PLAYERS, 3.5f, 1.0f);
		}
	}

	private static void tickFiring(ServerPlayer player) {
		FireState state = firing.get(player.getUUID());
		if (state == null) {
			return;
		}
		ServerLevel level = player.serverLevel();
		anchorPlayer(player, state.anchor);
		Vec3 chest = chestOf(player);
		Vec3 dir = state.dir;
		Vec3 end = chest.add(dir.scale(BEAM_RANGE));

		spawnBeamParticles(level, chest, dir, state.progress);
		damageEntitiesInBeam(player, level, chest, dir);
		carveTickAlongBeam(level, player, chest, dir, state.progress);

		if (state.progress % 5 == 0) {
			level.playSound(null, end.x, end.y, end.z,
					SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0f, 0.7f + level.getRandom().nextFloat() * 0.6f);
		}
		if (state.progress % 8 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.5f, 1.8f);
		}

		state.progress++;
		if (state.progress >= FIRE_TICKS) {
			finalizeBlast(player, level, chest, dir);
			firing.remove(player.getUUID());
			stunned.put(player.getUUID(), new StunState(state.anchor));
		}
	}

	private static void tickStunned(ServerPlayer player) {
		StunState state = stunned.get(player.getUUID());
		if (state == null) {
			return;
		}
		ServerLevel level = player.serverLevel();
		anchorPlayer(player, state.anchor);
		if (state.progress % 10 == 0) {
			level.sendParticles(ParticleTypes.SMOKE,
					player.getX(), player.getY() + 1.0, player.getZ(),
					6, 0.3, 0.6, 0.3, 0.02);
			level.sendParticles(ParticleTypes.LARGE_SMOKE,
					player.getX(), player.getY() + 1.2, player.getZ(),
					3, 0.3, 0.4, 0.3, 0.01);
		}
		if (state.progress == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4f, 0.6f);
		}
		state.progress++;
		if (state.progress >= STUN_TICKS) {
			stunned.remove(player.getUUID());
		}
	}

	private static void anchorPlayer(ServerPlayer player, Vec3 anchor) {
		Vec3 cur = player.position();
		double dx = cur.x - anchor.x;
		double dy = cur.y - anchor.y;
		double dz = cur.z - anchor.z;
		double d2 = dx * dx + dy * dy + dz * dz;
		if (d2 > 0.04) {
			player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.resetFallDistance();
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 250, false, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, 5, 128, false, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 0, false, false, false));
	}

	public static void clearState(UUID id) {
		charging.remove(id);
		firing.remove(id);
		stunned.remove(id);
	}

	private static Vec3 findGroundAnchor(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();
		BlockPos start = BlockPos.containing(pos);
		for (int dy = 0; dy < 32; dy++) {
			BlockPos here = start.below(dy);
			BlockState state = level.getBlockState(here);
			if (state.isAir() || state.liquid()) {
				continue;
			}
			if (state.getCollisionShape(level, here).isEmpty()) {
				continue;
			}
			return new Vec3(pos.x, here.getY() + 1.0, pos.z);
		}
		return pos;
	}

	private static Vec3 chestOf(ServerPlayer player) {
		return player.position().add(0, player.getBbHeight() * 0.55, 0);
	}

	private static void spawnBeamParticles(ServerLevel level, Vec3 origin, Vec3 dir, int progress) {
		RandomSource rand = level.getRandom();
		int slices = 60;
		for (int i = 0; i < slices; i++) {
			double f = (i + rand.nextDouble()) / slices;
			Vec3 center = origin.add(dir.scale(f * BEAM_RANGE));
			int spread = 2 + (int) (BEAM_RADIUS * 4);
			for (int j = 0; j < spread; j++) {
				double a = rand.nextDouble() * Math.PI * 2.0;
				double r = rand.nextDouble() * BEAM_RADIUS;
				Vec3 perp = perpendicular(dir);
				Vec3 perp2 = dir.cross(perp).normalize();
				Vec3 off = perp.scale(Math.cos(a) * r).add(perp2.scale(Math.sin(a) * r));
				Vec3 q = center.add(off);
				level.sendParticles(ModParticles.UNIBEAM_SPARK,
						q.x, q.y, q.z, 1,
						dir.x * 0.4, dir.y * 0.4, dir.z * 0.4, 0.05);
			}
		}
		for (int i = 0; i < 20; i++) {
			double f = rand.nextDouble();
			Vec3 q = origin.add(dir.scale(f * BEAM_RANGE));
			level.sendParticles(ParticleTypes.FLAME,
					q.x, q.y, q.z, 2, 0.3, 0.3, 0.3, 0.02);
			level.sendParticles(ParticleTypes.LAVA,
					q.x, q.y, q.z, 1, 0.2, 0.2, 0.2, 0.0);
		}
		Vec3 end = origin.add(dir.scale(BEAM_RANGE));
		if (progress % 4 == 0) {
			level.sendParticles(ParticleTypes.EXPLOSION,
					end.x, end.y, end.z, 2, 0.6, 0.6, 0.6, 0.0);
		}
	}

	private static Vec3 perpendicular(Vec3 dir) {
		Vec3 up = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		return dir.cross(up).normalize();
	}

	private static void damageEntitiesInBeam(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir) {
		Vec3 end = origin.add(dir.scale(BEAM_RANGE));
		AABB box = new AABB(origin, end).inflate(BEAM_RADIUS + 0.5);
		List<LivingEntity> ents = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator());
		for (LivingEntity e : ents) {
			Vec3 toEnt = e.getBoundingBox().getCenter().subtract(origin);
			double along = toEnt.dot(dir);
			if (along < 0 || along > BEAM_RANGE) {
				continue;
			}
			Vec3 closest = origin.add(dir.scale(along));
			double radial = e.getBoundingBox().getCenter().distanceTo(closest);
			if (radial > BEAM_RADIUS + e.getBbWidth() * 0.5) {
				continue;
			}
			e.hurt(ModDamageTypes.unibeam(level, player), DIRECT_HIT_DAMAGE_PER_TICK);
			applyDebuffs(e, AOE_DEBUFFS, 80, 1, true);
		}
	}

	private static void carveTickAlongBeam(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 dir, int progress) {
		RandomSource rand = level.getRandom();
		int destroyOps = 28;
		for (int i = 0; i < destroyOps; i++) {
			double f = rand.nextDouble();
			Vec3 center = origin.add(dir.scale(f * BEAM_RANGE));
			double a = rand.nextDouble() * Math.PI * 2.0;
			double r = rand.nextDouble() * BEAM_RADIUS;
			Vec3 perp = perpendicular(dir);
			Vec3 perp2 = dir.cross(perp).normalize();
			Vec3 off = perp.scale(Math.cos(a) * r).add(perp2.scale(Math.sin(a) * r));
			BlockPos pos = BlockPos.containing(center.add(off));
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.liquid()) {
				continue;
			}
			float hardness = state.getDestroySpeed(level, pos);
			if (hardness < 0f || hardness >= 50f) {
				continue;
			}
			level.destroyBlock(pos, false, player);
		}
		if (progress % 8 == 0) {
			double f = 0.2 + rand.nextDouble() * 0.7;
			Vec3 center = origin.add(dir.scale(f * BEAM_RANGE));
			level.explode(player, center.x, center.y, center.z,
					3.0f, true, Level.ExplosionInteraction.MOB);
		}
	}

	private static void finalizeBlast(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir) {
		LivingEntity directHit = pickDirectHit(player, level, origin, dir);
		if (directHit != null) {
			directHit.hurt(ModDamageTypes.unibeam(level, player), 80f);
			applyDebuffs(directHit, AOE_DEBUFFS, 240, 2, true);
		}
		AABB aoeBox = player.getBoundingBox().inflate(DEBUFF_RADIUS);
		List<LivingEntity> aoeTargets = level.getEntitiesOfClass(LivingEntity.class, aoeBox,
				e -> e != player && e.isAlive() && !e.isSpectator() && e != directHit);
		for (LivingEntity target : aoeTargets) {
			applyDebuffs(target, AOE_DEBUFFS, 120, 0, false);
		}
		carveCrater(level, player, origin, dir);
		drainEnergy(player);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.5f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.4f, 1.4f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.UNIBEAM_BLAST, SoundSource.PLAYERS, 3.6f, 0.7f);
		Vec3 end = origin.add(dir.scale(BEAM_RANGE));
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
				end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
	}

	private static LivingEntity pickDirectHit(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir) {
		Vec3 end = origin.add(dir.scale(BEAM_RANGE));
		BlockHitResult bh = level.clip(new ClipContext(origin, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 effectiveEnd = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(BEAM_RANGE)).inflate(2.5);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, origin, effectiveEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		return hit != null ? (LivingEntity) hit.getEntity() : null;
	}

	private static void applyDebuffs(LivingEntity target, Holder<MobEffect>[] effects, int baseDuration, int baseAmplifier, boolean direct) {
		int duration = direct ? baseDuration * 2 : baseDuration;
		int amplifier = direct ? Math.min(baseAmplifier * 2 + 1, 4) : baseAmplifier;
		for (Holder<MobEffect> effect : effects) {
			target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
		}
	}

	private static void carveCrater(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 dir) {
		RandomSource rand = level.getRandom();
		Vec3 forward = dir.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : dir.normalize();
		for (int step = 1; step <= CRATER_LENGTH; step++) {
			Vec3 center = origin.add(forward.scale(step));
			BlockPos centerPos = BlockPos.containing(center);
			float spread = 0.3f + (step / (float) CRATER_LENGTH) * 0.7f;
			int rays = (int) (CRATER_HALF_WIDTH * 2 * spread + 4);
			for (int i = 0; i < rays * 6; i++) {
				int dx = rand.nextInt(CRATER_HALF_WIDTH * 2 + 1) - CRATER_HALF_WIDTH;
				int dy = rand.nextInt(CRATER_HALF_WIDTH * 2 + 1) - CRATER_HALF_WIDTH;
				int dz = rand.nextInt(CRATER_HALF_WIDTH * 2 + 1) - CRATER_HALF_WIDTH;
				int distSq = dx * dx + dy * dy + dz * dz;
				int maxDistSq = CRATER_HALF_WIDTH * CRATER_HALF_WIDTH;
				if (distSq > maxDistSq) {
					continue;
				}
				if (rand.nextFloat() < 0.35f * (distSq / (float) maxDistSq)) {
					continue;
				}
				BlockPos pos = centerPos.offset(dx, dy, dz);
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || state.liquid()) {
					continue;
				}
				float hardness = state.getDestroySpeed(level, pos);
				if (hardness < 0f || hardness >= 50f) {
					continue;
				}
				level.destroyBlock(pos, false, player);
			}
			if (step % 3 == 0) {
				level.explode(player, center.x, center.y, center.z,
						3.0f + spread * 2.0f, true, Level.ExplosionInteraction.MOB);
			}
			placeFireRing(level, center, 2 + (int) (spread * 2));
			level.sendParticles(ParticleTypes.LAVA,
					center.x, center.y, center.z, 6, 1.0, 0.6, 1.0, 0.0);
			level.sendParticles(ParticleTypes.FLAME,
					center.x, center.y, center.z, 16, 1.4, 0.8, 1.4, 0.05);
		}
	}

	private static void placeFireRing(ServerLevel level, Vec3 center, int radius) {
		BlockPos centerPos = BlockPos.containing(center);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				for (int dy = -1; dy <= 2; dy++) {
					BlockPos pos = centerPos.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (!state.isAir()) {
						continue;
					}
					BlockPos below = pos.below();
					if (BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)
							&& !level.getBlockState(below).isAir()
							&& level.getRandom().nextFloat() < 0.55f) {
						level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
					}
				}
			}
		}
	}

	private static void drainEnergy(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		HeroData updated = data.withResources(0f, 0f);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncResources(player, updated);
	}

	@SafeVarargs
	private static Holder<MobEffect>[] effects(Holder<MobEffect>... entries) {
		return entries;
	}

	private static final class ChargeState {
		final Vec3 anchor;
		int progress;

		ChargeState(Vec3 anchor) {
			this.anchor = anchor;
			this.progress = 0;
		}
	}

	private static final class FireState {
		final Vec3 anchor;
		final Vec3 origin;
		final Vec3 dir;
		int progress;

		FireState(Vec3 anchor, Vec3 origin, Vec3 dir) {
			this.anchor = anchor;
			this.origin = origin;
			this.dir = dir;
			this.progress = 0;
		}
	}

	private static final class StunState {
		final Vec3 anchor;
		int progress;

		StunState(Vec3 anchor) {
			this.anchor = anchor;
			this.progress = 0;
		}
	}
}
