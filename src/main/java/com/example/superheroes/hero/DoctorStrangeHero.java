package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Doctor Strange — minimal shell on purpose: the whole feature budget goes
 * into the Mirror Dimension ability. Skin texture is a placeholder until the
 * user supplies art.
 */
public final class DoctorStrangeHero implements Hero {
	public static final ResourceLocation ID = ModId.of("doctor_strange");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/doctor_strange.png");

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
		return List.of(AbilityIds.MIRROR_DIMENSION);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.DOCTOR_STRANGE.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.DOCTOR_STRANGE.remove(player);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return false;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}
}
