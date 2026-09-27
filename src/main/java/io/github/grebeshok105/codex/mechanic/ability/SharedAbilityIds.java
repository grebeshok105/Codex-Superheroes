package io.github.grebeshok105.codex.mechanic.ability;

import net.minecraft.resources.ResourceLocation;

/**
 * Ids of the shared abilities registered by {@code bootstrap.SharedAbilities}.
 * Heroes list these in {@code getAbilities()} by alias so the mechanic classes stay
 * the single owner of each persisted id string.
 */
public final class SharedAbilityIds {
	public static final ResourceLocation FLIGHT = FlightAbility.ID;
	public static final ResourceLocation VILTRUMITE_RECOVERY = ViltrumiteRecoveryAbility.ID;
	public static final ResourceLocation VILTRUMITE_CHARGE = ViltrumiteChargeAbility.ID;

	private SharedAbilityIds() {
	}
}
