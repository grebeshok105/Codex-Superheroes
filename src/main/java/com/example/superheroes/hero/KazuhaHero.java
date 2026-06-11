package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class KazuhaHero implements Hero {
	public static final ResourceLocation ID = ModId.of("kazuha");
	public static final HeroTheme THEME = new HeroTheme(
			0xE008241A,
			0xD0030D0A,
			0x8878F0C8,
			0x33FFE0A8,
			0xFFFFB86A,
			0xFF0B5A45,
			0xFF3FD6A5,
			0x6648FFD0,
			0xFF62F5C6,
			0xFF361000,
			0xFFFF9A4A,
			0x66FFC080,
			0xFFFFA65A,
			0x5578F0C8,
			0xFFFFB86A,
			0xFFFFB86A,
			0xFFFFFFFF,
			0x6678F0C8
	);
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ModId.of("modifiers/kazuha/armor"), 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/kazuha/toughness"), 4.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/kazuha/damage"), 7.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/kazuha/attack_speed"), 1.2, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, ModId.of("modifiers/kazuha/speed"), 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, ModId.of("modifiers/kazuha/max_health"), 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, ModId.of("modifiers/kazuha/jump_strength"), 0.35, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SAFE_FALL_DISTANCE, ModId.of("modifiers/kazuha/safe_fall"), 18.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, ModId.of("modifiers/kazuha/reach"), 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 280f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 2.4f;
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
				AbilityIds.KAZUHA_CHIHAYABURU,
				AbilityIds.KAZUHA_MIDARE_RANZAN,
				AbilityIds.KAZUHA_AUTUMN_WHIRLWIND,
				AbilityIds.KAZUHA_MAPLE_STORM
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		PASSIVES.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return true;
	}

	@Override
	public HeroTheme getTheme() {
		return THEME;
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HeroHudConfig.KAZUHA;
	}
}
