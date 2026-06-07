package com.example.superheroes.ability;

import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class HandClapAbility implements Ability {
	private static final double RANGE = 18.0;
	private static final double CONE_HALF_ANGLE_COS = Math.cos(Math.toRadians(45.0));
	private static final float DAMAGE = 20.0f;
	private static final double KNOCKBACK = 3.5;
	private static final int COOLDOWN_TICKS = 240;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.HAND_CLAP;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 50f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();

		AABB area = new AABB(origin, origin).inflate(RANGE);
		List<Entity> hits = level.getEntities(player, area,
				e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity);
		for (Entity entity : hits) {
			Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = toTarget.length();
			if (dist > RANGE || dist < 0.001) continue;
			Vec3 norm = toTarget.normalize();
			if (norm.dot(forward) < CONE_HALF_ANGLE_COS) continue;
			entity.hurt(level.damageSources().playerAttack(player), DAMAGE);
			Vec3 push = forward.scale(KNOCKBACK);
			entity.push(push.x, 0.6, push.z);
			entity.hurtMarked = true;
		}

		Vec3 tip = origin.add(forward.scale(2.5));
		level.sendParticles(ParticleTypes.EXPLOSION,
				tip.x, tip.y, tip.z, 2, 0.4, 0.4, 0.4, 0.0);
		for (int i = 1; i <= 12; i++) {
			double t = i;
			double spread = 0.4 + i * 0.25;
			Vec3 c = origin.add(forward.scale(t * 1.4));
			level.sendParticles(ParticleTypes.CLOUD,
					c.x, c.y, c.z, 8, spread, 0.5, spread, 0.05);
			level.sendParticles(ParticleTypes.LARGE_SMOKE,
					c.x, c.y, c.z, 4, spread * 0.5, 0.3, spread * 0.5, 0.03);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.HOMELANDER_HAND_CLAP, SoundSource.PLAYERS, 2.0f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.6f, 1.1f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.7f);

		for (ServerPlayer nearby : PlayerLookup.around(level, player.position(), 24.0)) {
			double dist = nearby.position().distanceTo(player.position());
			float intensity = (float) Math.max(0.0, 1.0 - dist / 24.0) * 1.8f;
			if (intensity > 0.05f) {
				ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, 16));
			}
		}

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.HAND_CLAP, COOLDOWN_TICKS);
		return true;
	}
}
