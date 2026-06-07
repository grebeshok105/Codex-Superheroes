package com.example.superheroes.ability;

import com.example.superheroes.effect.ReinhardController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ReinhardSpeedJudgmentAbility implements Ability {
	private static final double RADIUS = 36.0;
	private static final float BASE_DAMAGE = 28.0f;
	private static final float SECOND_COMING_BASE_DAMAGE = 160.0f;
	private static final int COOLDOWN_TICKS = 8 * 20;
	private static final int DEBUFF_TICKS = 5 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_SPEED_JUDGMENT;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 300f;
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
		LivingEntity target = findFastestTarget(player, level);
		if (target == null) {
			player.displayClientMessage(Component.translatable("ability.superheroes.reinhard_speed_judgment.no_target"), true);
			return false;
		}

		double speedScore = speedScore(target);
		boolean secondComing = ReinhardController.isInSecondComing(player);
		float damage = secondComing
				? (float) (SECOND_COMING_BASE_DAMAGE + Math.min(220.0, speedScore * 14.0))
				: (float) (BASE_DAMAGE + Math.min(72.0, speedScore * 6.0));

		target.invulnerableTime = 0;
		target.hurt(level.damageSources().playerAttack(player), damage);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_TICKS, 4, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_TICKS, 2, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, DEBUFF_TICKS, 0, true, false, false));
		target.setDeltaMovement(target.getDeltaMovement().multiply(0.08, 0.25, 0.08));
		target.hurtMarked = true;

		Vec3 start = player.getEyePosition();
		Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
		Vec3 line = end.subtract(start);
		int steps = Math.max(8, (int) (line.length() * 2.0));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = start.add(line.scale(i / (double) steps));
			level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.02);
			}
		}
		level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 2, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 48, 0.5, 0.7, 0.5, 0.10);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.8f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.4f, 1.5f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.6f, 0.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity findFastestTarget(ServerPlayer player, ServerLevel level) {
		AABB box = player.getBoundingBox().inflate(RADIUS);
		LivingEntity best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e != player && !e.isSpectator())) {
			double score = speedScore(candidate);
			if (score > bestScore) {
				bestScore = score;
				best = candidate;
			}
		}
		return best;
	}

	private static double speedScore(LivingEntity entity) {
		Vec3 movement = entity.getDeltaMovement();
		double currentSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z) * 20.0;
		double attributeSpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * 20.0;
		return Math.max(currentSpeed, attributeSpeed);
	}
}
