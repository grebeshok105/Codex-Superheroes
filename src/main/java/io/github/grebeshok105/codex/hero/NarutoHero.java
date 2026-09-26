package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class NarutoHero implements Hero {
	public static final ResourceLocation ID = ModId.of("naruto");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/naruto.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0332B00,
			0xD0141000,
			0x88FFD60A,
			0x33FFE99A,
			0xFFFFE85A,
			0xFF7A6700,
			0xFFFFD60A,
			0x55FFEC8A,
			0xFFFFE060,
			0xFF1A1500,
			0xFFFFCC1A,
			0x55FFE070,
			0xFFFFCC1A,
			0x55FFD60A,
			0xFFFFE060,
			0xFFFFE060,
			0xFFFFF7C0,
			0x55FFE070
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.chakra", HeroHudConfig.EnergyIconType.SPIRAL, true, "BIJUUDAMA");

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/naruto/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/naruto/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/naruto/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/naruto/speed");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/naruto/attack_speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/naruto/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/naruto/knockback_resistance");
	private static final ResourceLocation JUMP_ID = ModId.of("modifiers/naruto/jump_strength");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/naruto/step_height");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 22.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.55, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 3.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, HP_ID, 40.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, JUMP_ID, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
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
		return 1.8f;
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
				AbilityIds.NARUTO_RASENGAN,
				AbilityIds.NARUTO_OODAMA_RASENGAN,
				AbilityIds.NARUTO_RASENSHURIKEN,
				AbilityIds.NARUTO_SAGE_MODE,
				AbilityIds.NARUTO_BIJUUDAMA,
				AbilityIds.NARUTO_SHADOW_CLONES
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
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.B;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.SKULL);
	}

	@Override
	public boolean canSuperJump() {
		return true;
	}

}
