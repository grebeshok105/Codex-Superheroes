package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ThanosCosmicSlamAbility implements Ability {
	private static final int COOLDOWN_TICKS = 100;
	private static final double RANGE = 64.0;
	private static final double BEAM_RADIUS = 2.2;
	private static final float CORE_DAMAGE = 55.0f;
	private static final float EDGE_DAMAGE = 24.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.THANOS_COSMIC_SLAM;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 100f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 farEnd = origin.add(dir.scale(RANGE));

		BlockHitResult blockHit = level.clip(new ClipContext(
				origin, farEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 endPoint = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : farEnd;

		AABB swept = new AABB(origin, endPoint).inflate(BEAM_RADIUS + 1.0);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, swept,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			Vec3 closest = closestPointOnSegment(origin, endPoint, le.position().add(0, le.getBbHeight() * 0.5, 0));
			double dist = closest.distanceTo(le.position().add(0, le.getBbHeight() * 0.5, 0));
			if (dist > BEAM_RADIUS) continue;
			float falloff = (float) Math.max(0.0, 1.0 - dist / BEAM_RADIUS);
			float damage = EDGE_DAMAGE + (CORE_DAMAGE - EDGE_DAMAGE) * falloff;
			le.hurt(ModDamageTypes.thanosCosmicSlam(level, player), damage);
			Vec3 push = dir.scale(2.4).add(0, 0.45, 0);
			le.setDeltaMovement(push);
			le.hurtMarked = true;
		}

		ModNetworking.broadcastThanosCosmicBeam(player, origin, endPoint);

		double len = endPoint.subtract(origin).length();
		int steps = (int) Math.max(8, len * 1.5);
		for (int i = 1; i < steps; i++) {
			double t = i / (double) steps;
			Vec3 p = origin.add(endPoint.subtract(origin).scale(t));
			level.sendParticles(ModParticles.PURPLE_FLAME, p.x, p.y, p.z, 2, 0.4, 0.4, 0.4, 0.02);
			if (i % 4 == 0) {
				level.sendParticles(ModParticles.DARK_STAR, p.x, p.y, p.z, 2, 0.5, 0.5, 0.5, 0.04);
			}
		}
		level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 2, 0.2, 0.2, 0.2, 0.0);
		level.sendParticles(ParticleTypes.SONIC_BOOM, origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, endPoint.x, endPoint.y, endPoint.z, 4, 1.2, 1.2, 1.2, 0.0);
		level.sendParticles(ModParticles.PURPLE_FLAME, endPoint.x, endPoint.y, endPoint.z, 120, 1.5, 1.5, 1.5, 0.18);
		level.sendParticles(ModParticles.DARK_STAR, endPoint.x, endPoint.y, endPoint.z, 80, 1.5, 1.5, 1.5, 0.15);
		level.sendParticles(ModParticles.WHITE_BOOM, endPoint.x, endPoint.y, endPoint.z, 40, 1.5, 1.5, 1.5, 0.0);

		level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.4f, 0.5f);
		level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.4f);
		level.playSound(null, endPoint.x, endPoint.y, endPoint.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.6f, 0.5f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 p) {
		Vec3 ab = b.subtract(a);
		double lenSq = ab.lengthSqr();
		if (lenSq < 1e-6) return a;
		double t = p.subtract(a).dot(ab) / lenSq;
		t = Math.max(0.0, Math.min(1.0, t));
		return a.add(ab.scale(t));
	}
}
