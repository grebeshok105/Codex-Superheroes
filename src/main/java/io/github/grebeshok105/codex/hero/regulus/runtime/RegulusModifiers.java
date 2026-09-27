package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Regulus's ability-scoped attribute set (BF3): exists only while madness is active and is
 * removed by id afterwards. Rows were {@code AbilityScopedModifiers.REGULUS_MADNESS_*};
 * ids stay byte-identical.
 */
public final class RegulusModifiers {
	public static final ResourceLocation REGULUS_MADNESS_ARMOR = ModId.of("modifiers/regulus/madness_armor");
	public static final ResourceLocation REGULUS_MADNESS_HP = ModId.of("modifiers/regulus/madness_max_health");
	public static final ResourceLocation REGULUS_MADNESS_DAMAGE = ModId.of("modifiers/regulus/madness_damage");

	public static final AttributeModifierSet REGULUS_MADNESS = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, REGULUS_MADNESS_ARMOR, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, REGULUS_MADNESS_HP, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ATTACK_DAMAGE, REGULUS_MADNESS_DAMAGE, 0.40, AttributeModifier.Operation.ADD_VALUE)
			.abilityScoped()
			.build();

	private RegulusModifiers() {
	}
}
