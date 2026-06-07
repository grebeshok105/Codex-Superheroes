package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ViltrumiteRecoveryAbility implements Ability {
	private static final int COOLDOWN_TICKS = 280;
	private static final float HEAL_AMOUNT = 18f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.VILTRUMITE_RECOVERY;
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
		player.clearFire();
		player.heal(HEAL_AMOUNT);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 0, true, false, true));

		level.sendParticles(ParticleTypes.HEART,
				player.getX(), player.getY() + 1.2, player.getZ(),
				10, 0.45, 0.55, 0.45, 0.02);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.55, 0.75, 0.55, 0.12);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.35f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.55f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
