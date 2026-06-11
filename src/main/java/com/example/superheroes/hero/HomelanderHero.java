package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.physics.ShockwaveUtil;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class HomelanderHero implements Hero {
	public static final ResourceLocation ID = ModId.of("homelander");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/homelander.png");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 100f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 0.5f;
	}

	@Override
	public float getManaMax() {
		return 100f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
			default -> EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.FLIGHT,
				AbilityIds.EYE_LASERS,
				AbilityIds.X_RAY,
				AbilityIds.IRON_FISTS,
				AbilityIds.HAND_CLAP,
				AbilityIds.STUNNING_ROAR);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.HOMELANDER.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.HOMELANDER.remove(player);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return true;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}

	@Override
	public HeroTheme getTheme() {
		return HeroTheme.HOMELANDER;
	}

	@Override
	public void onLanded(ServerPlayer player, LandingImpact impact) {
		float intensity = impact.intensity();
		float scale = 0.30f + intensity * 1.20f;
		double radius = 3.0 + scale * 8.0;
		float damage = 4.0f + scale * 10.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();

		switch (impact.tier()) {
			case WEAK -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
				level.sendParticles(ParticleTypes.CLOUD, cx, cy + 0.1, cz, 12, 0.6, 0.05, 0.6, 0.02);
			}
			case NORMAL -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1f, 0.95f);
				level.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.1f);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 28, radius * 0.4, 0.15, radius * 0.4, 0.06);
				level.sendParticles(ParticleTypes.CLOUD, cx, cy + 0.1, cz, 16, radius * 0.3, 0.1, radius * 0.3, 0.05);
			}
			case STRONG -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6f, 0.7f);
				level.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.4f, 0.9f);
				level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 1.0f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 50, radius * 0.55, 0.25, radius * 0.55, 0.08);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 40, radius * 0.5, 0.2, radius * 0.5, 0.1);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, cy + 0.5, cz, 4, radius * 0.4, 0.1, radius * 0.4, 0.0);
			}
			case EPIC -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);
				level.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.7f);
				level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.85f);
				level.playSound(null, cx, cy, cz, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.9f, 1.4f);
				level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 0.3, cz, 3, radius * 0.4, 0.2, radius * 0.4, 0.0);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 80, radius * 0.6, 0.4, radius * 0.6, 0.12);
				level.sendParticles(ParticleTypes.FLASH, cx, cy + 1.0, cz, 1, 0.0, 0.0, 0.0, 0.0);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, cy + 0.5, cz, 8, radius * 0.5, 0.2, radius * 0.5, 0.0);
				level.sendParticles(ParticleTypes.LAVA, cx, cy + 0.2, cz, 20, radius * 0.5, 0.2, radius * 0.5, 0.05);
			}
		}
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HeroHudConfig.HOMELANDER;
	}
}
