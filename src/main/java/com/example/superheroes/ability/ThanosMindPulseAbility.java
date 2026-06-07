package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ThanosMindPulseAbility implements Ability {
	private static final int COOLDOWN_TICKS = 80;
	private static final double RANGE = 36.0;
	private static final double CONE_DOT = 0.55;
	private static final DustParticleOptions YELLOW_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.92f, 0.2f), 1.6f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.THANOS_MIND_PULSE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 70f;
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
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));

		AABB scan = new AABB(eye, end).inflate(2.0);
		int hits = 0;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			Vec3 toEntity = le.position().add(0, le.getBbHeight() / 2, 0).subtract(eye);
			double len = toEntity.length();
			if (len < 0.001) continue;
			double dot = toEntity.scale(1.0 / len).dot(dir);
			if (dot < CONE_DOT) continue;
			if (len > RANGE) continue;

			le.hurt(ModDamageTypes.thanosMindPulse(level, player), 22.0f);
			le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, true, true, true));
			le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, true, true, true));
			le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, true, true, true));
			le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, true, true));
			level.sendParticles(YELLOW_DUST,
					le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(), 80, 0.6, 0.8, 0.6, 0.0);
			level.sendParticles(ParticleTypes.ENCHANTED_HIT,
					le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(), 30, 0.5, 0.5, 0.5, 0.4);
			hits++;
		}

		int steps = (int) Math.max(20, RANGE * 3);
		for (int i = 0; i < steps; i++) {
			double t = (double) i / steps;
			Vec3 p = eye.add(dir.scale(t * RANGE));
			level.sendParticles(YELLOW_DUST, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.0);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		level.sendParticles(ParticleTypes.FLASH, eye.x + dir.x, eye.y + dir.y, eye.z + dir.z, 2, 0.2, 0.2, 0.2, 0.0);
		for (int i = 0; i < steps; i += 2) {
			double t = (double) i / steps;
			Vec3 p = eye.add(dir.scale(t * RANGE));
			level.sendParticles(com.example.superheroes.particle.ModParticles.PURPLE_FLAME,
					p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
			if (i % 4 == 0) {
				level.sendParticles(com.example.superheroes.particle.ModParticles.DARK_STAR,
						p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
			if (i % 3 == 0) {
				level.sendParticles(com.example.superheroes.particle.ModParticles.DAZZLING,
						p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.0);
			}
			if (i % 5 == 0) {
				level.sendParticles(com.example.superheroes.particle.ModParticles.SUN_PARTICLE,
						p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 1.5f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
