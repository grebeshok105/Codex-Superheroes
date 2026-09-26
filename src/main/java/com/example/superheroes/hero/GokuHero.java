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

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/goku/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/goku/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/goku/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/goku/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/goku/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/goku/knockback_resistance");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/goku/attack_speed");
	private static final ResourceLocation JUMP_ID = ModId.of("modifiers/goku/jump_strength");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/goku/step_height");
	private static final ResourceLocation REACH_ID = ModId.of("modifiers/goku/entity_reach");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 40.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 2.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, JUMP_ID, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

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
	public AttributeModifierSet passiveAttributes() {
		return PASSIVES;
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
