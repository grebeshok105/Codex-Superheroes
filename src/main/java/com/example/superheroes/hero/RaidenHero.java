package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class RaidenHero implements Hero {
	public static final ResourceLocation ID = ModId.of("raiden_shogun");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/raiden_shogun.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE01A0A2E,
			0xD00A0418,
			0x99A464FF,
			0x44E0CCFF,
			0xFFB890FF,
			0xFF3A1A88,
			0xFFA464FF,
			0x66C8A0FF,
			0xFFB890FF,
			0xFF1A0044,
			0xFFC8A0FF,
			0x66E0C8FF,
			0xFFC8A0FF,
			0x66A464FF,
			0xFFB890FF,
			0xFFB890FF,
			0xFFFFE8FF,
			0x66C8A0FF
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.electro", HeroHudConfig.EnergyIconType.LIGHTNING, true, "TRANSCENDENCE");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 1500f;
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
			default -> EntityDimensions.scalable(0.6f, 1.85f).withEyeHeight(1.65f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.RAIDEN_SWORD_DRAW,
				AbilityIds.RAIDEN_EYE_OF_JUDGMENT,
				AbilityIds.RAIDEN_MUSOU_SHINSETSU,
				AbilityIds.RAIDEN_MUSOU_ISSHIN,
				AbilityIds.RAIDEN_PLUNGING_STRIKE,
				AbilityIds.RAIDEN_TRANSCENDENCE
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.RAIDEN.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.RAIDEN.remove(player);
		HeroAttributes.RAIDEN_BURST.remove(player);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return false;
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
	public com.example.superheroes.physics.ImpactStyle getImpactStyle() {
		return com.example.superheroes.physics.ImpactStyle.WEAPON;
	}
	@Override
	public double getImpactPower() {
		return 1.05;
	}

}
