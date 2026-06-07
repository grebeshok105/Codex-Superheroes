package com.example.superheroes.ability;

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

import java.util.Set;

public final class ThanosSpacePortalAbility implements Ability {
	private static final int COOLDOWN_TICKS = 240;
	private static final double RANGE = 30.0;
	private static final double CONE_RADIUS_SQR = 16.0;
	private static final double LANDING_DISTANCE = 2.5;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.THANOS_SPACE_PORTAL;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 150f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (AbilityCooldowns.isOnCooldown(player, getId())) return false;
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		return pickTarget(player, player.serverLevel(), eye, dir) != null;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);

		LivingEntity target = pickTarget(player, level, eye, dir);
		if (target == null) {
			return false;
		}

		Vec3 originPos = target.position();

		Vec3 landing = player.position().add(dir.x * LANDING_DISTANCE, 0.0, dir.z * LANDING_DISTANCE);
		float yaw = (player.getYRot() + 180f) % 360f;
		float pitch = 0f;

		spawnPortalRing(level, originPos);

		if (target instanceof ServerPlayer sp) {
			sp.connection.teleport(landing.x, landing.y, landing.z, yaw, pitch, Set.of());
		} else {
			target.teleportTo(landing.x, landing.y, landing.z);
			target.setYRot(yaw);
			target.setYHeadRot(yaw);
		}

		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 1, true, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10, 0, true, true, true));

		spawnPortalRing(level, target.position());

		level.playSound(null, originPos.x, originPos.y, originPos.z,
				SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4f, 1.2f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6f, 1.6f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity pickTarget(ServerPlayer self, ServerLevel level, Vec3 eye, Vec3 dir) {
		Vec3 end = eye.add(dir.scale(RANGE));
		AABB scan = new AABB(eye, end).inflate(4.0);
		LivingEntity closest = null;
		double closestAlong = RANGE + 1.0;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> e != self && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(self.getUUID())))) {
			Vec3 toEntity = le.position().add(0, le.getBbHeight() / 2, 0).subtract(eye);
			double along = toEntity.dot(dir);
			if (along < 0.5 || along > RANGE) continue;
			Vec3 perp = toEntity.subtract(dir.scale(along));
			if (perp.lengthSqr() > CONE_RADIUS_SQR) continue;
			if (along < closestAlong) {
				closestAlong = along;
				closest = le;
			}
		}
		return closest;
	}

	private static void spawnPortalRing(ServerLevel level, Vec3 pos) {
		level.sendParticles(ParticleTypes.PORTAL,
				pos.x, pos.y + 1.0, pos.z, 180, 0.6, 1.2, 0.6, 0.8);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL,
				pos.x, pos.y + 1.0, pos.z, 140, 0.5, 1.0, 0.5, 0.4);
		level.sendParticles(ParticleTypes.DRAGON_BREATH,
				pos.x, pos.y + 0.8, pos.z, 60, 0.5, 0.8, 0.5, 0.06);
		level.sendParticles(ParticleTypes.END_ROD,
				pos.x, pos.y + 1.0, pos.z, 24, 0.4, 0.8, 0.4, 0.05);
		level.sendParticles(com.example.superheroes.particle.ModParticles.PURPLE_FLAME,
				pos.x, pos.y + 1.0, pos.z, 80, 0.5, 1.0, 0.5, 0.1);
		level.sendParticles(com.example.superheroes.particle.ModParticles.SOUL_SPARK,
				pos.x, pos.y + 1.0, pos.z, 40, 0.4, 0.9, 0.4, 0.06);
	}
}
