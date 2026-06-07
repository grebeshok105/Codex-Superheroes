package com.example.superheroes.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class XRayAbility implements Ability {
	private static final double RADIUS = 32.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.X_RAY;
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
		return 0.2f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(RADIUS);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
			entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 5, 0, true, false, false));
		}
	}
}
