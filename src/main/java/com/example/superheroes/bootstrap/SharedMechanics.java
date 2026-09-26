package com.example.superheroes.bootstrap;

import com.example.superheroes.command.AdminBuildSyncController;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.AutoSaturationController;
import com.example.superheroes.effect.FlightController;
import com.example.superheroes.effect.HeavensStrikeController;
import com.example.superheroes.effect.HeroEquipmentLock;
import com.example.superheroes.effect.HeroLandingTracker;
import com.example.superheroes.effect.HeroMeleeImpactController;
import com.example.superheroes.effect.HeroPassiveRegenController;
import com.example.superheroes.effect.SuperJumpController;
import com.example.superheroes.horde.HordeManager;
import com.example.superheroes.physics.BallisticBodyTracker;

/**
 * Wiring for hero-agnostic mechanics (composition-root side, after E2 moves these to
 * {@code mechanic/}): the registrations that used to live in SuperheroesMod —
 * each controller's own event listeners plus its HeroTickDispatcher rows, in the
 * old relative order. Content-track rows (horde, admin build sync) park here until
 * stage IC gives them a home.
 */
public final class SharedMechanics {

	private SharedMechanics() {
	}

	public static void register(HeroModuleContext ctx) {
		HeroLandingTracker.register(ctx);
		ctx.ticks().global(HeroLandingTracker::pruneGonePlayers);
		HeroEquipmentLock.register(ctx);
		ctx.ticks().player(HeroEquipmentLock::tickPlayer);
		ctx.ticks().player(SuperJumpController::tickPlayer);
		ctx.ticks().player(AutoSaturationController::tickPlayer);
		ctx.ticks().player(HeroPassiveRegenController::tickPlayer);
		HeroMeleeImpactController.register(ctx);
		ctx.ticks().global(HeroMeleeImpactController::serverTick);
		ctx.ticks().global(BallisticBodyTracker::tick);
		ctx.ticks().global(FlightController::cleanup);
		ctx.ticks().global(HeavensStrikeController::serverTick);
		// content rows kept here until their own stage
		ctx.ticks().level((server, level) -> HordeManager.tick(level));
		AdminBuildSyncController.register(ctx);
	}

	/**
	 * Shared player ticks that must run AFTER the hero modules, preserving the old
	 * registerTickHandlers order: Unibeam@345 -> Landing@347 -> Flight@380.
	 * Landing reads UnibeamController.isBusy (written by Unibeam's player tick);
	 * ViltrumiteCharge/Rush read FlightController.isFlightActive before Flight's
	 * own player tick refreshed it — both reads keep their old freshness here.
	 */
	public static void registerPost(HeroModuleContext ctx) {
		ctx.ticks().player(HeroLandingTracker::tickPlayer);
		ctx.ticks().player(FlightController::tickPlayer);
	}
}
