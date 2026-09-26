package io.github.grebeshok105.codex.bootstrap;

import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.command.AdminBuildSyncController;
import io.github.grebeshok105.codex.core.lifecycle.HeroTickDispatcher;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.AutoSaturationController;
import io.github.grebeshok105.codex.effect.FlightController;
import io.github.grebeshok105.codex.effect.HeavensStrikeController;
import io.github.grebeshok105.codex.effect.HeroEquipmentLock;
import io.github.grebeshok105.codex.effect.HeroLandingTracker;
import io.github.grebeshok105.codex.effect.HeroMeleeImpactController;
import io.github.grebeshok105.codex.effect.HeroPassiveRegenController;
import io.github.grebeshok105.codex.effect.HeroReactionController;
import io.github.grebeshok105.codex.effect.SuperJumpController;
import io.github.grebeshok105.codex.horde.HordeManager;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.PassiveReconciler;
import io.github.grebeshok105.codex.physics.BallisticBodyTracker;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;

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
		// Core lifecycle rows that used to open SuperheroesMod.registerPlayerLifecycle.
		// That table ran BEFORE every hero module — so the shared rows that started
		// each hook list stay here in register (pre-modules), while the rows that
		// closed a list (cooldown sync/clear, EntityControlLock hero-clear) live in
		// registerPost. Rows in between are hero-owned and moved to their modules.
		ctx.lifecycle().onJoin(HeroTransformService::onPlayerJoin);
		ctx.lifecycle().onJoin(HeroDataStore::syncPublicHero);
		ctx.lifecycle().onLeave(HeroTransformService::onPlayerLeave);
		ctx.lifecycle().onLeave(EntityControlLock::releaseOwnedBy);
		ctx.lifecycle().onDeath(EntityControlLock::releaseOwnedBy);
		ctx.lifecycle().onRespawn(HeroTransformService::onPlayerRespawn);
		// Cross-hero reaction broadcast (Homelander <-> Omniman) — global by design.
		ctx.lifecycle().onHeroTransformed(HeroReactionController::onTransformed);
		ctx.lifecycle().onServerStopped(server -> HordeManager.resetAll());
		ctx.lifecycle().onServerStopped(server -> EnergyLocks.resetAll());

		HeroLandingTracker.register(ctx);
		ctx.ticks().global(HeroLandingTracker::pruneGonePlayers);
		HeroEquipmentLock.register(ctx);
		ctx.ticks().player(HeroEquipmentLock::tickPlayer);
		ctx.ticks().player(SuperJumpController::tickPlayer);
		ctx.ticks().player(AutoSaturationController::tickPlayer);
		ctx.ticks().player(HeroPassiveRegenController::tickPlayer);
		ctx.ticks().global(HeroMeleeImpactController::serverTick);
		ctx.ticks().global(BallisticBodyTracker::tick);
		ctx.ticks().global(FlightController::cleanup);
		ctx.ticks().global(HeavensStrikeController::serverTick);
		// content rows kept here until their own stage
		ctx.ticks().level((server, level) -> HordeManager.tick(level));
		AdminBuildSyncController.register(ctx);
	}

	/**
	 * Shared wiring that must run AFTER the hero modules. Two old orders are kept here:
	 * AttackEntity listeners — IronFists@64 < ... < MeleeImpact@67 — so IronFists'
	 * consuming result still short-circuits the generic melee-impact handler;
	 * player ticks — Unibeam@345 -> Landing@347 -> Flight@380 — so Landing reads
	 * UnibeamController.isBusy fresh and ViltrumiteCharge/Rush read
	 * FlightController.isFlightActive before Flight's own player tick refreshed it.
	 */
	public static void registerPost(HeroModuleContext ctx) {
		HeroMeleeImpactController.register(ctx);
		ctx.ticks().player(HeroLandingTracker::tickPlayer);
		ctx.ticks().player(FlightController::tickPlayer);

		// C4: recompute the synced ability_availability attachment after all hero ticks
		// (writes only on change) — was the last row of the old PLAYERS table.
		ctx.ticks().player(io.github.grebeshok105.codex.ability.AbilityAvailabilitySync::tickPlayer);

		// Core lifecycle rows that closed their lists in the old table — they keep
		// running after every hero-owned hook (registerPost is called post-modules).
		ctx.lifecycle().onJoin(AbilityCooldowns::syncAll);
		ctx.lifecycle().onDeath(AbilityCooldowns::clearAndSync);
		ctx.lifecycle().onHeroClear(EntityControlLock::releaseOwnedBy);

		// The two rows of the old registerTickHandlers() — they used to register after
		// all module ticks, so they sit at the end of the post-module call.
		// ResourceController.tick stays the last player-phase tick, after AASync.
		ctx.ticks().global(PassiveReconciler::serverTick);
		ctx.ticks().player((server, p, data) -> ResourceController.tick(p));
	}
}
