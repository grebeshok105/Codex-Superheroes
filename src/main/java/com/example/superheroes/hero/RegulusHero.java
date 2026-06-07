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

public final class RegulusHero implements Hero {
	public static final ResourceLocation ID = ModId.of("regulus");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/regulus.png");

	public static final HeroTheme THEME = new HeroTheme(
			0xFFFFFFFF,
			0xFF606060,
			0xFFFFFFFF,
			0x44FFFFFF,
			0xFFFFFFFF,
			0xFFCCCCCC,
			0xFFFFFFFF,
			0x66FFFFFF,
			0xFFFFFFFF,
			0xFF555555,
			0xFFFFFFFF,
			0x66E0E0E0,
			0xFF555555,
			0x66E0E0E0,
			0xFFFFFFFF,
			0xFFFFFFFF,
			0xFFFFFFFF,
			0x66FFFFFF
	);

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 1000f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 2.0f;
	}

	@Override
	public float getManaMax() {
		return 0f;
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
		return List.of(AbilityIds.LION_HEART, AbilityIds.MANIA_OF_GREED, AbilityIds.GREEDS_EMBRACE, AbilityIds.LION_ROAR, AbilityIds.COUNTER_STRIKE);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.REGULUS.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.REGULUS.remove(player);
		player.removeEffect(MobEffects.REGENERATION);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.JUMP);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return true;
	}

	@Override
	public void onLanded(ServerPlayer player, LandingImpact impact) {
		float intensity = impact.intensity();
		float scale = 0.25f + intensity * 1.05f;
		double radius = 2.5 + scale * 6.5;
		float damage = 3.0f + scale * 8.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();

		switch (impact.tier()) {
			case WEAK -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.5f);
				level.sendParticles(ParticleTypes.CLOUD, cx, cy + 0.1, cz, 12, 0.6, 0.05, 0.6, 0.02);
			}
			case NORMAL -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 24, radius * 0.4, 0.15, radius * 0.4, 0.06);
				level.sendParticles(ParticleTypes.FLASH, cx, cy + 0.5, cz, 1, 0, 0, 0, 0);
			}
			case STRONG -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.8f);
				level.playSound(null, cx, cy, cz, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.9f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 40, radius * 0.55, 0.25, radius * 0.55, 0.08);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 32, radius * 0.5, 0.2, radius * 0.5, 0.1);
				level.sendParticles(ParticleTypes.FLASH, cx, cy + 0.6, cz, 2, 0, 0, 0, 0);
			}
			case EPIC -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.6f);
				level.playSound(null, cx, cy, cz, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.75f);
				level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 0.95f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 60, radius * 0.6, 0.3, radius * 0.6, 0.1);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 48, radius * 0.55, 0.25, radius * 0.55, 0.12);
				level.sendParticles(ParticleTypes.FLASH, cx, cy + 0.7, cz, 3, 0, 0, 0, 0);
			}
		}
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}

	@Override
	public HeroTheme getTheme() {
		return THEME;
	}
}
