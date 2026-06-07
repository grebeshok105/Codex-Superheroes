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
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class GokuSolarFlareAbility implements Ability {
	private static final int COOLDOWN_TICKS = 500;
	private static final double RADIUS = 20.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GOKU_SOLAR_FLARE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 70f;
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
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator());
		for (LivingEntity target : targets) {
			if (target instanceof ServerPlayer) {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 1, true, true, true));
			} else {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 3, true, true, true));
				target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, true, true));
			}
		}
		level.sendParticles(ParticleTypes.FLASH,
				player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.6, 0.6, 0.6, 0);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(), 100, RADIUS * 0.2, 1.2, RADIUS * 0.2, 0.2);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6f, 1.8f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
