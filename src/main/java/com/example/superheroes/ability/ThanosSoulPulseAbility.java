package com.example.superheroes.ability;

import com.example.superheroes.effect.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ThanosSoulPulseAbility implements Ability {
	private static final int COOLDOWN_TICKS = 400;
	private static final double RADIUS = 10.0;
	private static final float DAMAGE = 8.0f;
	private static final float KNOCKBACK_STRENGTH = 2.4f;
	private static final int LOCK_DURATION_TICKS = 80;
	private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.62f, 0.25f), 1.6f);
	private static final DustParticleOptions CYAN_DUST = new DustParticleOptions(new Vector3f(0.45f, 0.85f, 1.0f), 1.4f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.THANOS_SOUL_PULSE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 180f;
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
		Vec3 center = player.position().add(0, 1.0, 0);

		AABB aoe = new AABB(
				center.x - RADIUS, center.y - 4, center.z - RADIUS,
				center.x + RADIUS, center.y + 6, center.z + RADIUS);

		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, aoe,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			double dist = le.position().distanceTo(player.position());
			if (dist > RADIUS) continue;

			le.hurt(level.damageSources().magic(), DAMAGE);

			Vec3 push = le.position().subtract(player.position());
			double horiz = Math.max(0.01, Math.sqrt(push.x * push.x + push.z * push.z));
			double scale = KNOCKBACK_STRENGTH * (1.0 - Math.min(0.7, dist / RADIUS));
			le.setDeltaMovement(push.x / horiz * scale, 0.55, push.z / horiz * scale);
			le.hurtMarked = true;

			le.addEffect(new MobEffectInstance(ModEffects.DISABLED_ABILITIES, LOCK_DURATION_TICKS, 0, false, true, true));

			level.sendParticles(ORANGE_DUST,
					le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(), 30, 0.4, 0.5, 0.4, 0.0);
			level.sendParticles(CYAN_DUST,
					le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(), 30, 0.4, 0.5, 0.4, 0.0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(), 24, 0.35, 0.5, 0.35, 0.4);
		}

		spawnRingFx(level, center);

		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.4f, 1.6f);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5f, 0.7f);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.6f, 0.6f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void spawnRingFx(ServerLevel level, Vec3 center) {
		for (int ring = 1; ring <= 3; ring++) {
			double r = (RADIUS / 3.0) * ring;
			int count = 32 + ring * 16;
			for (int i = 0; i < count; i++) {
				double angle = (Math.PI * 2 * i) / count;
				double px = center.x + Math.cos(angle) * r;
				double pz = center.z + Math.sin(angle) * r;
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						px, center.y - 0.5, pz, 2, 0.05, 0.4, 0.05, 0.3);
				level.sendParticles(ORANGE_DUST,
						px, center.y, pz, 2, 0.1, 0.3, 0.1, 0.0);
				if (ring == 3 && i % 4 == 0) {
					level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
							px, center.y - 0.3, pz, 1, 0.05, 0.05, 0.05, 0.02);
				}
			}
		}
		level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 4, 0.6, 0.4, 0.6, 0.0);
		level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.5, center.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(com.example.superheroes.particle.ModParticles.SOUL_SPARK,
				center.x, center.y, center.z, 80, RADIUS * 0.5, 1.2, RADIUS * 0.5, 0.1);
	}
}
