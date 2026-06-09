package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ATrainHyperspeedAbility implements Ability {
	private static final int EFFECT_TICKS = 40;
	private static final int EXIT_COOLDOWN_TICKS = 6 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.A_TRAIN_HYPERSPEED;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 30f;
	}

	@Override
	public float costPerTick() {
		return 2.2f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		applySpeed(player);
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9f, 2.0f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		applySpeed(player);
		player.fallDistance = 0f;
		if (player.tickCount % 4 == 0) {
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.CLOUD,
					player.getX(), player.getY() + 0.08, player.getZ(), 8, 0.35, 0.04, 0.35, 0.01);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					player.getX(), player.getY() + 0.8, player.getZ(), 3, 0.25, 0.25, 0.25, 0.02);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.DIG_SPEED);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		AbilityCooldowns.setCooldownTicks(player, getId(), EXIT_COOLDOWN_TICKS);
	}

	private static void applySpeed(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, EFFECT_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_TICKS, 0, true, false, true));
	}
}
