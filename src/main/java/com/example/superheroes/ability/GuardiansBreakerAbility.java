package com.example.superheroes.ability;

import com.example.superheroes.network.ScreenShakeS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
	private static final int BREAK_DURATION_TICKS = 100;
	private static final double TARGET_RANGE = 20.0;
	private static final double DASH_TICKS = 3.0;
	private static final double MIN_DASH_SPEED = 4.4;
	private static final double MAX_DASH_SPEED = 7.6;
	private static final double PATH_RADIUS = 1.7;
	private static final double IMPACT_RADIUS = 12.0;
	private static final double CRATER_RADIUS = 3.6;
	private static final int MAX_BLOCKS_DESTROYED = 600;
	private static final float HARDNESS_LIMIT = 60.0f;
	private static final float DAMAGE = 32.0f;
	private static final float SWEEP_DAMAGE = 18.0f;
	private static final double TARGET_KNOCKBACK = 8.8;
	private static final double TARGET_UPWARD_KNOCKBACK = 1.35;
	private static final double SWEEP_KNOCKBACK = 5.8;
	private static final double SWEEP_UPWARD_KNOCKBACK = 0.9;

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
		destroyed += breakSphere(level, player, targetCenter, CRATER_RADIUS, playerCenter, 0.0,
				MAX_BLOCKS_DESTROYED - destroyed);

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
		int swept = sweepImpactTargets(player, target, targetCenter, direction);

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
				4 + Math.min(6, destroyed / 60), 1.2, 1.0, 1.2, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetCenter.x, targetCenter.y, targetCenter.z,
				1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.CRIT, targetCenter.x, targetCenter.y, targetCenter.z,
				72 + swept * 4, 1.35, 1.0, 1.35, 0.28);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetCenter.x, targetCenter.y, targetCenter.z,
				28 + swept * 2, 0.8, 0.75, 0.8, 0.0);
		level.sendParticles(ParticleTypes.POOF, targetCenter.x, targetCenter.y, targetCenter.z,
				60, 2.2, 1.2, 2.2, 0.16);
		level.sendParticles(ParticleTypes.CLOUD, targetCenter.x, targetCenter.y, targetCenter.z,
				80, 3.0, 1.3, 3.0, 0.16);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.8f, 0.48f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.05f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.55f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.8f, 0.42f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.45f, 0.38f);
		shake(level, targetCenter);

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
			destroyed += breakSphere(level, player, center, PATH_RADIUS, origin, TARGET_RANGE + PATH_RADIUS,
					MAX_BLOCKS_DESTROYED - destroyed);
		}
		return destroyed;
	}

	private static int breakSphere(ServerLevel level, ServerPlayer player, Vec3 center, double radius, Vec3 origin,
			double maxDistanceFromOrigin, int limit) {
		if (limit <= 0) {
			return 0;
		}

		int destroyed = 0;
		int blockRadius = (int) Math.ceil(radius);
		double radiusSqr = radius * radius;
		double originLimitSqr = maxDistanceFromOrigin * maxDistanceFromOrigin;
		BlockPos centerPos = BlockPos.containing(center);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int shell = 0; shell <= blockRadius && destroyed < limit; shell++) {
			for (int dx = -shell; dx <= shell && destroyed < limit; dx++) {
				for (int dy = -shell; dy <= shell && destroyed < limit; dy++) {
					for (int dz = -shell; dz <= shell && destroyed < limit; dz++) {
						if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) != shell) {
							continue;
						}
						int x = centerPos.getX() + dx;
						int y = centerPos.getY() + dy;
						int z = centerPos.getZ() + dz;
						double bx = x + 0.5;
						double by = y + 0.5;
						double bz = z + 0.5;
						double cx = bx - center.x;
						double cy = by - center.y;
						double cz = bz - center.z;
						if (cx * cx + cy * cy + cz * cz > radiusSqr) {
							continue;
						}
						if (maxDistanceFromOrigin > 0.0) {
							double ox = bx - origin.x;
							double oy = by - origin.y;
							double oz = bz - origin.z;
							if (ox * ox + oy * oy + oz * oz > originLimitSqr) {
								continue;
							}
						}
						pos.set(x, y, z);
						BlockState state = level.getBlockState(pos);
						if (!canBreak(level, pos, state)) {
							continue;
						}
						if (level.destroyBlock(pos.immutable(), false, player)) {
							destroyed++;
						}
					}
				}
			}
		}
		return destroyed;
	}

	private static int sweepImpactTargets(ServerPlayer player, LivingEntity primary, Vec3 impact, Vec3 direction) {
		ServerLevel level = player.serverLevel();
		AABB scan = new AABB(impact, impact).inflate(IMPACT_RADIUS);
		int swept = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
				entity -> validTarget(player, entity) && entity != primary)) {
			Vec3 center = centerOf(target);
			double distanceSqr = center.distanceToSqr(impact);
			if (distanceSqr > IMPACT_RADIUS * IMPACT_RADIUS) {
				continue;
			}
			double distance = Math.sqrt(distanceSqr);
			double falloff = 1.0 - Math.min(distance / IMPACT_RADIUS, 1.0);
			Vec3 away = center.subtract(impact);
			if (away.lengthSqr() < 1.0e-4) {
				away = direction;
			} else {
				away = away.normalize();
			}
			float damage = (float) (SWEEP_DAMAGE * (0.35 + falloff * 0.65));
			double knockback = SWEEP_KNOCKBACK * (0.35 + falloff * 0.65);
			target.hurt(player.damageSources().playerAttack(player), damage);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, true, true));
			target.setDeltaMovement(away.x * knockback + direction.x * 1.1,
					SWEEP_UPWARD_KNOCKBACK + falloff * 0.45,
					away.z * knockback + direction.z * 1.1);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			swept++;
		}
		return swept;
	}

	private static void shake(ServerLevel level, Vec3 center) {
		for (ServerPlayer nearby : PlayerLookup.around(level, center, 48.0)) {
			double distance = nearby.position().distanceTo(center);
			float intensity = (float) Math.max(0.08, 1.0 - distance / 48.0) * 2.2f;
			ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, 28));
		}
	}

	private static boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || !state.getFluidState().isEmpty()) {
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
