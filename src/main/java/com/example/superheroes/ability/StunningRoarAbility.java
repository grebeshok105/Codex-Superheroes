package com.example.superheroes.ability;

import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class StunningRoarAbility implements Ability {
	private static final double RADIUS = 12.0;
	private static final float DAMAGE = 14.0f;
	private static final int DARKNESS_DURATION = 80;
	private static final int COOLDOWN_TICKS = 160;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.STUNNING_ROAR;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 30f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		Vec3 mouth = player.getEyePosition().add(player.getViewVector(1f).scale(0.6));

		AABB box = new AABB(origin, origin).inflate(RADIUS);
		List<Entity> hits = level.getEntities(player, box,
				e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity);
		for (Entity e : hits) {
			double d = e.position().distanceTo(origin);
			if (d > RADIUS) continue;
			e.hurt(level.damageSources().playerAttack(player), DAMAGE);
			Vec3 push = e.position().subtract(origin).normalize().scale(0.6);
			e.push(push.x, 0.25, push.z);
			e.hurtMarked = true;
			if (e instanceof LivingEntity le) {
				le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_DURATION, 0, false, false, true));
			}
		}

		level.sendParticles(ParticleTypes.SONIC_BOOM,
				mouth.x, mouth.y, mouth.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION,
				mouth.x, mouth.y, mouth.z, 3, 0.5, 0.3, 0.5, 0.0);
		for (int i = 0; i < 36; i++) {
			double a = (i / 36.0) * Math.PI * 2.0;
			double rx = Math.cos(a) * RADIUS;
			double rz = Math.sin(a) * RADIUS;
			level.sendParticles(ParticleTypes.LARGE_SMOKE,
					origin.x + rx, origin.y + 0.6, origin.z + rz, 2, 0.1, 0.2, 0.1, 0.04);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.HOMELANDER_ROAR, SoundSource.PLAYERS, 1.6f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.HOMELANDER_ROAR_DEEP, SoundSource.PLAYERS, 1.0f, 1.0f);

		for (ServerPlayer nearby : PlayerLookup.around(level, origin, 24.0)) {
			double dist = nearby.position().distanceTo(origin);
			if (dist > RADIUS + 4.0) continue;
			float intensity = (float) Math.max(0.0, 1.0 - dist / (RADIUS + 4.0)) * 2.0f;
			if (intensity > 0.05f) {
				ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, 24));
			}
		}

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.STUNNING_ROAR, COOLDOWN_TICKS);
		return true;
	}
}
