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

public final class ATrainHero implements Hero {
	public static final ResourceLocation ID = ModId.of("a_train");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/a_train.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE006101F,
			0xD0020712,
			0x991E7BFF,
			0x44FF2438,
			0xFFE5F0FF,
			0xFF051E44,
			0xFF2C8CFF,
			0x66FF2E46,
			0xFFFFFFFF,
			0xFF12080A,
			0xFFFF4A5C,
			0x66FF4A5C,
			0xFFE5F0FF,
			0x661E7BFF,
			0xFFE5F0FF,
			0xFFFF4A5C,
			0xFFFFFFFF,
			0x662C8CFF
	);
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ModId.of("modifiers/a_train/armor"), 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/a_train/toughness"), 3.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/a_train/damage"), 5.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/a_train/attack_speed"), 2.4, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, ModId.of("modifiers/a_train/speed"), 0.85, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, ModId.of("modifiers/a_train/max_health"), 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, ModId.of("modifiers/a_train/jump_strength"), 0.22, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SAFE_FALL_DISTANCE, ModId.of("modifiers/a_train/safe_fall"), 24.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 300f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 2.1f;
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
				AbilityIds.A_TRAIN_MACH_DASH,
				AbilityIds.A_TRAIN_SONIC_BOOM,
				AbilityIds.A_TRAIN_HYPERSPEED,
				AbilityIds.A_TRAIN_ADRENALINE_RUSH
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
		player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
		player.removeEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED);
		player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
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
		return HeroHudConfig.A_TRAIN;
	}
}
