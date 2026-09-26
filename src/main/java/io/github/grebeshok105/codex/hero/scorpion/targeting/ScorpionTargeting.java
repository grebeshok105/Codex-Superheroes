package io.github.grebeshok105.codex.hero.scorpion.targeting;

import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.mechanic.targeting.TargetFilter;
import net.minecraft.server.level.ServerPlayer;

/** Target scans shared by Scorpion's spear, hellport and hell breath. */
public final class ScorpionTargeting {
	private ScorpionTargeting() {
	}

	/**
	 * The pre-module {@code TargetFilters.hostileTo} predicate as a {@link TargetFilter}:
	 * living, not the owner, not a spectator, harmable under PvP/team rules, not a creative player.
	 */
	public static TargetFilter hostileTo(ServerPlayer owner) {
		return TargetFilter.of(owner).withoutOwner().alive().withoutSpectators()
				.and(candidate -> !(candidate instanceof ServerPlayer victim && !victim.canHarmPlayer(owner)))
				.and(TargetFilters::notCreative);
	}
}
