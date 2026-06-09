package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class RemHealingMagicAbility implements Ability {
	private static final int COOLDOWN_TICKS = 14 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_HEALING_MAGIC;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 65f;
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
		player.clearFire();
		player.heal(14f);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 0, true, false, true));
		level.sendParticles(ParticleTypes.HEART,
				player.getX(), player.getY() + 1.2, player.getZ(), 12, 0.45, 0.5, 0.45, 0.02);
		level.sendParticles(ParticleTypes.SNOWFLAKE,
				player.getX(), player.getY() + 1.0, player.getZ(), 28, 0.5, 0.7, 0.5, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.45f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
