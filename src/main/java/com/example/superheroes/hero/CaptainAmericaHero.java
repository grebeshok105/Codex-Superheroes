package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class CaptainAmericaHero implements Hero {
	public static final ResourceLocation ID = ModId.of("captain_america");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/captain_america.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE00A1E40,
			0xD0040A1A,
			0x881E40AF,
			0x33EF4444,
			0xFF60A0FF,
			0xFF0E2860,
			0xFF1E40AF,
			0x556090FF,
			0xFF60A0FF,
			0xFF1A0606,
			0xFFEF4444,
			0x55FF7878,
			0xFFEF4444,
			0x551E40AF,
			0xFFEF4444,
			0xFFEF4444,
			0xFFFFFFFF,
			0x556090FF
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.super_serum", HeroHudConfig.EnergyIconType.SHIELD, true, "COUNTER STANCE");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 200f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.4f;
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
				AbilityIds.CAP_SHIELD_THROW,
				AbilityIds.CAP_SHIELD_SLAM,
				AbilityIds.CAP_SHIELD_DASH,
				AbilityIds.CAP_COUNTER_STANCE
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.CAPTAIN_AMERICA.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.CAPTAIN_AMERICA.remove(player);
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
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public double getImpactPower() {
		return 0.88;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.B;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.SHIELD, PassiveGlyph.FIST, PassiveGlyph.FEATHER);
	}

}
