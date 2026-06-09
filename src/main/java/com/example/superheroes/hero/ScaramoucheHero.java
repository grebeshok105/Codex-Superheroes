package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.ScaramoucheWindPrisonAbility;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class ScaramoucheHero implements Hero {
	public static final ResourceLocation ID = ModId.of("scaramouche");
	public static final HeroTheme THEME = new HeroTheme(
			0xE0061B24,
			0xD0020A12,
			0x9934E4D7,
			0x443DFFE8,
			0xFF7EF7E4,
			0xFF063D46,
			0xFF23D9D0,
			0x6625F5E5,
			0xFF56F2E9,
			0xFF170A40,
			0xFFA47BFF,
			0x667F66FF,
			0xFFB79BFF,
			0x6634E4D7,
			0xFFA47BFF,
			0xFF7EF7E4,
			0xFFFFFFFF,
			0x663DFFE8
	);
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ModId.of("modifiers/scaramouche/armor"), 14.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/scaramouche/toughness"), 4.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/scaramouche/damage"), 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/scaramouche/attack_speed"), 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, ModId.of("modifiers/scaramouche/speed"), 0.18, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, ModId.of("modifiers/scaramouche/max_health"), 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, ModId.of("modifiers/scaramouche/jump_strength"), 0.25, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SAFE_FALL_DISTANCE, ModId.of("modifiers/scaramouche/safe_fall"), 24.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 320f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.6f;
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
				AbilityIds.FLIGHT,
				AbilityIds.SCARAMOUCHE_WINDSTEP,
				AbilityIds.SCARAMOUCHE_ELECTRO_SWIRL,
				AbilityIds.SCARAMOUCHE_WIND_PRISON,
				AbilityIds.SCARAMOUCHE_SKYFALL_BURST
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		PASSIVES.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		player.removeEffect(MobEffects.SLOW_FALLING);
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.JUMP);
		if (player instanceof ServerPlayer serverPlayer) {
			ScaramoucheWindPrisonAbility.clear(serverPlayer);
		}
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return true;
	}

	@Override
	public HeroTheme getTheme() {
		return THEME;
	}
}
