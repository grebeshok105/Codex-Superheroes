package com.example.superheroes.ability;

import com.example.superheroes.effect.OmnimanMomentumController;
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

public final class OmnimanWorldBreakerAbility implements Ability {
	private static final int COOLDOWN_TICKS = 150;
	private static final int DEBILITATE_TICKS = 100;
	private static final double TARGET_RANGE = 9.5;
	private static final double SHOCKWAVE_RANGE = 7.0;
	private static final double MAX_CONE_RADIUS = 2.15;
	private static final float TARGET_DAMAGE = 36.0f;
	private static final float SHOCKWAVE_DAMAGE = 16.0f;
	private static final double TARGET_KNOCKBACK = 2.9;
	private static final double TARGET_UPWARD_KNOCKBACK = 1.15;
	private static final double SHOCKWAVE_KNOCKBACK = 1.35;
	private static final double SHOCKWAVE_UPWARD_KNOCKBACK = 0.55;
	private static final float MOMENTUM_COST = 35f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.OMNIMAN_WORLD_BREAKER;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 85f;
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
		Vec3 direction = player.getViewVector(1f).normalize();
		float boost = momentumBoost(player);
		LivingEntity target = findTarget(player, TARGET_RANGE);

		if (target != null) {
			hitPrimaryTarget(player, target, direction, boost);
			playTargetEffects(level, player, target, direction, boost);
		} else {
			emitFrontalShockwave(player, direction, boost);
			playShockwaveEffects(level, eye, direction, boost);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.35f, 0.55f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f + boost * 0.25f, 0.65f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void hitPrimaryTarget(ServerPlayer player, LivingEntity target, Vec3 direction, float boost) {
		float damage = TARGET_DAMAGE + boost * 4.0f;
		double knockback = TARGET_KNOCKBACK + boost * 0.35;
		double upward = TARGET_UPWARD_KNOCKBACK + boost * 0.12;

		target.hurt(player.damageSources().playerAttack(player), damage);
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBILITATE_TICKS, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBILITATE_TICKS, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 35, 0, true, false, false));
		push(target, direction, knockback, upward);
	}

	private static void emitFrontalShockwave(ServerPlayer player, Vec3 direction, float boost) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		AABB scan = new AABB(eye, eye.add(direction.scale(SHOCKWAVE_RANGE))).inflate(MAX_CONE_RADIUS + 0.8, 1.2, MAX_CONE_RADIUS + 0.8);
		float damage = SHOCKWAVE_DAMAGE + boost * 2.5f;
		double knockback = SHOCKWAVE_KNOCKBACK + boost * 0.22;
		double upward = SHOCKWAVE_UPWARD_KNOCKBACK + boost * 0.08;

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, scan, e -> validTarget(player, e))) {
			Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
			double along = center.subtract(eye).dot(direction);
			if (along < 0.5 || along > SHOCKWAVE_RANGE) continue;
			if (!insideCone(eye, direction, center, along)) continue;

			entity.hurt(player.damageSources().playerAttack(player), damage);
			entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true, true));
			entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, true, true));
			push(entity, direction, knockback, upward);
		}
	}

	private static LivingEntity findTarget(ServerPlayer player, double range) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 direction = player.getViewVector(1f).normalize();
		AABB scan = new AABB(eye, eye.add(direction.scale(range))).inflate(MAX_CONE_RADIUS + 0.7, 1.3, MAX_CONE_RADIUS + 0.7);
		LivingEntity closest = null;
		double closestAlong = range + 1.0;

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, scan, e -> validTarget(player, e))) {
			Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.55, 0);
			Vec3 toEntity = center.subtract(eye);
			double along = toEntity.dot(direction);
			if (along < 0.35 || along > range) continue;
			if (!insideCone(eye, direction, center, along)) continue;
			if (along < closestAlong) {
				closestAlong = along;
				closest = entity;
			}
		}

		return closest;
	}

	private static boolean insideCone(Vec3 eye, Vec3 direction, Vec3 center, double along) {
		Vec3 perpendicular = center.subtract(eye).subtract(direction.scale(along));
		double allowedRadius = Math.min(MAX_CONE_RADIUS, 0.65 + along * 0.24);
		return perpendicular.lengthSqr() <= allowedRadius * allowedRadius;
	}

	private static boolean validTarget(ServerPlayer player, LivingEntity entity) {
		return entity != player && entity.isAlive() && !entity.isSpectator()
				&& !(entity instanceof Player p && p.isCreative());
	}

	private static void push(LivingEntity target, Vec3 direction, double strength, double upward) {
		target.setDeltaMovement(direction.x * strength, upward, direction.z * strength);
		target.hurtMarked = true;
		if (target instanceof ServerPlayer targetPlayer) {
			targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
		}
	}

	private static void playTargetEffects(ServerLevel level, ServerPlayer player, LivingEntity target, Vec3 direction, float boost) {
		Vec3 eye = player.getEyePosition();
		Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.55, 0);
		double distance = Math.max(0.001, targetCenter.subtract(eye).length());
		int steps = (int) Math.max(8.0, distance * 4.0);

		for (int i = 1; i <= steps; i++) {
			Vec3 point = eye.add(direction.scale(distance * i / steps));
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z,
					2, 0.07, 0.07, 0.07, 0.02);
		}

		level.sendParticles(ParticleTypes.EXPLOSION, targetCenter.x, targetCenter.y, targetCenter.z,
				2 + (int) boost, 0.25, 0.2, 0.25, 0.0);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetCenter.x, targetCenter.y, targetCenter.z,
				18, 0.28, 0.35, 0.28, 0.0);
		level.sendParticles(ParticleTypes.POOF, targetCenter.x, targetCenter.y, targetCenter.z,
				36 + (int) (boost * 6.0f), 0.45, 0.35, 0.45, 0.08);
		level.sendParticles(ParticleTypes.CLOUD, targetCenter.x - direction.x * 0.8, targetCenter.y, targetCenter.z - direction.z * 0.8,
				22, 0.35, 0.25, 0.35, 0.16);

		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.5f, 0.55f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.95f, 0.75f);
	}

	private static void playShockwaveEffects(ServerLevel level, Vec3 eye, Vec3 direction, float boost) {
		int steps = 9;
		for (int i = 1; i <= steps; i++) {
			double distance = SHOCKWAVE_RANGE * i / steps;
			Vec3 point = eye.add(direction.scale(distance));
			double spread = Math.min(MAX_CONE_RADIUS, 0.35 + distance * 0.24);
			level.sendParticles(ParticleTypes.CLOUD, point.x, point.y - 0.15, point.z,
					6, spread * 0.35, 0.12, spread * 0.35, 0.08);
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z,
					4, spread * 0.22, 0.12, spread * 0.22, 0.04);
		}
		level.sendParticles(ParticleTypes.FLASH, eye.x + direction.x * 1.2, eye.y, eye.z + direction.z * 1.2,
				1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.POOF, eye.x + direction.x * 2.5, eye.y - 0.15, eye.z + direction.z * 2.5,
				24 + (int) (boost * 4.0f), 0.7, 0.25, 0.7, 0.1);
	}

	private static float momentumBoost(ServerPlayer player) {
		return OmnimanMomentumController.consume(player, MOMENTUM_COST) ? 1.0f : 0.0f;
	}
}
