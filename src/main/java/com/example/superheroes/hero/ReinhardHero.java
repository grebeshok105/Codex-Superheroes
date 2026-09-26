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

public final class ReinhardHero implements Hero {
	public static final ResourceLocation ID = ModId.of("reinhard");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/reinhard.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE02A0608,
			0xD0140204,
			0x99E62020,
			0x44FFB0B0,
			0xFFFF4848,
			0xFF6E0000,
			0xFFE61A1A,
			0x66FF6464,
			0xFFFF3030,
			0xFF1A0000,
			0xFFFF6060,
			0x66FFA0A0,
			0xFFFF6060,
			0x66E61A1A,
			0xFFFF4040,
			0xFFFF4040,
			0xFFFFE0E0,
			0x66FF6464
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.divine_blessing", HeroHudConfig.EnergyIconType.SWORD, false, null);

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/reinhard/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/reinhard/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/reinhard/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/reinhard/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/reinhard/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/reinhard/knockback_resistance");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/reinhard/attack_speed");
	private static final ResourceLocation JUMP_ID = ModId.of("modifiers/reinhard/jump_strength");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/reinhard/step_height");
	private static final ResourceLocation REACH_ID = ModId.of("modifiers/reinhard/entity_reach");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 28.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 14.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 60.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, JUMP_ID, 0.3, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

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
	public AttributeModifierSet passiveAttributes() {
		return PASSIVES;
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
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
		return THEME;
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public double getImpactPower() {
		return 1.05;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.S;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FEATHER, PassiveGlyph.SHIELD, PassiveGlyph.STAR, PassiveGlyph.SWORD,
				PassiveGlyph.HEART, PassiveGlyph.BOLT);
	}

	@Override
	public boolean canSuperJump() {
		return true;
	}

}
