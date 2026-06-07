package com.example.superheroes.ability;

import com.example.superheroes.effect.RegulusGreedController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ManiaOfGreedAbility implements Ability {
	private static final double RAY_RANGE = 100.0;
	public static final int COOLDOWN_TICKS = 25 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.MANIA_OF_GREED;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 5f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		LivingEntity victim = findTarget(player);
		if (victim == null) {
			return false;
		}
		ServerLevel level = player.serverLevel();
		RegulusGreedController.startMagnet(player, victim);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.6f);
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
				SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.7f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		RegulusGreedController.tickMagnet(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		RegulusGreedController.releaseAndFreeze(player);
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.MANIA_OF_GREED, COOLDOWN_TICKS);
	}

	private static LivingEntity findTarget(ServerPlayer player) {
		Vec3 eyes = player.getEyePosition(1.0f);
		Vec3 look = player.getViewVector(1.0f);
		Vec3 end = eyes.add(look.scale(RAY_RANGE));
		HitResult blockHit = player.serverLevel().clip(new ClipContext(eyes, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDistSq = blockHit.getType() == HitResult.Type.MISS
				? RAY_RANGE * RAY_RANGE
				: blockHit.getLocation().distanceToSqr(eyes);

		AABB box = player.getBoundingBox().expandTowards(look.scale(RAY_RANGE)).inflate(1.5);
		LivingEntity best = null;
		double bestDist = maxDistSq;
		for (Entity entity : player.serverLevel().getEntities(player, box,
				e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity)) {
			Vec3 pos = entity.getBoundingBox().getCenter();
			Vec3 toEntity = pos.subtract(eyes);
			double along = toEntity.dot(look);
			if (along <= 0) continue;
			Vec3 closest = eyes.add(look.scale(along));
			double offsetSq = closest.distanceToSqr(pos);
			double radius = entity.getBoundingBox().getSize() * 0.6;
			if (offsetSq > radius * radius) continue;
			double d = pos.distanceToSqr(eyes);
			if (d < bestDist) {
				bestDist = d;
				best = (LivingEntity) entity;
			}
		}
		return best;
	}
}
