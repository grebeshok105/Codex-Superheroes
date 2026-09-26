package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroHudConfig;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/raiden/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/raiden/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/raiden/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/raiden/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/raiden/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/raiden/knockback_resistance");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/raiden/attack_speed");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

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
	public AttributeModifierSet passiveAttributes() {
		return PASSIVES;
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		AbilityScopedModifiers.RAIDEN_BURST.remove(player);
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
	public io.github.grebeshok105.codex.core.hero.ImpactStyle getImpactStyle() {
		return io.github.grebeshok105.codex.core.hero.ImpactStyle.WEAPON;
	}
	@Override
	public double getImpactPower() {
		return 1.05;
	}

}
