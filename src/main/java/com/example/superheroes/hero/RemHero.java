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

public final class RemHero implements Hero {
	public static final ResourceLocation ID = ModId.of("rem");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/rem.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE0061422,
			0xD0020810,
			0x993D8CFF,
			0x443BDFFF,
			0xFF9CDCFF,
			0xFF082E54,
			0xFF4BB7FF,
			0x665FCBFF,
			0xFFE9F7FF,
			0xFF1A2740,
			0xFFB9E7FF,
			0x66B9E7FF,
			0xFFE2F4FF,
			0x663D8CFF,
			0xFFE2F4FF,
			0xFF9CDCFF,
			0xFFFFFFFF,
			0x665FCBFF
	);
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ModId.of("modifiers/rem/armor"), 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/rem/toughness"), 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/rem/damage"), 7.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/rem/attack_speed"), 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, ModId.of("modifiers/rem/speed"), 0.16, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, ModId.of("modifiers/rem/max_health"), 22.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, ModId.of("modifiers/rem/knockback_resistance"), 0.35, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, ModId.of("modifiers/rem/step_height"), 0.4, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 240f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.4f;
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
			default -> EntityDimensions.scalable(0.6f, 1.78f).withEyeHeight(1.58f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.REM_HEALING_MAGIC,
				AbilityIds.REM_ICE_BURST,
				AbilityIds.REM_ONI_RAGE,
				AbilityIds.REM_MORNING_STAR,
				AbilityIds.REM_MACE_CRATER,
				AbilityIds.REM_ONI_KICK,
				AbilityIds.REM_HUMA_ICE_SPIKES
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
		player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
		player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			com.example.superheroes.effect.RemDemonismController.clear(serverPlayer);
		}
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
}
