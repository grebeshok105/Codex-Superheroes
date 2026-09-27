package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.github.grebeshok105.codex.mechanic.flight.FlightController;
import io.github.grebeshok105.codex.mechanic.flight.FlightModifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Homelander's flight hooks: uranium pressure gates and grounds him, milk madness
 * lifts the gate and boosts tuning. Registered from {@code HomelanderModule}; the
 * mechanic classes in {@code mechanic.flight} stay hero-agnostic.
 */
public final class HomelanderFlightModifier implements FlightModifier {

	@Override
	public boolean denyActivation(ServerPlayer player, HeroData data) {
		return !HomelanderEffects.isMadness(player)
				&& UraniumDefenseController.isUnderUraniumThreat(player)
				&& FlightController.isOnCooldown(player);
	}

	@Override
	public boolean restrictsFlight(ServerPlayer player, HeroData data) {
		return !HomelanderEffects.isMadness(player)
				&& UraniumDefenseController.isUnderUraniumThreat(player);
	}

	@Override
	public boolean boosted(Player player) {
		return HomelanderEffects.isMadness(player);
	}
}
