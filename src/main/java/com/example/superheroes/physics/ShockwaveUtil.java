package com.example.superheroes.physics;

import com.example.superheroes.network.ScreenShakeS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ShockwaveUtil {
	private ShockwaveUtil() {
	}

	public static void detonateMob(LivingEntity source, ServerLevel world, Vec3 center, double radius, float damage, boolean breakBlocks) {
		detonateMob(source, world, center, radius, damage, breakBlocks, source.damageSources().mobAttack(source));
	}

	public static void detonateMob(LivingEntity source, ServerLevel world, Vec3 center, double radius, float damage, boolean breakBlocks, DamageSource damageSource) {
		AABB box = new AABB(
				center.x - radius, center.y - 1.0, center.z - radius,
				center.x + radius, center.y + 2.0, center.z + radius);
		List<Entity> hits = world.getEntities(source, box);
		double r2 = radius * radius;
		DamageSource ds = damageSource;
		for (Entity e : hits) {
			Vec3 to = e.position().subtract(center);
			double d2 = to.lengthSqr();
			if (d2 > r2 || d2 < 1.0e-4) {
				continue;
			}
			double falloff = 1.0 - Math.sqrt(d2) / radius;
			Vec3 knock = to.normalize().scale(1.5 * falloff).add(0.0, 0.5 * falloff, 0.0);
			e.push(knock.x, knock.y, knock.z);
			e.hurtMarked = true;
			if (e instanceof LivingEntity le && damage > 0f) {
				le.hurt(ds, damage * (float) falloff);
			}
		}
		if (breakBlocks) {
			BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
			int r = (int) Math.ceil(radius);
			int cx = (int) Math.floor(center.x);
			int cy = (int) Math.floor(center.y);
			int cz = (int) Math.floor(center.z);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					for (int dy = -1; dy <= 2; dy++) {
						if (dx * dx + dz * dz + dy * dy > r * r) {
							continue;
						}
						m.set(cx + dx, cy + dy, cz + dz);
						BlockState bs = world.getBlockState(m);
						if (bs.isAir()) {
							continue;
						}
						float hardness = bs.getDestroySpeed(world, m);
						if (hardness >= 0f && hardness < 0.6f) {
							world.destroyBlock(m.immutable(), true, source);
						}
					}
				}
			}
		}
		world.sendParticles(ParticleTypes.EXPLOSION,
				center.x, center.y + 0.2, center.z,
				12, radius * 0.4, 0.2, radius * 0.4, 0.0);
		world.sendParticles(ParticleTypes.LARGE_SMOKE,
				center.x, center.y + 0.1, center.z,
				40, radius * 0.5, 0.3, radius * 0.5, 0.05);
		world.sendParticles(ParticleTypes.POOF,
				center.x, center.y + 0.1, center.z,
				30, radius * 0.5, 0.2, radius * 0.5, 0.1);
		world.playSound(null, center.x, center.y, center.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.5f);
		world.playSound(null, center.x, center.y, center.z,
				SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.0f, 0.6f);
		float baseShake = (float) Math.min(2.5, 0.6 + radius / 4.0);
		int shakeT = (int) Math.min(40, 14 + radius * 2);
		for (ServerPlayer nearby : PlayerLookup.around(world, center, 32.0)) {
			double dist = nearby.position().distanceTo(center);
			float intensity = (float) Math.max(0.0, 1.0 - dist / 32.0) * baseShake;
			if (intensity > 0.05f) {
				ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, shakeT));
			}
		}
	}

	public static void detonate(ServerPlayer source, Vec3 center, double radius, float damage, boolean breakBlocks) {
		detonate(source, center, radius, damage, breakBlocks, source.damageSources().playerAttack(source));
	}

	public static void detonate(ServerPlayer source, Vec3 center, double radius, float damage, boolean breakBlocks, DamageSource damageSource) {
		ServerLevel world = source.serverLevel();
		AABB box = new AABB(
				center.x - radius, center.y - 1.0, center.z - radius,
				center.x + radius, center.y + 2.0, center.z + radius);
		List<Entity> hits = world.getEntities(source, box);
		double r2 = radius * radius;
		DamageSource ds = damageSource;
		for (Entity e : hits) {
			Vec3 to = e.position().subtract(center);
			double d2 = to.lengthSqr();
			if (d2 > r2 || d2 < 1.0e-4) {
				continue;
			}
			double falloff = 1.0 - Math.sqrt(d2) / radius;
			Vec3 knock = to.normalize().scale(1.5 * falloff).add(0.0, 0.5 * falloff, 0.0);
			e.push(knock.x, knock.y, knock.z);
			e.hurtMarked = true;
			if (e instanceof LivingEntity le && damage > 0f) {
				le.hurt(ds, damage * (float) falloff);
			}
		}
		if (breakBlocks) {
			BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
			int r = (int) Math.ceil(radius);
			int cx = (int) Math.floor(center.x);
			int cy = (int) Math.floor(center.y);
			int cz = (int) Math.floor(center.z);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					for (int dy = -1; dy <= 2; dy++) {
						if (dx * dx + dz * dz + dy * dy > r * r) {
							continue;
						}
						m.set(cx + dx, cy + dy, cz + dz);
						BlockState bs = world.getBlockState(m);
						if (bs.isAir()) {
							continue;
						}
						float hardness = bs.getDestroySpeed(world, m);
						if (hardness >= 0f && hardness < 0.6f) {
							world.destroyBlock(m.immutable(), true, source);
						}
					}
				}
			}
		}
		world.sendParticles(ParticleTypes.EXPLOSION,
				center.x, center.y + 0.2, center.z,
				12, radius * 0.4, 0.2, radius * 0.4, 0.0);
		world.sendParticles(ParticleTypes.LARGE_SMOKE,
				center.x, center.y + 0.1, center.z,
				40, radius * 0.5, 0.3, radius * 0.5, 0.05);
		world.sendParticles(ParticleTypes.POOF,
				center.x, center.y + 0.1, center.z,
				30, radius * 0.5, 0.2, radius * 0.5, 0.1);
		world.playSound(null, center.x, center.y, center.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.5f);
		world.playSound(null, center.x, center.y, center.z,
				SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.6f, 0.4f);
		world.playSound(null, center.x, center.y, center.z,
				SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.0f, 0.6f);
		float baseShake = (float) Math.min(2.5, 0.6 + radius / 4.0);
		int shakeT = (int) Math.min(40, 14 + radius * 2);
		for (ServerPlayer nearby : PlayerLookup.around(world, center, 32.0)) {
			double dist = nearby.position().distanceTo(center);
			float intensity = (float) Math.max(0.0, 1.0 - dist / 32.0) * baseShake;
			if (intensity > 0.05f) {
				ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, shakeT));
			}
		}
	}
}
