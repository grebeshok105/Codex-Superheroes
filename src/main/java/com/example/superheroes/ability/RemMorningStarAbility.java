package com.example.superheroes.ability;

import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.RemDemonismController;
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

public final class RemMorningStarAbility implements Ability {
	private static final int COOLDOWN_TICKS = 8 * 20;
	private static final double RANGE = 18.0;
	private static final double CONE_DOT = 0.82;
	private static final float DAMAGE = 12.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_MORNING_STAR;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 45f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return RemDemonismController.isActive(player) && !AbilityCooldowns.isOnCooldown(player, getId());
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
				new AABB(eye, eye.add(forward.scale(RANGE))).inflate(3.0), target -> isValidTarget(player, target))) {
			Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = center.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;
			double dot = toTarget.scale(1.0 / distance).dot(forward);
			if (dot < CONE_DOT) continue;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = target;
			}
		}
		if (best == null) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					eye.x + forward.x * 1.2, eye.y - 0.25, eye.z + forward.z * 1.2, 8, 0.2, 0.16, 0.2, 0.03);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.7f, 1.25f);
			return false;
		}
		Vec3 center = best.position().add(0.0, best.getBbHeight() * 0.5, 0.0);
		best.invulnerableTime = 0;
		best.hurt(level.damageSources().playerAttack(player), DAMAGE);
		best.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 2, true, true, true));
		best.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 10 * 20, 0, false, true, true));
		RemDemonismController.startMorningStarPull(player, best);
		for (int i = 0; i <= 14; i++) {
			double t = i / 14.0;
			Vec3 point = eye.lerp(center, t);
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 2, 0.06, 0.06, 0.06, 0.02);
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, point.x, point.y, point.z, 1, 0.04, 0.04, 0.04, 0.01);
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.35, 0.2, 0.35, 0.0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				eye.x + forward.x * 1.2, eye.y - 0.25, eye.z + forward.z * 1.2, 12, 0.25, 0.2, 0.25, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 0.65f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.3f, 0.75f);
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
