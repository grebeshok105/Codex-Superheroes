package io.github.grebeshok105.codex.hero.raiden.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Raiden's ability-scoped attribute set — moved verbatim out of
 * {@code hero/AbilityScopedModifiers} (BF3) so the hero module owns its buff.
 */
public final class RaidenModifiers {
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

	private RaidenModifiers() {
	}
}
