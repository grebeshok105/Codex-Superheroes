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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class IronManHero implements Hero {
	public static final ResourceLocation ID = ModId.of("iron_man");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/ironman.png");

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
		return 3.0f;
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
		return List.of(AbilityIds.IRON_MAN_FLIGHT, AbilityIds.SUPERSONIC, AbilityIds.REPULSOR, AbilityIds.BOX_ESP, AbilityIds.UNIBEAM, AbilityIds.IRON_MAN_HULKBUSTER);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.IRON_MAN.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.IRON_MAN.remove(player);
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
		return HeroTheme.IRON_MAN;
	}

	@Override
	public void onLanded(ServerPlayer player, LandingImpact impact) {
		float intensity = impact.intensity();
		float scale = 0.30f + intensity * 1.20f;
		double radius = 2.0 + scale * 5.5;
		float damage = 2.0f + scale * 7.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();

		switch (impact.tier()) {
			case WEAK -> {
				level.playSound(null, cx, cy, cz, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 0.9f, 1.3f);
				level.playSound(null, cx, cy, cz, SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 1.0f, 1.1f);
				level.sendParticles(ParticleTypes.SMOKE, cx, cy + 0.1, cz, 8, 0.5, 0.05, 0.5, 0.02);
			}
			case NORMAL -> {
				level.playSound(null, cx, cy, cz, SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.0f, 0.95f);
				level.playSound(null, cx, cy, cz, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.3f, 0.8f);
				level.playSound(null, cx, cy, cz, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.6f);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 22, radius * 0.4, 0.15, radius * 0.4, 0.05);
				level.sendParticles(ParticleTypes.SMOKE, cx, cy + 0.1, cz, 18, radius * 0.35, 0.1, radius * 0.35, 0.04);
			}
			case STRONG -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.3f, 0.85f);
				level.playSound(null, cx, cy, cz, SoundEvents.IRON_GOLEM_DEATH, SoundSource.PLAYERS, 1.2f, 0.75f);
				level.playSound(null, cx, cy, cz, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.6f, 0.6f);
				level.playSound(null, cx, cy, cz, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 1.2f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 40, radius * 0.5, 0.2, radius * 0.5, 0.06);
				level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 32, radius * 0.45, 0.18, radius * 0.45, 0.08);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, cy + 0.4, cz, 3, radius * 0.4, 0.1, radius * 0.4, 0.0);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 0.3, cz, 30, radius * 0.4, 0.15, radius * 0.4, 0.15);
			}
			case EPIC -> {
				level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.55f);
				level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.1f);
				level.playSound(null, cx, cy, cz, SoundEvents.IRON_GOLEM_DEATH, SoundSource.PLAYERS, 1.5f, 0.6f);
				level.playSound(null, cx, cy, cz, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 2.0f, 0.5f);
				level.playSound(null, cx, cy, cz, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.3f, 0.9f);
				level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 0.3, cz, 2, radius * 0.35, 0.2, radius * 0.35, 0.0);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 65, radius * 0.55, 0.3, radius * 0.55, 0.1);
				level.sendParticles(ParticleTypes.FLASH, cx, cy + 0.8, cz, 1, 0.0, 0.0, 0.0, 0.0);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, cy + 0.4, cz, 6, radius * 0.5, 0.1, radius * 0.5, 0.0);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 0.3, cz, 60, radius * 0.5, 0.2, radius * 0.5, 0.25);
				level.sendParticles(ParticleTypes.FIREWORK, cx, cy + 0.4, cz, 30, radius * 0.45, 0.2, radius * 0.45, 0.1);
			}
		}
	}
}
