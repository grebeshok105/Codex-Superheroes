package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class SungJinwooHero implements Hero {
	public static final ResourceLocation ID = ModId.of("sung_jinwoo");
	public static final ResourceLocation SKIN_PHASE_1 = ModId.of("textures/entity/hero/sung_jinwoo_phase1.png");
	public static final ResourceLocation SKIN_PHASE_2 = ModId.of("textures/entity/hero/sung_jinwoo_phase2.png");

	public static final HeroTheme THEME = new HeroTheme(
			0xE01A0030,                  // panelTop  — глубокий тёмно-фиолетовый
			0xD0080012,                  // panelBottom — почти чёрный
			0x88B58CFF,                  // panelBorder — светло-фиолетовый
			0x33C7A8FF,                  // panelHighlight
			0xFFD8B6FF,                  // heroNameColor — лавандовый
			0xFF3A1166,                  // energyDark — тёмно-фиолетовый
			0xFFB58CFF,                  // energyBright — фиолетовый
			0x55C7A8FF,                  // energyGlow
			0xFFB58CFF,                  // energyIcon
			0xFF1A0B33,                  // manaDark
			0xFF8A5AFF,                  // manaBright
			0x558A5AFF,                  // manaGlow
			0xFF8A5AFF,                  // manaIcon
			0x55B58CFF,                  // radialBorderIdle
			0xFFC7A8FF,                  // radialBorderActive
			0xFFC7A8FF,                  // radialKeyActive
			0xFFF1E6FF,                  // radialTextActive
			0x55C7A8FF                   // radialGlow
	);

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
		return 1.0f;
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
				AbilityIds.ARISE,
				AbilityIds.SHADOW_EXCHANGE,
				AbilityIds.SACRIFICE,
				AbilityIds.RULERS_AUTHORITY,
				AbilityIds.SHADOW_EXTRACTION,
				AbilityIds.MONARCHS_DOMAIN);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		// Все способности на ENERGY (Shadow Charges) по дефолту,
		// MONARCHS_DOMAIN жрёт MANA (символично — глубокий резерв монарха).
		return abilityId.equals(AbilityIds.MONARCHS_DOMAIN) ? ResourceKind.MANA : ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.SUNG_JINWOO.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.SUNG_JINWOO.remove(player);
		player.removeEffect(MobEffects.NIGHT_VISION);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.REGENERATION);
		if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
			com.example.superheroes.effect.SungJinwooController.resetPhase(sp);
		}
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return false;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN_PHASE_1;
	}

	@Override
	public HeroTheme getTheme() {
		return THEME;
	}
}
