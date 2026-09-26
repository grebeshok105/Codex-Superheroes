package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class GokuHero implements Hero {
	public static final ResourceLocation ID = ModId.of("goku");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/goku.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0331504,
			0xD0140602,
			0x88E85D04,
			0x33FFCC88,
			0xFFFF9A4A,
			0xFF7A2900,
			0xFFE85D04,
			0x55FFAA66,
			0xFFFFAA44,
			0xFF1A0A00,
			0xFFFF7A28,
			0x55FF8844,
			0xFFFF7A28,
			0x55E85D04,
			0xFFFFAA44,
			0xFFFFAA44,
			0xFFFFE0B0,
			0x55FFAA66
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.ki", HeroHudConfig.EnergyIconType.SUN, true, "SPIRIT BOMB");

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
		return 1.0f;
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
		return List.of(
				AbilityIds.GOKU_KAMEHAMEHA,
				AbilityIds.GOKU_INSTANT_TRANSMISSION,
				AbilityIds.GOKU_KI_CHARGE,
				AbilityIds.GOKU_SOLAR_FLARE,
				AbilityIds.GOKU_SPIRIT_BOMB,
				AbilityIds.GOKU_SUPER_SAIYAN_AURA
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.GOKU.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.GOKU.remove(player);
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
		return 1.20;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.A;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.FEATHER);
	}

}
