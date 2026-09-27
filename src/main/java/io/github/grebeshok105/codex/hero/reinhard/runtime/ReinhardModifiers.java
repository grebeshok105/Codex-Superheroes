package io.github.grebeshok105.codex.hero.reinhard.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Reinhard's ability-scoped attribute sets — moved verbatim out of
 * {@code hero/AbilityScopedModifiers} (BF3) so the hero module owns its buffs.
 */
public final class ReinhardModifiers {

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

	private ReinhardModifiers() {
	}

	private static double lerp(double a, double b, double f) {
		return a + (b - a) * f;
	}
}
