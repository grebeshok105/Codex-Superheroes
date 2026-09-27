package io.github.grebeshok105.codex.hero.ironman;

import io.github.grebeshok105.codex.hero.ironman.ability.IronManFlightAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.IronManLegionAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.IronManNanoFormAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.IronManSuitSwitchAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.RepulsorAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.SmartMissileAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.SupersonicAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.UnibeamAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Iron Man's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 * {@link #IRON_MAN_FLIGHT} and {@link #SUPERSONIC} resolve to {@code SharedAbilityIds}:
 * the shared flight mechanic is the owner of "which ability ids map to which FlightMode".
 */
public final class IronManAbilities {
	public static final ResourceLocation IRON_MAN_FLIGHT = IronManFlightAbility.ID;
	public static final ResourceLocation SUPERSONIC = SupersonicAbility.ID;
	public static final ResourceLocation REPULSOR = RepulsorAbility.ID;
	public static final ResourceLocation UNIBEAM = UnibeamAbility.ID;
	public static final ResourceLocation IRON_MAN_SMART_MISSILE = SmartMissileAbility.ID;
	public static final ResourceLocation IRON_MAN_NANO_FORM = IronManNanoFormAbility.ID;
	public static final ResourceLocation IRON_MAN_SUIT_SWITCH = IronManSuitSwitchAbility.ID;
	public static final ResourceLocation IRON_MAN_LEGION = IronManLegionAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			IRON_MAN_FLIGHT,
			SUPERSONIC,
			REPULSOR,
			UNIBEAM,
			IRON_MAN_SMART_MISSILE,
			IRON_MAN_NANO_FORM,
			IRON_MAN_SUIT_SWITCH,
			IRON_MAN_LEGION
	);

	private IronManAbilities() {
	}
}
