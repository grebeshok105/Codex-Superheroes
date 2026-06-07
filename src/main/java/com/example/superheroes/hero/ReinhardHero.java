package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class ReinhardHero implements Hero {
	public static final ResourceLocation ID = ModId.of("reinhard");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/reinhard.png");

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
		return 2.5f;
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
				AbilityIds.REINHARD_SWORD_DRAW,
				AbilityIds.REINHARD_AIR_SLASH,
				AbilityIds.REINHARD_SWORD_WAVE,
				AbilityIds.REINHARD_COUNTER_RIPOSTE,
				AbilityIds.REINHARD_DIVINE_AURA,
				AbilityIds.REINHARD_SPEED_JUDGMENT,
				AbilityIds.REINHARD_JUDGMENT_MARK,
				AbilityIds.REINHARD_WISH
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.REINHARD.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.REINHARD.remove(player);
		HeroAttributes.REINHARD_DRAW.remove(player);
		for (int p = 1; p <= 5; p++) {
			HeroAttributes.buildReinhardPhaseSet(p).remove(player);
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
		return HeroTheme.REINHARD;
	}
}
