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
		return HeroTheme.RAIDEN;
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HeroHudConfig.RAIDEN;
	}
}
