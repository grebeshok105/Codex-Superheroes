package io.github.grebeshok105.codex.hero.doomsday.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Doomsday's attribute modifier ids and the tier-scaled set (moved out of
 * {@code AbilityScopedModifiers}; the {@code modifiers/doomsday/*} ids stay byte-identical).
 * {@code DOOMSDAY} is the tier-7 ceiling set — applied as a passive, then
 * {@link #buildDoomsdayTierSet} re-derives per-tier amounts on top of it.
 */
public final class DoomsdayModifiers {
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

	private DoomsdayModifiers() {
	}

	private static double lerp(double a, double b, double f) {
		return a + (b - a) * f;
	}
}
