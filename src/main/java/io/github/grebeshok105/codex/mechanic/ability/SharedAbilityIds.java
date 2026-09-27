package io.github.grebeshok105.codex.mechanic.ability;

import io.github.grebeshok105.codex.ModId;
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
	 * Hero-owned flight abilities the shared flight mechanic must recognize — the mechanic is the
	 * single owner of "which ids map to which {@code FlightMode}", so the literals live here and
	 * the hero ability classes alias them ({@code IronManFlightAbility.ID} etc.).
	 */
	public static final ResourceLocation IRON_MAN_FLIGHT = ModId.of("iron_man_flight");
	public static final ResourceLocation SUPERSONIC = ModId.of("supersonic");

	private SharedAbilityIds() {
	}
}
