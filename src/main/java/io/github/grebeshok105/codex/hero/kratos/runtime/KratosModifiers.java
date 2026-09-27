package io.github.grebeshok105.codex.hero.kratos.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Kratos's transient, ability-scoped attribute set: the Spartan Rage buff that exists only
 * while the ability is active and is removed by id afterwards (moved out of
 * {@code AbilityScopedModifiers}; the {@code modifiers/kratos/*} ids stay byte-identical).
 */
public final class KratosModifiers {
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

	private KratosModifiers() {
	}
}
