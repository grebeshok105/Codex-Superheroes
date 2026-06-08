package com.example.superheroes.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GuardiansBreakerAbility implements Ability {
	private static final int COOLDOWN_TICKS = 160;
	private static final int BREAK_DURATION_TICKS = 80;
	private static final double TARGET_RANGE = 20.0;
	private static final double DASH_TICKS = 5.0;
	private static final double MIN_DASH_SPEED = 2.4;
	private static final double MAX_DASH_SPEED = 4.8;
	private static final double PATH_RADIUS = 1.4;
	private static final double IMPACT_RADIUS = 3.2;
	private static final int MAX_BLOCKS_DESTROYED = 120;
	private static final float HARDNESS_LIMIT = 20.0f;
	private static final float DAMAGE = 24.0f;
	private static final double TARGET_KNOCKBACK = 4.2;
	private static final double TARGET_UPWARD_KNOCKBACK = 0.85;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GUARDIANS_BREAKER;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 55f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId()) && findTarget(player) != null;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		LivingEntity target = findTarget(player);
		if (target == null) {
			return false;
		}

		ServerLevel level = player.serverLevel();
		Vec3 playerCenter = centerOf(player);
		Vec3 targetCenter = centerOf(target);
		Vec3 toTarget = targetCenter.subtract(playerCenter);
		double distance = toTarget.length();
		Vec3 direction = distance > 0.001 ? toTarget.scale(1.0 / distance) : safeViewDirection(player);

		int destroyed = breakFlightPath(level, player, playerCenter, direction, Math.min(distance, TARGET_RANGE));
		destroyed += breakSphere(level, player, targetCenter, IMPACT_RADIUS, playerCenter, MAX_BLOCKS_DESTROYED - destroyed);

		Vec3 motion = direction.scale(dashSpeed(distance));
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		target.hurt(player.damageSources().playerAttack(player), DAMAGE);
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BREAK_DURATION_TICKS, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BREAK_DURATION_TICKS, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, BREAK_DURATION_TICKS, 0, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
		target.setDeltaMovement(direction.x * TARGET_KNOCKBACK, TARGET_UPWARD_KNOCKBACK, direction.z * TARGET_KNOCKBACK);
		target.hurtMarked = true;
		if (target instanceof ServerPlayer targetPlayer) {
			targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
		}

		int steps = (int) Math.max(8.0, distance * 4.0);
		for (int i = 1; i <= steps; i++) {
			Vec3 point = playerCenter.add(direction.scale(distance * i / steps));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
					2, 0.06, 0.06, 0.06, 0.01);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z,
						3, 0.18, 0.18, 0.18, 0.06);
			}
		}

		level.sendParticles(ParticleTypes.EXPLOSION, targetCenter.x, targetCenter.y, targetCenter.z,
				2 + Math.min(3, destroyed / 32), 0.35, 0.35, 0.35, 0.0);
		level.sendParticles(ParticleTypes.CRIT, targetCenter.x, targetCenter.y, targetCenter.z,
				36, 0.45, 0.5, 0.45, 0.22);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetCenter.x, targetCenter.y, targetCenter.z,
				16, 0.3, 0.4, 0.3, 0.0);
		level.sendParticles(ParticleTypes.POOF, targetCenter.x, targetCenter.y, targetCenter.z,
				36, 0.55, 0.45, 0.55, 0.09);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.35f, 0.58f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.55f, 1.45f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.75f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.4f, 0.58f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity findTarget(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = centerOf(player);
		AABB scan = player.getBoundingBox().inflate(TARGET_RANGE);
		LivingEntity closest = null;
		double closestDistanceSqr = TARGET_RANGE * TARGET_RANGE;

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> validTarget(player, e))) {
			double distanceSqr = centerOf(entity).distanceToSqr(origin);
			if (distanceSqr <= closestDistanceSqr) {
				closestDistanceSqr = distanceSqr;
				closest = entity;
			}
		}

		return closest;
	}

	private static boolean validTarget(ServerPlayer player, LivingEntity entity) {
		return entity != player
				&& entity.isAlive()
				&& !entity.isSpectator()
				&& !(entity instanceof Player p && p.isCreative());
	}

	private static int breakFlightPath(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 direction, double distance) {
		int destroyed = 0;
		int steps = (int) Math.max(6.0, Math.ceil(distance * 2.0));
		for (int i = 1; i <= steps && destroyed < MAX_BLOCKS_DESTROYED; i++) {
			Vec3 center = origin.add(direction.scale(distance * i / steps));
			destroyed += breakSphere(level, player, center, PATH_RADIUS, origin, MAX_BLOCKS_DESTROYED - destroyed);
		}
		return destroyed;
	}

	private static int breakSphere(ServerLevel level, ServerPlayer player, Vec3 center, double radius, Vec3 origin, int limit) {
		if (limit <= 0) {
			return 0;
		}

		int destroyed = 0;
		int blockRadius = (int) Math.ceil(radius);
		double radiusSqr = radius * radius;
		BlockPos centerPos = BlockPos.containing(center);
		for (int dx = -blockRadius; dx <= blockRadius && destroyed < limit; dx++) {
			for (int dy = -blockRadius; dy <= blockRadius && destroyed < limit; dy++) {
				for (int dz = -blockRadius; dz <= blockRadius && destroyed < limit; dz++) {
					BlockPos pos = centerPos.offset(dx, dy, dz);
					Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
					if (blockCenter.distanceToSqr(center) > radiusSqr) {
						continue;
					}
					if (blockCenter.distanceToSqr(origin) > TARGET_RANGE * TARGET_RANGE) {
						continue;
					}
					BlockState state = level.getBlockState(pos);
					if (!canBreak(level, pos, state)) {
						continue;
					}
					if (level.destroyBlock(pos, false, player)) {
						destroyed++;
					}
				}
			}
		}
		return destroyed;
	}

	private static boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.liquid()) {
			return false;
		}
		float hardness = state.getDestroySpeed(level, pos);
		return hardness >= 0f && hardness < HARDNESS_LIMIT;
	}

	private static Vec3 centerOf(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
	}

	private static Vec3 safeViewDirection(ServerPlayer player) {
		Vec3 direction = player.getViewVector(1f).normalize();
		return direction.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 0.0, 1.0) : direction;
	}

	private static double dashSpeed(double distance) {
		return Math.max(MIN_DASH_SPEED, Math.min(MAX_DASH_SPEED, distance / DASH_TICKS));
	}
}
