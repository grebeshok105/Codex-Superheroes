package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.Hero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Transient, ability-scoped attribute sets (BF3): buffs that exist only while an
 * ability, rage or hero phase is active and are removed by id afterwards.
 * Permanent passive sets live on the hero classes behind {@code Hero#passiveAttributes()}.
 */
public final class AbilityScopedModifiers {
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

	public static void thanosClearStoneModifiers(net.minecraft.world.entity.LivingEntity entity) {
		for (io.github.grebeshok105.codex.item.infinity.InfinityStoneType t : io.github.grebeshok105.codex.item.infinity.InfinityStoneType.values()) {
			net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttribute(t.getAttribute());
			if (instance != null) {
				instance.removeModifier(t.getModifierId());
			}
		}
	}

	private AbilityScopedModifiers() {
	}
}
