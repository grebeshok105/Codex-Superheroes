package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class ScorpionHero implements Hero {
	public static final ResourceLocation ID = ModId.of("scorpion");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/scorpion.png");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 150f;
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
				AbilityIds.SCORPION_SPEAR,
				AbilityIds.SCORPION_HELLFIRE,
				AbilityIds.SCORPION_FIRE_TELEPORT,
				AbilityIds.SCORPION_HELL_BREATH
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.SCORPION.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.SCORPION.remove(player);
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
		return HeroTheme.SCORPION;
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HeroHudConfig.SCORPION;
	}
}
