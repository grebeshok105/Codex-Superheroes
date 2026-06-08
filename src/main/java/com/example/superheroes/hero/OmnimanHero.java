package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.OmnimanViltrumiteRushAbility;
import com.example.superheroes.effect.OmnimanMomentumController;
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

public final class OmnimanHero implements Hero {
	public static final ResourceLocation ID = ModId.of("omniman");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/omniman.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE00F1720,
			0xD0081018,
			0xAA7A0F18,
			0x55E5D0C8,
			0xFFE5D0C8,
			0xFF2A333D,
			0xFFB01C28,
			0x66D93642,
			0xFFFFE6DA,
			0xFF161C22,
			0xFFC9D1D9,
			0x66C9D1D9,
			0xFFE6D5CB,
			0x66840E18,
			0xFFC9D1D9,
			0xFFE6D5CB,
			0xFFFFFFFF,
			0x66B01C28
	);

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 320f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.5f;
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
			default -> EntityDimensions.scalable(0.6f, 1.88f).withEyeHeight(1.68f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.FLIGHT,
				AbilityIds.OMNIMAN_VILTRUMITE_RUSH,
				AbilityIds.OMNIMAN_WORLD_BREAKER,
				AbilityIds.SHOCKWAVE_PULSE,
				AbilityIds.VILTRUMITE_RECOVERY);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.OMNIMAN.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.OMNIMAN.remove(player);
		player.removeEffect(MobEffects.REGENERATION);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		if (player instanceof ServerPlayer sp) {
			OmnimanMomentumController.clear(sp);
			OmnimanViltrumiteRushAbility.clear(sp);
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
		float momentum = OmnimanMomentumController.momentum(player) / 100f;
		float scale = 0.45f + intensity * 1.45f + momentum * 0.45f;
		double radius = 4.0 + scale * 9.0;
		float damage = 7.0f + scale * 13.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();
		level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.55f);
		level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 0.85f);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 0.3, cz, 2, radius * 0.35, 0.15, radius * 0.35, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.15, cz, 70, radius * 0.55, 0.25, radius * 0.55, 0.1);
		level.sendParticles(ParticleTypes.CRIT, cx, cy + 0.65, cz, 34, radius * 0.35, 0.2, radius * 0.35, 0.16);
	}
}
