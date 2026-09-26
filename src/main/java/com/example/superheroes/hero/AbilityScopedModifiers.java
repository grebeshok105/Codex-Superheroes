package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Transient, ability-scoped attribute sets (BF3): buffs that exist only while an
 * ability, rage or hero phase is active and are removed by id afterwards.
 * Permanent passive sets live on the hero classes behind {@code Hero#passiveAttributes()}.
 */
public final class AbilityScopedModifiers {
	public static final ResourceLocation KRATOS_RAGE_DAMAGE = ModId.of("modifiers/kratos/rage_damage");
	public static final ResourceLocation KRATOS_RAGE_SPEED = ModId.of("modifiers/kratos/rage_speed");
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
			.abilityScoped()
			.build();

	/** Нано-клинок: чистый бонус к урону ближнего боя, пока активна форма клинка. */
	public static final ResourceLocation NANO_BLADE_DAMAGE = ModId.of("modifiers/iron_man/nano_blade_damage");

	public static final AttributeModifierSet NANO_BLADE = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, NANO_BLADE_DAMAGE, 7.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	/** Нано-щит: жёсткая стойкость, пока активна форма щита. */
	public static final ResourceLocation NANO_SHIELD_ARMOR = ModId.of("modifiers/iron_man/nano_shield_armor");
	public static final ResourceLocation NANO_SHIELD_TOUGHNESS = ModId.of("modifiers/iron_man/nano_shield_toughness");
	public static final ResourceLocation NANO_SHIELD_KNOCKBACK = ModId.of("modifiers/iron_man/nano_shield_knockback");

	public static final AttributeModifierSet NANO_SHIELD = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, NANO_SHIELD_ARMOR, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, NANO_SHIELD_TOUGHNESS, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, NANO_SHIELD_KNOCKBACK, 0.4, AttributeModifier.Operation.ADD_VALUE)
			.build();

	public static final ResourceLocation REGULUS_MADNESS_ARMOR = ModId.of("modifiers/regulus/madness_armor");
	public static final ResourceLocation REGULUS_MADNESS_HP = ModId.of("modifiers/regulus/madness_max_health");
	public static final ResourceLocation REGULUS_MADNESS_DAMAGE = ModId.of("modifiers/regulus/madness_damage");

	public static final AttributeModifierSet REGULUS_MADNESS = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, REGULUS_MADNESS_ARMOR, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, REGULUS_MADNESS_HP, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_DAMAGE, REGULUS_MADNESS_DAMAGE, 0.40, AttributeModifier.Operation.ADD_VALUE)
			.abilityScoped()
			.build();

	public static final ResourceLocation REINHARD_PHASE_DAMAGE = ModId.of("modifiers/reinhard/phase_damage");
	public static final ResourceLocation REINHARD_PHASE_ARMOR = ModId.of("modifiers/reinhard/phase_armor");
	public static final ResourceLocation REINHARD_PHASE_TOUGHNESS = ModId.of("modifiers/reinhard/phase_toughness");
	public static final ResourceLocation REINHARD_PHASE_HP = ModId.of("modifiers/reinhard/phase_max_health");
	public static final ResourceLocation REINHARD_PHASE_SPEED = ModId.of("modifiers/reinhard/phase_speed");

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

	public static final ResourceLocation REINHARD_DRAW_DAMAGE = ModId.of("modifiers/reinhard/draw_damage");
	public static final ResourceLocation REINHARD_DRAW_SPEED = ModId.of("modifiers/reinhard/draw_speed");
	public static final ResourceLocation REINHARD_DRAW_JUMP = ModId.of("modifiers/reinhard/draw_jump");
	public static final ResourceLocation REINHARD_DRAW_ATTACK_SPEED = ModId.of("modifiers/reinhard/draw_attack_speed");

	public static final AttributeModifierSet REINHARD_DRAW = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, REINHARD_DRAW_DAMAGE, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, REINHARD_DRAW_SPEED, 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.JUMP_STRENGTH, REINHARD_DRAW_JUMP, 0.4, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, REINHARD_DRAW_ATTACK_SPEED, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.abilityScoped()
			.build();

	public static final ResourceLocation REINHARD_SECOND_COMING_DAMAGE = ModId.of("modifiers/reinhard/second_coming_damage");
	public static final ResourceLocation REINHARD_SECOND_COMING_ARMOR = ModId.of("modifiers/reinhard/second_coming_armor");
	public static final ResourceLocation REINHARD_SECOND_COMING_TOUGHNESS = ModId.of("modifiers/reinhard/second_coming_toughness");
	public static final ResourceLocation REINHARD_SECOND_COMING_HP = ModId.of("modifiers/reinhard/second_coming_max_health");
	public static final ResourceLocation REINHARD_SECOND_COMING_SPEED = ModId.of("modifiers/reinhard/second_coming_speed");
	public static final ResourceLocation REINHARD_SECOND_COMING_KNOCKBACK = ModId.of("modifiers/reinhard/second_coming_knockback_resistance");
	public static final ResourceLocation REINHARD_SECOND_COMING_ATTACK_SPEED = ModId.of("modifiers/reinhard/second_coming_attack_speed");
	public static final ResourceLocation REINHARD_SECOND_COMING_REACH = ModId.of("modifiers/reinhard/second_coming_entity_reach");
	public static final ResourceLocation REINHARD_SECOND_COMING_STEP = ModId.of("modifiers/reinhard/second_coming_step_height");

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

	public static final ResourceLocation RAIDEN_BURST_DAMAGE = ModId.of("modifiers/raiden/burst_damage");
	public static final ResourceLocation RAIDEN_BURST_SPEED = ModId.of("modifiers/raiden/burst_speed");
	public static final ResourceLocation RAIDEN_BURST_ATTACK_SPEED = ModId.of("modifiers/raiden/burst_attack_speed");

	// Бафф во время Burst (Q): +50% movement speed, +1.5 attack speed, +6 attack damage.
	public static final AttributeModifierSet RAIDEN_BURST = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, RAIDEN_BURST_DAMAGE, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, RAIDEN_BURST_SPEED, 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_SPEED, RAIDEN_BURST_ATTACK_SPEED, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.abilityScoped()
			.build();

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

	public static void thanosClearStoneModifiers(net.minecraft.world.entity.LivingEntity entity) {
		for (com.example.superheroes.item.infinity.InfinityStoneType t : com.example.superheroes.item.infinity.InfinityStoneType.values()) {
			net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttribute(t.getAttribute());
			if (instance != null) {
				instance.removeModifier(t.getModifierId());
			}
		}
	}

	private AbilityScopedModifiers() {
	}

	private static double lerp(double a, double b, double f) {
		return a + (b - a) * f;
	}
}
