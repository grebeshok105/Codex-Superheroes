package io.github.grebeshok105.codex.mechanic.ability;

import io.github.grebeshok105.codex.mechanic.flight.FlightIds;
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

	/**
	 * Hero-owned flight abilities the shared flight mechanic must recognize — the flight package
	 * owns the literals and the id→mode mapping ({@code FlightIds}); these aliases keep the
	 * mechanic-facing surface ({@code IronManFlightAbility.ID} etc. alias them in turn).
	 */
	public static final ResourceLocation IRON_MAN_FLIGHT = FlightIds.IRON_MAN_FLIGHT;
	public static final ResourceLocation SUPERSONIC = FlightIds.SUPERSONIC;

	private SharedAbilityIds() {
	}
}
