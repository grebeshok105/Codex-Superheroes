package com.example.superheroes.ability;

import com.example.superheroes.effect.KratosRageController;
import com.example.superheroes.hero.HeroAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class KratosSpartanRageAbility implements Ability {
	private static final int DURATION_TICKS = 240;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KRATOS_SPARTAN_RAGE;
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
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return KratosRageController.getRage(player) >= KratosRageController.MAX_RAGE - 0.001f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!KratosRageController.tryActivate(player)) return false;
		HeroAttributes.KRATOS_RAGE.apply(player);
		player.setHealth(player.getMaxHealth());

		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION_TICKS, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 2, true, false, true));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.0f, 0.55f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.6f);
		level.sendParticles(ParticleTypes.FLAME,
				player.getX(), player.getY() + 0.6, player.getZ(),
				120, 0.6, 1.0, 0.6, 0.18);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				player.getX(), player.getY() + 0.6, player.getZ(),
				40, 0.8, 0.4, 0.8, 0.05);
		level.sendParticles(ParticleTypes.LAVA,
				player.getX(), player.getY() + 0.4, player.getZ(),
				18, 0.8, 0.2, 0.8, 0.0);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (player.tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.FLAME,
					player.getX(), player.getY() + 1.0, player.getZ(),
					6, 0.45, 0.7, 0.45, 0.04);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					player.getX(), player.getY() + 0.4, player.getZ(),
					3, 0.4, 0.3, 0.4, 0.02);
		}
		if (player.tickCount % 12 == 0) {
			level.sendParticles(ParticleTypes.LAVA,
					player.getX(), player.getY() + 0.5, player.getZ(),
					2, 0.3, 0.1, 0.3, 0.0);
		}
		if (player.tickCount % 20 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 0.6f);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		HeroAttributes.KRATOS_RAGE.remove(player);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.ABSORPTION);
		KratosRageController.onAbilityDeactivated(player);
	}

	public static boolean isActive(ServerPlayer player) {
		return KratosRageController.isActive(player);
	}
}
