package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class BattleBeastBloodlustAbility implements Ability {
	private static final int COOLDOWN_TICKS = 16 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.BATTLE_BEAST_BLOODLUST;
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
		player.heal(12f);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, true, false, true));
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
				player.getX(), player.getY() + 1.2, player.getZ(), 24, 0.45, 0.55, 0.45, 0.08);
		level.sendParticles(ParticleTypes.CRIT,
				player.getX(), player.getY() + 1.0, player.getZ(), 48, 0.55, 0.7, 0.55, 0.12);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.1f, 1.2f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
