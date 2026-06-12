package com.example.superheroes.ability;

import com.example.superheroes.effect.ScorpionController;
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

/**
 * "GET OVER HERE!" — kunai on a chain that skewers the closest enemy in the
 * crosshair cone and drags it to Scorpion (Rem morning-star pull pattern).
 */
public final class ScorpionSpearAbility implements Ability {
	private static final int COOLDOWN_TICKS = 9 * 20;
	private static final double RANGE = 22.0;
	private static final double CONE_DOT = 0.80;
	private static final float DAMAGE = 8.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCORPION_SPEAR;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 25f;
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
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}

		LivingEntity best = null;
		double bestDistance = RANGE + 1.0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(eye, eye.add(forward.scale(RANGE))).inflate(3.0),
				target -> isValidTarget(player, target))) {
			Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = center.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) {
				continue;
			}
			if (toTarget.scale(1.0 / distance).dot(forward) < CONE_DOT) {
				continue;
			}
			if (distance < bestDistance) {
				bestDistance = distance;
				best = target;
			}
		}

		if (best == null) {
			level.sendParticles(ParticleTypes.FLAME,
					eye.x + forward.x * 1.2, eye.y - 0.25, eye.z + forward.z * 1.2,
					10, 0.2, 0.16, 0.2, 0.03);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.7f, 1.2f);
			return false;
		}

		Vec3 center = best.position().add(0.0, best.getBbHeight() * 0.5, 0.0);
		best.invulnerableTime = 0;
		best.hurt(level.damageSources().playerAttack(player), DAMAGE);
		best.igniteForSeconds(3f);
		best.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6 * 20, 2, true, true, true));
		ScorpionController.startSpearPull(player, best);

		for (int i = 0; i <= 16; i++) {
			Vec3 point = eye.lerp(center, i / 16.0);
			level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, 0.06, 0.06, 0.06, 0.02);
			if (i % 4 == 0) {
				level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.01);
			}
		}
		level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 4, 0.3, 0.3, 0.3, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.35, 0.2, 0.35, 0.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.playSound(null, best.getX(), best.getY(), best.getZ(),
				SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.3f, 0.7f);
		level.playSound(null, best.getX(), best.getY(), best.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 0.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}
}
