package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class BattleBeastHero implements Hero {
	public static final ResourceLocation ID = ModId.of("battle_beast");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/battle_beast.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE0181512,
			0xD00B0806,
			0xAA7A1010,
			0x55E3C6A5,
			0xFFE3C6A5,
			0xFF2C241E,
			0xFFD04432,
			0x66FF5A4A,
			0xFFFFE0C2,
			0xFF1B120E,
			0xFFC9A16E,
			0x66D6A668,
			0xFFFFC98E,
			0x668F1512,
			0xFFFFD0A0,
			0xFFFFE0C2,
			0xFFFFFFFF,
			0x66D04432
	);
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ModId.of("modifiers/battle_beast/armor"), 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/battle_beast/toughness"), 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, ModId.of("modifiers/battle_beast/damage"), 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/battle_beast/attack_speed"), 0.4, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, ModId.of("modifiers/battle_beast/speed"), 0.16, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, ModId.of("modifiers/battle_beast/max_health"), 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, ModId.of("modifiers/battle_beast/knockback_resistance"), 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, ModId.of("modifiers/battle_beast/reach"), 0.8, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, ModId.of("modifiers/battle_beast/step_height"), 0.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SCALE, ModId.of("modifiers/battle_beast/scale"), 0.35, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 260f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.1f;
	}

	@Override
	public float getManaMax() {
		return 0f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.86f, 1.98f).withEyeHeight(1.68f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.86f, 0.86f).withEyeHeight(0.58f);
			default -> EntityDimensions.scalable(0.86f, 2.5f).withEyeHeight(2.23f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.BATTLE_BEAST_PREDATOR_LEAP,
				AbilityIds.BATTLE_BEAST_AXE_CLEAVE,
				AbilityIds.BATTLE_BEAST_WAR_ROAR,
				AbilityIds.BATTLE_BEAST_BLOODLUST
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		PASSIVES.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		if (player instanceof ServerPlayer serverPlayer) {
			com.example.superheroes.effect.BattleBeastCurseController.clear(serverPlayer);
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
	public void onLanded(ServerPlayer player, LandingImpact impact) {
		if (impact.tier() == LandingImpact.Tier.WEAK) {
			return;
		}
		ServerLevel level = player.serverLevel();
		double radius = 3.5 + impact.intensity() * 5.5;
		float damage = com.example.superheroes.effect.BattleBeastCurseController.scaleDamage(player,
				4.0f + impact.intensity() * 8.0f);
		com.example.superheroes.physics.ShockwaveUtil.detonate(player, player.position(), radius, damage, false);
		level.sendParticles(ParticleTypes.CRIT,
				player.getX(), player.getY() + 0.4, player.getZ(), 32, radius * 0.35, 0.2, radius * 0.35, 0.12);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.65f);
	}
}
