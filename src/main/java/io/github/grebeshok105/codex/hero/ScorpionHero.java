package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroHudConfig;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class ScorpionHero implements Hero {
	public static final ResourceLocation ID = ModId.of("scorpion");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/scorpion.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE01A0A02,
			0xD00A0301,
			0x88FF6A14,
			0x33FFB060,
			0xFFFF8A2A,
			0xFF6A1E00,
			0xFFFFB048,
			0x55FF8A30,
			0xFFFFB048,
			0xFF200800,
			0xFFFF9A30,
			0x55FFCC80,
			0xFFFF9A30,
			0x55FF6A14,
			0xFFFFB048,
			0xFFFFB048,
			0xFFFFF0E0,
			0x55FF8A30
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.hellfire", HeroHudConfig.EnergyIconType.FLAME, false, null);

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/scorpion/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/scorpion/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/scorpion/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/scorpion/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/scorpion/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/scorpion/knockback_resistance");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 14.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 4.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.3, AttributeModifier.Operation.ADD_VALUE)
			.build();

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
	public AttributeModifierSet passiveAttributes() {
		return PASSIVES;
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
}
