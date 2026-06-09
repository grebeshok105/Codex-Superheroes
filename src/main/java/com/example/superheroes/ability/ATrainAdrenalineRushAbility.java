package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ATrainAdrenalineRushAbility implements Ability {
	private static final int COOLDOWN_TICKS = 12 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.A_TRAIN_ADRENALINE_RUSH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 60f;
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
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.removeEffect(MobEffects.CONFUSION);
		player.heal(10f);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, true, false, true));
		level.sendParticles(ParticleTypes.HEART,
				player.getX(), player.getY() + 1.1, player.getZ(), 8, 0.4, 0.45, 0.4, 0.02);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 0.9, player.getZ(), 36, 0.45, 0.55, 0.45, 0.08);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.7f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
