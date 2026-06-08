package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.MeteorSlamAbility;
import com.example.superheroes.ability.ViltrumiteChargeAbility;
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

public final class InvincibleHero implements Hero {
	public static final ResourceLocation ID = ModId.of("invincible");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/invincible.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE0081F38,
			0xD0030C1A,
			0x990AA4D8,
			0x44FFE15A,
			0xFFFFE15A,
			0xFF073C68,
			0xFF0AA4D8,
			0x660AD8FF,
			0xFF33CFFF,
			0xFF2B2200,
			0xFFFFD640,
			0x66FFE878,
			0xFFFFD640,
			0x660AA4D8,
			0xFFFFD640,
			0xFFFFD640,
			0xFFFFFFFF,
			0x66FFE15A
	);

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 240f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.2f;
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
			default -> EntityDimensions.scalable(0.6f, 1.85f).withEyeHeight(1.65f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.FLIGHT,
				AbilityIds.VILTRUMITE_THUNDER_CLAP,
				AbilityIds.SHOCKWAVE_PULSE,
				AbilityIds.VILTRUMITE_RECOVERY,
				AbilityIds.GUARDIANS_BREAKER);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.INVINCIBLE.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.INVINCIBLE.remove(player);
		player.removeEffect(MobEffects.REGENERATION);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		if (player instanceof ServerPlayer sp) {
			ViltrumiteChargeAbility.clear(sp);
			MeteorSlamAbility.clear(sp);
		}
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
		return THEME;
	}

	@Override
	public void onLanded(ServerPlayer player, LandingImpact impact) {
		float intensity = impact.intensity();
		float scale = 0.25f + intensity * 1.1f;
		double radius = 2.8 + scale * 7.0;
		float damage = 3.0f + scale * 8.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 0.3, cz,
				(int) (16 + radius * 5), radius * 0.35, 0.15, radius * 0.35, 0.12);
		if (impact.tier() == LandingImpact.Tier.STRONG || impact.tier() == LandingImpact.Tier.EPIC) {
			level.sendParticles(ParticleTypes.FLASH, cx, cy + 0.8, cz, 1, 0, 0, 0, 0);
			level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
	}
}
