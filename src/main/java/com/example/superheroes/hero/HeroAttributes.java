package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class HeroAttributes {
	public static final ResourceLocation HOMELANDER_ARMOR = ModId.of("modifiers/homelander/armor");
	public static final ResourceLocation HOMELANDER_TOUGHNESS = ModId.of("modifiers/homelander/toughness");
	public static final ResourceLocation HOMELANDER_DAMAGE = ModId.of("modifiers/homelander/damage");
	public static final ResourceLocation HOMELANDER_SPEED = ModId.of("modifiers/homelander/speed");
	public static final ResourceLocation HOMELANDER_HP = ModId.of("modifiers/homelander/max_health");
	public static final ResourceLocation HOMELANDER_KNOCKBACK = ModId.of("modifiers/homelander/knockback_resistance");

	public static final ResourceLocation IRON_MAN_ARMOR = ModId.of("modifiers/iron_man/armor");
	public static final ResourceLocation IRON_MAN_TOUGHNESS = ModId.of("modifiers/iron_man/toughness");
	public static final ResourceLocation IRON_MAN_DAMAGE = ModId.of("modifiers/iron_man/damage");
	public static final ResourceLocation IRON_MAN_SPEED = ModId.of("modifiers/iron_man/speed");
	public static final ResourceLocation IRON_MAN_KNOCKBACK = ModId.of("modifiers/iron_man/knockback_resistance");

	public static final ResourceLocation REGULUS_ARMOR = ModId.of("modifiers/regulus/armor");
	public static final ResourceLocation REGULUS_KNOCKBACK = ModId.of("modifiers/regulus/knockback_resistance");
	public static final ResourceLocation REGULUS_MADNESS_ARMOR = ModId.of("modifiers/regulus/madness_armor");
	public static final ResourceLocation REGULUS_MADNESS_HP = ModId.of("modifiers/regulus/madness_max_health");
	public static final ResourceLocation REGULUS_MADNESS_DAMAGE = ModId.of("modifiers/regulus/madness_damage");

	public static final ResourceLocation SUNG_ARMOR = ModId.of("modifiers/sung_jinwoo/armor");
	public static final ResourceLocation SUNG_TOUGHNESS = ModId.of("modifiers/sung_jinwoo/toughness");
	public static final ResourceLocation SUNG_DAMAGE = ModId.of("modifiers/sung_jinwoo/damage");
	public static final ResourceLocation SUNG_SPEED = ModId.of("modifiers/sung_jinwoo/speed");
	public static final ResourceLocation SUNG_KNOCKBACK = ModId.of("modifiers/sung_jinwoo/knockback_resistance");
	public static final ResourceLocation SUNG_ATTACK_SPEED = ModId.of("modifiers/sung_jinwoo/attack_speed");

	public static final ResourceLocation DOOMSDAY_ARMOR = ModId.of("modifiers/doomsday/armor");
	public static final ResourceLocation DOOMSDAY_TOUGHNESS = ModId.of("modifiers/doomsday/toughness");
	public static final ResourceLocation DOOMSDAY_DAMAGE = ModId.of("modifiers/doomsday/damage");
	public static final ResourceLocation DOOMSDAY_SPEED = ModId.of("modifiers/doomsday/speed");
	public static final ResourceLocation DOOMSDAY_HP = ModId.of("modifiers/doomsday/max_health");
	public static final ResourceLocation DOOMSDAY_KNOCKBACK = ModId.of("modifiers/doomsday/knockback_resistance");
	public static final ResourceLocation DOOMSDAY_SCALE = ModId.of("modifiers/doomsday/scale");
	public static final ResourceLocation DOOMSDAY_REACH = ModId.of("modifiers/doomsday/entity_reach");
	public static final ResourceLocation DOOMSDAY_BLOCK_REACH = ModId.of("modifiers/doomsday/block_reach");
	public static final ResourceLocation DOOMSDAY_STEP = ModId.of("modifiers/doomsday/step_height");
	public static final ResourceLocation DOOMSDAY_JUMP = ModId.of("modifiers/doomsday/jump_strength");
	public static final ResourceLocation DOOMSDAY_BERSERK_DAMAGE = ModId.of("modifiers/doomsday/berserk_damage");
	public static final ResourceLocation DOOMSDAY_BERSERK_ARMOR = ModId.of("modifiers/doomsday/berserk_armor");
	public static final ResourceLocation DOOMSDAY_BERSERK_SPEED = ModId.of("modifiers/doomsday/berserk_speed");
	public static final ResourceLocation DOOMSDAY_ADAPT_DAMAGE = ModId.of("modifiers/doomsday/adapt_damage");

	public static final ResourceLocation GOKU_ARMOR = ModId.of("modifiers/goku/armor");
	public static final ResourceLocation GOKU_TOUGHNESS = ModId.of("modifiers/goku/toughness");
	public static final ResourceLocation GOKU_DAMAGE = ModId.of("modifiers/goku/damage");
	public static final ResourceLocation GOKU_SPEED = ModId.of("modifiers/goku/speed");
	public static final ResourceLocation GOKU_HP = ModId.of("modifiers/goku/max_health");
	public static final ResourceLocation GOKU_KNOCKBACK = ModId.of("modifiers/goku/knockback_resistance");
	public static final ResourceLocation GOKU_ATTACK_SPEED = ModId.of("modifiers/goku/attack_speed");
	public static final ResourceLocation GOKU_JUMP = ModId.of("modifiers/goku/jump_strength");
	public static final ResourceLocation GOKU_STEP = ModId.of("modifiers/goku/step_height");
	public static final ResourceLocation GOKU_REACH = ModId.of("modifiers/goku/entity_reach");

	public static final ResourceLocation NARUTO_ARMOR = ModId.of("modifiers/naruto/armor");
	public static final ResourceLocation NARUTO_TOUGHNESS = ModId.of("modifiers/naruto/toughness");
	public static final ResourceLocation NARUTO_DAMAGE = ModId.of("modifiers/naruto/damage");
	public static final ResourceLocation NARUTO_SPEED = ModId.of("modifiers/naruto/speed");
	public static final ResourceLocation NARUTO_ATTACK_SPEED = ModId.of("modifiers/naruto/attack_speed");
	public static final ResourceLocation NARUTO_HP = ModId.of("modifiers/naruto/max_health");
	public static final ResourceLocation NARUTO_KNOCKBACK = ModId.of("modifiers/naruto/knockback_resistance");
	public static final ResourceLocation NARUTO_JUMP = ModId.of("modifiers/naruto/jump_strength");
	public static final ResourceLocation NARUTO_STEP = ModId.of("modifiers/naruto/step_height");

	public static final ResourceLocation CAP_ARMOR = ModId.of("modifiers/captain_america/armor");
	public static final ResourceLocation CAP_TOUGHNESS = ModId.of("modifiers/captain_america/toughness");
	public static final ResourceLocation CAP_DAMAGE = ModId.of("modifiers/captain_america/damage");
	public static final ResourceLocation CAP_HP = ModId.of("modifiers/captain_america/max_health");
	public static final ResourceLocation CAP_KNOCKBACK = ModId.of("modifiers/captain_america/knockback_resistance");

	public static final ResourceLocation KRATOS_ARMOR = ModId.of("modifiers/kratos/armor");
	public static final ResourceLocation KRATOS_TOUGHNESS = ModId.of("modifiers/kratos/toughness");
	public static final ResourceLocation KRATOS_DAMAGE = ModId.of("modifiers/kratos/damage");
	public static final ResourceLocation KRATOS_ATTACK_SPEED = ModId.of("modifiers/kratos/attack_speed");
	public static final ResourceLocation KRATOS_HP = ModId.of("modifiers/kratos/max_health");
	public static final ResourceLocation KRATOS_KNOCKBACK = ModId.of("modifiers/kratos/knockback_resistance");
	public static final ResourceLocation KRATOS_REACH = ModId.of("modifiers/kratos/entity_reach");
	public static final ResourceLocation KRATOS_STEP = ModId.of("modifiers/kratos/step_height");
	public static final ResourceLocation KRATOS_RAGE_DAMAGE = ModId.of("modifiers/kratos/rage_damage");
	public static final ResourceLocation KRATOS_RAGE_SPEED = ModId.of("modifiers/kratos/rage_speed");

	public static final ResourceLocation LOKI_ARMOR = ModId.of("modifiers/loki/armor");
	public static final ResourceLocation LOKI_DAMAGE = ModId.of("modifiers/loki/damage");
	public static final ResourceLocation LOKI_SPEED = ModId.of("modifiers/loki/speed");
	public static final ResourceLocation LOKI_HP = ModId.of("modifiers/loki/max_health");
	public static final ResourceLocation LOKI_JUMP = ModId.of("modifiers/loki/jump_strength");
	public static final ResourceLocation LOKI_KNOCKBACK = ModId.of("modifiers/loki/knockback_resistance");

	public static final ResourceLocation THANOS_ARMOR = ModId.of("modifiers/thanos/armor");
	public static final ResourceLocation THANOS_TOUGHNESS = ModId.of("modifiers/thanos/toughness");
	public static final ResourceLocation THANOS_DAMAGE = ModId.of("modifiers/thanos/damage");
	public static final ResourceLocation THANOS_HP = ModId.of("modifiers/thanos/max_health");
	public static final ResourceLocation THANOS_KNOCKBACK = ModId.of("modifiers/thanos/knockback_resistance");
	public static final ResourceLocation THANOS_SPEED = ModId.of("modifiers/thanos/speed");
	public static final ResourceLocation THANOS_REACH = ModId.of("modifiers/thanos/entity_reach");
	public static final ResourceLocation THANOS_STEP = ModId.of("modifiers/thanos/step_height");
	public static final ResourceLocation THANOS_JUMP = ModId.of("modifiers/thanos/jump_strength");
	public static final ResourceLocation THANOS_SCALE = ModId.of("modifiers/thanos/scale");

	public static final ResourceLocation REINHARD_ARMOR = ModId.of("modifiers/reinhard/armor");
	public static final ResourceLocation REINHARD_TOUGHNESS = ModId.of("modifiers/reinhard/toughness");
	public static final ResourceLocation REINHARD_DAMAGE = ModId.of("modifiers/reinhard/damage");
	public static final ResourceLocation REINHARD_SPEED = ModId.of("modifiers/reinhard/speed");
	public static final ResourceLocation REINHARD_HP = ModId.of("modifiers/reinhard/max_health");
	public static final ResourceLocation REINHARD_KNOCKBACK = ModId.of("modifiers/reinhard/knockback_resistance");
	public static final ResourceLocation REINHARD_ATTACK_SPEED = ModId.of("modifiers/reinhard/attack_speed");
	public static final ResourceLocation REINHARD_JUMP = ModId.of("modifiers/reinhard/jump_strength");
	public static final ResourceLocation REINHARD_STEP = ModId.of("modifiers/reinhard/step_height");
	public static final ResourceLocation REINHARD_REACH = ModId.of("modifiers/reinhard/entity_reach");

	public static final ResourceLocation REINHARD_PHASE_DAMAGE = ModId.of("modifiers/reinhard/phase_damage");
	public static final ResourceLocation REINHARD_PHASE_ARMOR = ModId.of("modifiers/reinhard/phase_armor");
	public static final ResourceLocation REINHARD_PHASE_TOUGHNESS = ModId.of("modifiers/reinhard/phase_toughness");
	public static final ResourceLocation REINHARD_PHASE_HP = ModId.of("modifiers/reinhard/phase_max_health");
	public static final ResourceLocation REINHARD_PHASE_SPEED = ModId.of("modifiers/reinhard/phase_speed");

	public static final ResourceLocation REINHARD_DRAW_DAMAGE = ModId.of("modifiers/reinhard/draw_damage");
	public static final ResourceLocation REINHARD_DRAW_SPEED = ModId.of("modifiers/reinhard/draw_speed");
	public static final ResourceLocation REINHARD_DRAW_JUMP = ModId.of("modifiers/reinhard/draw_jump");
	public static final ResourceLocation REINHARD_DRAW_ATTACK_SPEED = ModId.of("modifiers/reinhard/draw_attack_speed");

	public static final ResourceLocation REINHARD_SECOND_COMING_DAMAGE = ModId.of("modifiers/reinhard/second_coming_damage");
	public static final ResourceLocation REINHARD_SECOND_COMING_ARMOR = ModId.of("modifiers/reinhard/second_coming_armor");
	public static final ResourceLocation REINHARD_SECOND_COMING_TOUGHNESS = ModId.of("modifiers/reinhard/second_coming_toughness");
	public static final ResourceLocation REINHARD_SECOND_COMING_HP = ModId.of("modifiers/reinhard/second_coming_max_health");
	public static final ResourceLocation REINHARD_SECOND_COMING_SPEED = ModId.of("modifiers/reinhard/second_coming_speed");
	public static final ResourceLocation REINHARD_SECOND_COMING_KNOCKBACK = ModId.of("modifiers/reinhard/second_coming_knockback_resistance");
	public static final ResourceLocation REINHARD_SECOND_COMING_ATTACK_SPEED = ModId.of("modifiers/reinhard/second_coming_attack_speed");
	public static final ResourceLocation REINHARD_SECOND_COMING_REACH = ModId.of("modifiers/reinhard/second_coming_entity_reach");
	public static final ResourceLocation REINHARD_SECOND_COMING_STEP = ModId.of("modifiers/reinhard/second_coming_step_height");




	public static final AttributeModifierSet HOMELANDER = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, HOMELANDER_ARMOR, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, HOMELANDER_TOUGHNESS, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, HOMELANDER_DAMAGE, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, HOMELANDER_SPEED, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HOMELANDER_HP, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, HOMELANDER_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final ResourceLocation IRON_MAN_HP = ModId.of("modifiers/iron_man/max_health");

	public static final AttributeModifierSet IRON_MAN = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, IRON_MAN_ARMOR, 22.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, IRON_MAN_TOUGHNESS, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, IRON_MAN_DAMAGE, 7.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, IRON_MAN_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, IRON_MAN_HP, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, IRON_MAN_KNOCKBACK, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.build();

	/** Hulkbuster mode: больший масштаб, грубая прочность, экстра-урон, движение чуть медленнее. */
	public static final ResourceLocation HULKBUSTER_ARMOR = ModId.of("modifiers/iron_man/hulkbuster_armor");
	public static final ResourceLocation HULKBUSTER_TOUGHNESS = ModId.of("modifiers/iron_man/hulkbuster_toughness");
	public static final ResourceLocation HULKBUSTER_DAMAGE = ModId.of("modifiers/iron_man/hulkbuster_damage");
	public static final ResourceLocation HULKBUSTER_SPEED = ModId.of("modifiers/iron_man/hulkbuster_speed");
	public static final ResourceLocation HULKBUSTER_HP = ModId.of("modifiers/iron_man/hulkbuster_max_health");
	public static final ResourceLocation HULKBUSTER_SCALE = ModId.of("modifiers/iron_man/hulkbuster_scale");
	public static final ResourceLocation HULKBUSTER_REACH = ModId.of("modifiers/iron_man/hulkbuster_reach");
	public static final ResourceLocation HULKBUSTER_STEP = ModId.of("modifiers/iron_man/hulkbuster_step");

	public static final AttributeModifierSet HULKBUSTER = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, HULKBUSTER_ARMOR, 25.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, HULKBUSTER_TOUGHNESS, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, HULKBUSTER_DAMAGE, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, HULKBUSTER_SPEED, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HULKBUSTER_HP, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SCALE, HULKBUSTER_SCALE, 0.20, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, HULKBUSTER_REACH, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, HULKBUSTER_STEP, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet REGULUS = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, REGULUS_ARMOR, 70.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, REGULUS_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet REGULUS_MADNESS = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, REGULUS_MADNESS_ARMOR, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, REGULUS_MADNESS_HP, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_DAMAGE, REGULUS_MADNESS_DAMAGE, 0.40, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet SUNG_JINWOO = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, SUNG_ARMOR, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, SUNG_TOUGHNESS, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, SUNG_DAMAGE, 4.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SUNG_SPEED, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.KNOCKBACK_RESISTANCE, SUNG_KNOCKBACK, 0.3, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, SUNG_ATTACK_SPEED, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet DOOMSDAY = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, DOOMSDAY_ARMOR, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, DOOMSDAY_TOUGHNESS, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DOOMSDAY_DAMAGE, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, DOOMSDAY_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, DOOMSDAY_HP, 80.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, DOOMSDAY_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SCALE, DOOMSDAY_SCALE, 1.2, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, DOOMSDAY_REACH, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.BLOCK_INTERACTION_RANGE, DOOMSDAY_BLOCK_REACH, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, DOOMSDAY_STEP, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, DOOMSDAY_JUMP, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet GOKU = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, GOKU_ARMOR, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, GOKU_TOUGHNESS, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, GOKU_DAMAGE, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, GOKU_SPEED, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, GOKU_HP, 40.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, GOKU_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, GOKU_ATTACK_SPEED, 2.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, GOKU_JUMP, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, GOKU_STEP, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, GOKU_REACH, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet NARUTO = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, NARUTO_ARMOR, 22.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, NARUTO_TOUGHNESS, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, NARUTO_DAMAGE, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, NARUTO_SPEED, 0.55, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_SPEED, NARUTO_ATTACK_SPEED, 3.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, NARUTO_HP, 40.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, NARUTO_KNOCKBACK, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, NARUTO_JUMP, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, NARUTO_STEP, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final ResourceLocation CAP_SPEED = ModId.of("modifiers/captain_america/speed");
	public static final ResourceLocation CAP_ATTACK_SPEED = ModId.of("modifiers/captain_america/attack_speed");

	public static final AttributeModifierSet CAPTAIN_AMERICA = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, CAP_ARMOR, 18.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, CAP_TOUGHNESS, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, CAP_DAMAGE, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, CAP_ATTACK_SPEED, 2.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, CAP_HP, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, CAP_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.KNOCKBACK_RESISTANCE, CAP_KNOCKBACK, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet KRATOS = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, KRATOS_ARMOR, 18.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, KRATOS_TOUGHNESS, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, KRATOS_DAMAGE, 2.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, KRATOS_ATTACK_SPEED, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, KRATOS_HP, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KRATOS_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, KRATOS_REACH, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, KRATOS_STEP, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final ResourceLocation KRATOS_RAGE_ARMOR = ModId.of("modifiers/kratos/rage_armor");
	public static final ResourceLocation KRATOS_RAGE_TOUGHNESS = ModId.of("modifiers/kratos/rage_toughness");
	public static final ResourceLocation KRATOS_RAGE_HP = ModId.of("modifiers/kratos/rage_hp");
	public static final ResourceLocation KRATOS_RAGE_KB = ModId.of("modifiers/kratos/rage_kb");
	public static final ResourceLocation KRATOS_RAGE_FLAT = ModId.of("modifiers/kratos/rage_flat");

	public static final AttributeModifierSet KRATOS_RAGE = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, KRATOS_RAGE_FLAT, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, KRATOS_RAGE_DAMAGE, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.add(Attributes.MOVEMENT_SPEED, KRATOS_RAGE_SPEED, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ARMOR, KRATOS_RAGE_ARMOR, 15.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, KRATOS_RAGE_TOUGHNESS, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, KRATOS_RAGE_HP, 15.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KRATOS_RAGE_KB, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet LOKI = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, LOKI_ARMOR, 18.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, ModId.of("modifiers/loki/toughness"), 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, LOKI_DAMAGE, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ModId.of("modifiers/loki/attack_speed"), 2.2, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, LOKI_SPEED, 0.60, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, LOKI_HP, 24.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, LOKI_JUMP, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, LOKI_KNOCKBACK, 0.85, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SAFE_FALL_DISTANCE, ModId.of("modifiers/loki/safe_fall"), 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, ModId.of("modifiers/loki/reach"), 0.8, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet THANOS = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, THANOS_ARMOR, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, THANOS_TOUGHNESS, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, THANOS_DAMAGE, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, THANOS_HP, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, THANOS_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, THANOS_SPEED, 0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, THANOS_REACH, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, THANOS_STEP, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, THANOS_JUMP, 0.4, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SCALE, THANOS_SCALE, 0.25, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet REINHARD = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, REINHARD_ARMOR, 28.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, REINHARD_TOUGHNESS, 14.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, REINHARD_DAMAGE, 9.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, REINHARD_SPEED, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, REINHARD_HP, 60.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, REINHARD_KNOCKBACK, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, REINHARD_ATTACK_SPEED, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, REINHARD_JUMP, 0.3, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, REINHARD_STEP, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REINHARD_REACH, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static AttributeModifierSet buildReinhardPhaseSet(int phase) {
		int p = Math.max(1, Math.min(5, phase));
		double f = (p - 1) / 4.0;
		return AttributeModifierSet.builder()
				.add(Attributes.ATTACK_DAMAGE, REINHARD_PHASE_DAMAGE, lerp(0.0, 14.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ARMOR, REINHARD_PHASE_ARMOR, lerp(0.0, 24.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ARMOR_TOUGHNESS, REINHARD_PHASE_TOUGHNESS, lerp(0.0, 10.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.MAX_HEALTH, REINHARD_PHASE_HP, lerp(0.0, 60.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.MOVEMENT_SPEED, REINHARD_PHASE_SPEED, lerp(0.0, 0.40, f), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
				.build();
	}

	public static final AttributeModifierSet REINHARD_DRAW = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, REINHARD_DRAW_DAMAGE, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, REINHARD_DRAW_SPEED, 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.JUMP_STRENGTH, REINHARD_DRAW_JUMP, 0.4, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, REINHARD_DRAW_ATTACK_SPEED, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	// Второе пришествие — одноразовое возрождение Рейнхарда. Колоссальный buff к статам:
	// +900 атаки (с мечом ~1000 за удар), огромная броня/ХП/скорость/прыжок/реч.
	public static final AttributeModifierSet REINHARD_SECOND_COMING = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, REINHARD_SECOND_COMING_DAMAGE, 900.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR, REINHARD_SECOND_COMING_ARMOR, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, REINHARD_SECOND_COMING_TOUGHNESS, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, REINHARD_SECOND_COMING_HP, 200.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, REINHARD_SECOND_COMING_SPEED, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.KNOCKBACK_RESISTANCE, REINHARD_SECOND_COMING_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, REINHARD_SECOND_COMING_ATTACK_SPEED, 4.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REINHARD_SECOND_COMING_REACH, 2.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, REINHARD_SECOND_COMING_STEP, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final ResourceLocation RAIDEN_ARMOR = ModId.of("modifiers/raiden/armor");
	public static final ResourceLocation RAIDEN_TOUGHNESS = ModId.of("modifiers/raiden/toughness");
	public static final ResourceLocation RAIDEN_DAMAGE = ModId.of("modifiers/raiden/damage");
	public static final ResourceLocation RAIDEN_SPEED = ModId.of("modifiers/raiden/speed");
	public static final ResourceLocation RAIDEN_HP = ModId.of("modifiers/raiden/max_health");
	public static final ResourceLocation RAIDEN_KNOCKBACK = ModId.of("modifiers/raiden/knockback_resistance");
	public static final ResourceLocation RAIDEN_ATTACK_SPEED = ModId.of("modifiers/raiden/attack_speed");
	public static final ResourceLocation RAIDEN_BURST_DAMAGE = ModId.of("modifiers/raiden/burst_damage");
	public static final ResourceLocation RAIDEN_BURST_SPEED = ModId.of("modifiers/raiden/burst_speed");
	public static final ResourceLocation RAIDEN_BURST_ATTACK_SPEED = ModId.of("modifiers/raiden/burst_attack_speed");

	public static final AttributeModifierSet RAIDEN = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, RAIDEN_ARMOR, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, RAIDEN_TOUGHNESS, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, RAIDEN_DAMAGE, 5.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, RAIDEN_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, RAIDEN_HP, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, RAIDEN_KNOCKBACK, 0.7, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, RAIDEN_ATTACK_SPEED, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	// Бафф во время Burst (Q): +50% movement speed, +1.5 attack speed, +6 attack damage.
	public static final AttributeModifierSet RAIDEN_BURST = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, RAIDEN_BURST_DAMAGE, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, RAIDEN_BURST_SPEED, 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_SPEED, RAIDEN_BURST_ATTACK_SPEED, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final ResourceLocation INVINCIBLE_ARMOR = ModId.of("modifiers/invincible/armor");
	public static final ResourceLocation INVINCIBLE_TOUGHNESS = ModId.of("modifiers/invincible/toughness");
	public static final ResourceLocation INVINCIBLE_DAMAGE = ModId.of("modifiers/invincible/damage");
	public static final ResourceLocation INVINCIBLE_SPEED = ModId.of("modifiers/invincible/speed");
	public static final ResourceLocation INVINCIBLE_HP = ModId.of("modifiers/invincible/max_health");
	public static final ResourceLocation INVINCIBLE_KNOCKBACK = ModId.of("modifiers/invincible/knockback_resistance");
	public static final ResourceLocation INVINCIBLE_ATTACK_SPEED = ModId.of("modifiers/invincible/attack_speed");
	public static final ResourceLocation INVINCIBLE_JUMP = ModId.of("modifiers/invincible/jump_strength");
	public static final ResourceLocation INVINCIBLE_STEP = ModId.of("modifiers/invincible/step_height");
	public static final ResourceLocation OMNIMAN_ARMOR = ModId.of("modifiers/omniman/armor");
	public static final ResourceLocation OMNIMAN_TOUGHNESS = ModId.of("modifiers/omniman/toughness");
	public static final ResourceLocation OMNIMAN_DAMAGE = ModId.of("modifiers/omniman/damage");
	public static final ResourceLocation OMNIMAN_SPEED = ModId.of("modifiers/omniman/speed");
	public static final ResourceLocation OMNIMAN_HP = ModId.of("modifiers/omniman/max_health");
	public static final ResourceLocation OMNIMAN_KNOCKBACK = ModId.of("modifiers/omniman/knockback_resistance");
	public static final ResourceLocation OMNIMAN_ATTACK_SPEED = ModId.of("modifiers/omniman/attack_speed");
	public static final ResourceLocation OMNIMAN_JUMP = ModId.of("modifiers/omniman/jump_strength");
	public static final ResourceLocation OMNIMAN_STEP = ModId.of("modifiers/omniman/step_height");
	public static final ResourceLocation OMNIMAN_REACH = ModId.of("modifiers/omniman/entity_reach");

	public static final AttributeModifierSet INVINCIBLE = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, INVINCIBLE_ARMOR, 26.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, INVINCIBLE_TOUGHNESS, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, INVINCIBLE_DAMAGE, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, INVINCIBLE_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, INVINCIBLE_HP, 40.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, INVINCIBLE_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, INVINCIBLE_ATTACK_SPEED, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, INVINCIBLE_JUMP, 0.3, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, INVINCIBLE_STEP, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final AttributeModifierSet OMNIMAN = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, OMNIMAN_ARMOR, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, OMNIMAN_TOUGHNESS, 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, OMNIMAN_DAMAGE, 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, OMNIMAN_SPEED, 0.32, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, OMNIMAN_HP, 60.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, OMNIMAN_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, OMNIMAN_ATTACK_SPEED, 0.8, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, OMNIMAN_JUMP, 0.35, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, OMNIMAN_STEP, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, OMNIMAN_REACH, 0.8, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static void thanosClearStoneModifiers(net.minecraft.world.entity.LivingEntity entity) {
		for (com.example.superheroes.item.infinity.InfinityStoneType t : com.example.superheroes.item.infinity.InfinityStoneType.values()) {
			net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttribute(t.getAttribute());
			if (instance != null) {
				instance.removeModifier(t.getModifierId());
			}
		}
	}


	private HeroAttributes() {
	}

	public static AttributeModifierSet buildDoomsdayTierSet(int tier) {
		int t = Math.max(1, Math.min(7, tier));
		double f = (t - 1) / 6.0;
		return AttributeModifierSet.builder()
				.add(Attributes.ARMOR, DOOMSDAY_ARMOR, lerp(0.0, 30.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ARMOR_TOUGHNESS, DOOMSDAY_TOUGHNESS, lerp(0.0, 20.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ATTACK_DAMAGE, DOOMSDAY_DAMAGE, lerp(0.0, 20.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.MOVEMENT_SPEED, DOOMSDAY_SPEED, lerp(0.0, 0.25, f), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
				.add(Attributes.MAX_HEALTH, DOOMSDAY_HP, lerp(0.0, 80.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.KNOCKBACK_RESISTANCE, DOOMSDAY_KNOCKBACK, lerp(0.0, 1.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.SCALE, DOOMSDAY_SCALE, lerp(0.0, 1.2, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ENTITY_INTERACTION_RANGE, DOOMSDAY_REACH, lerp(0.0, 1.5, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.BLOCK_INTERACTION_RANGE, DOOMSDAY_BLOCK_REACH, lerp(0.0, 1.5, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.STEP_HEIGHT, DOOMSDAY_STEP, lerp(0.0, 1.0, f), AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.JUMP_STRENGTH, DOOMSDAY_JUMP, lerp(0.0, 0.6, f), AttributeModifier.Operation.ADD_VALUE)
				.build();
	}

	private static double lerp(double a, double b, double f) {
		return a + (b - a) * f;
	}
}
