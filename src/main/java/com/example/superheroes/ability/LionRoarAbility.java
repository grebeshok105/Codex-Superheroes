package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
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

public final class LionRoarAbility implements Ability {
	private static final double RANGE = 18.0;
	private static final double CONE_HALF_ANGLE_COS = Math.cos(Math.toRadians(45.0));
	private static final float DAMAGE = 14.0f;
	private static final double KNOCKBACK = 3.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.LION_ROAR;
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
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();

		AABB area = new AABB(origin, origin).inflate(RANGE);
		List<Entity> candidates = level.getEntities(player, area,
				e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity);

		for (Entity entity : candidates) {
			Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = toTarget.length();
			if (dist > RANGE || dist < 0.001) {
				continue;
			}
			Vec3 toNorm = toTarget.normalize();
			double dot = toNorm.dot(forward);
			if (dot < CONE_HALF_ANGLE_COS) {
				continue;
			}
			entity.hurt(ModDamageTypes.lionRoar(level, player), DAMAGE);
			Vec3 push = forward.scale(KNOCKBACK);
			entity.push(push.x, 0.5, push.z);
			entity.hurtMarked = true;
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.1f);

		Vec3 cloudOrigin = origin.add(forward.scale(2.0));
		level.sendParticles(ParticleTypes.SONIC_BOOM,
				cloudOrigin.x, cloudOrigin.y, cloudOrigin.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.CLOUD,
				cloudOrigin.x, cloudOrigin.y, cloudOrigin.z, 60,
				2.0, 1.0, 2.0, 0.2);
		return true;
	}
}
