package com.example.superheroes.ability;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GuardiansBreakerAbility implements Ability {
	private static final int COOLDOWN_TICKS = 160;
	private static final int BREAK_DURATION_TICKS = 100;
	private static final double RANGE = 5.5;
	private static final double CONE_RADIUS_SQR = 2.25;
	private static final float DAMAGE = 18.0f;
	private static final double VERTICAL_KNOCKBACK = 1.05;

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
		Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.55, 0);

		target.hurt(player.damageSources().playerAttack(player), DAMAGE);
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BREAK_DURATION_TICKS, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BREAK_DURATION_TICKS, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, BREAK_DURATION_TICKS, 0, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
		target.setDeltaMovement(target.getDeltaMovement().x * 0.35, VERTICAL_KNOCKBACK, target.getDeltaMovement().z * 0.35);
		target.hurtMarked = true;
		if (target instanceof ServerPlayer targetPlayer) {
			targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
		}

		Vec3 eye = player.getEyePosition();
		Vec3 dir = targetCenter.subtract(eye);
		double distance = Math.max(0.001, dir.length());
		Vec3 step = dir.scale(1.0 / distance);
		int steps = (int) Math.max(6.0, distance * 4.0);
		for (int i = 1; i <= steps; i++) {
			Vec3 point = eye.add(step.scale(distance * i / steps));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
					2, 0.06, 0.06, 0.06, 0.01);
		}

		level.sendParticles(ParticleTypes.CRIT, targetCenter.x, targetCenter.y, targetCenter.z,
				28, 0.35, 0.45, 0.35, 0.18);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetCenter.x, targetCenter.y, targetCenter.z,
				12, 0.25, 0.35, 0.25, 0.0);
		level.sendParticles(ParticleTypes.POOF, targetCenter.x, targetCenter.y, targetCenter.z,
				24, 0.4, 0.35, 0.4, 0.06);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.2f, 0.8f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.4f, 0.75f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9f, 0.55f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity findTarget(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f).normalize();
		Vec3 end = eye.add(dir.scale(RANGE));
		AABB scan = new AABB(eye, end).inflate(1.6);
		LivingEntity closest = null;
		double closestAlong = RANGE + 1.0;

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.isCreative()))) {
			Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
			Vec3 toEntity = center.subtract(eye);
			double along = toEntity.dot(dir);
			if (along < 0.4 || along > RANGE) continue;
			Vec3 perpendicular = toEntity.subtract(dir.scale(along));
			if (perpendicular.lengthSqr() > CONE_RADIUS_SQR) continue;
			if (along < closestAlong) {
				closestAlong = along;
				closest = entity;
			}
		}

		return closest;
	}
}
