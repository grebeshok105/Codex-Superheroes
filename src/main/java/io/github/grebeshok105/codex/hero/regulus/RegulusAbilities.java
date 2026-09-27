package io.github.grebeshok105.codex.hero.regulus;

import io.github.grebeshok105.codex.hero.regulus.ability.CounterStrikeAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.GreedsEmbraceAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.LionHeartAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.LionRoarAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.ManiaOfGreedAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Regulus's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class RegulusAbilities {
	public static final ResourceLocation LION_HEART = LionHeartAbility.ID;
	public static final ResourceLocation MANIA_OF_GREED = ManiaOfGreedAbility.ID;
	public static final ResourceLocation GREEDS_EMBRACE = GreedsEmbraceAbility.ID;
	public static final ResourceLocation LION_ROAR = LionRoarAbility.ID;
	public static final ResourceLocation COUNTER_STRIKE = CounterStrikeAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			LION_HEART,
			MANIA_OF_GREED,
			GREEDS_EMBRACE,
			LION_ROAR,
			COUNTER_STRIKE
	);

	private RegulusAbilities() {
	}
}
