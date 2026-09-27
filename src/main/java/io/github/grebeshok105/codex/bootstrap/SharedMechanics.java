package io.github.grebeshok105.codex.bootstrap;

import io.github.grebeshok105.codex.compat.falbiks.FalbiksSnapCompat;
import io.github.grebeshok105.codex.core.ability.AbilityAvailabilitySync;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.lifecycle.HeroTickDispatcher;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.net.ActivateAbilityC2SPayload;
import io.github.grebeshok105.codex.core.net.BindAbilityResourceC2SPayload;
import io.github.grebeshok105.codex.core.net.HeroMeleeChargeC2SPayload;
import io.github.grebeshok105.codex.core.net.SuperJumpC2SPayload;
import io.github.grebeshok105.codex.mechanic.flight.FlightStateS2CPayload;
import io.github.grebeshok105.codex.mechanic.passive.AutoSaturationController;
import io.github.grebeshok105.codex.mechanic.flight.FlightController;
import io.github.grebeshok105.codex.mechanic.passive.HeroEquipmentLock;
import io.github.grebeshok105.codex.mechanic.falls.HeroLandingTracker;
import io.github.grebeshok105.codex.mechanic.impact.HeroMeleeImpactController;
import io.github.grebeshok105.codex.mechanic.passive.HeroPassiveRegenController;
import io.github.grebeshok105.codex.mechanic.falls.SuperJumpController;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.PassiveReconciler;
import io.github.grebeshok105.codex.mechanic.impact.BallisticBodyTracker;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;

/**
 * Wiring for hero-agnostic mechanics (composition-root side, after E2 moves these to
 * {@code mechanic/}): the registrations that used to live in SuperheroesMod —
 * each controller's own event listeners plus its HeroTickDispatcher rows, in the
 * old relative order. Content-track rows (admin build sync) park here until
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
		// The ability layer's onDeactivate sweep behind transform's seam — transform may not
		// reach into core.ability, so the registration lands here at the composition root.
		HeroTransformService.abilityDeactivator(AbilityRouter::deactivateAll);

		ctx.lifecycle().onJoin(HeroDataStore::syncPublicHero);
		ctx.lifecycle().onLeave(HeroTransformService::onPlayerLeave);
		ctx.lifecycle().onLeave(EntityControlLock::releaseOwnedBy);
		ctx.lifecycle().onDeath(EntityControlLock::releaseOwnedBy);
		ctx.lifecycle().onRespawn(HeroTransformService::onPlayerRespawn);

		// Shared payload wiring: the receivers live on classes core.net may not depend on,
		// so they register through the module context.
		ctx.payloads().c2s(ActivateAbilityC2SPayload.TYPE, ActivateAbilityC2SPayload.STREAM_CODEC,
				(payload, context) -> AbilityRouter.activate(context.player(), payload.abilityId()));
		ctx.payloads().c2s(BindAbilityResourceC2SPayload.TYPE, BindAbilityResourceC2SPayload.STREAM_CODEC,
				(payload, context) -> AbilityRouter.bind(context.player(), payload.abilityId(), payload.kind()));
		ctx.payloads().c2s(SuperJumpC2SPayload.TYPE, SuperJumpC2SPayload.STREAM_CODEC,
				(payload, context) -> SuperJumpController.activate(context.player()));
		ctx.payloads().c2s(HeroMeleeChargeC2SPayload.TYPE, HeroMeleeChargeC2SPayload.STREAM_CODEC,
				(payload, context) -> HeroMeleeImpactController.handleChargeInput(context.player(), payload));
		ctx.payloads().s2c(FlightStateS2CPayload.TYPE, FlightStateS2CPayload.STREAM_CODEC);

		ctx.ticks().global(HeroLandingTracker::pruneGonePlayers);
		HeroEquipmentLock.register(ctx);
		ctx.ticks().player(HeroEquipmentLock::tickPlayer);
		ctx.ticks().player(SuperJumpController::tickPlayer);
		ctx.ticks().player(AutoSaturationController::tickPlayer);
		ctx.ticks().player(HeroPassiveRegenController::tickPlayer);
		ctx.ticks().global(HeroMeleeImpactController::serverTick);
		ctx.ticks().global(BallisticBodyTracker::tick);
		ctx.ticks().global(FlightController::cleanup);

		// compat bridges subscribe their CrossModHooks listeners here — compat sees
		// only the seam, hero code fires it.
		FalbiksSnapCompat.init();
	}

	/**
	 * Shared wiring that must run AFTER the hero modules. Two old orders are kept here:
	 * AttackEntity listeners — IronFists@64 < ... < MeleeImpact@67 — so IronFists'
	 * consuming result still short-circuits the generic melee-impact handler;
	 * player ticks — Unibeam@345 -> Landing@347 -> Flight@380 — so Landing reads
	 * IronManHero.suppressesLanding (UnibeamController.isBusy) fresh and ViltrumiteCharge/Rush read
	 * FlightController.isFlightActive before Flight's own player tick refreshed it.
	 */
	public static void registerPost(HeroModuleContext ctx) {
		HeroMeleeImpactController.register(ctx);
		ctx.ticks().player(HeroLandingTracker::tickPlayer);
		ctx.ticks().player(FlightController::tickPlayer);

		// C4: recompute the synced ability_availability attachment after all hero ticks
		// (writes only on change) — was the last row of the old PLAYERS table.
		ctx.ticks().player(AbilityAvailabilitySync::tickPlayer);

		// Core lifecycle rows that closed their lists in the old table — they keep
		// running after every hero-owned hook (registerPost is called post-modules).
		ctx.lifecycle().onJoin(AbilityCooldowns::syncAll);
		ctx.lifecycle().onDeath(AbilityCooldowns::clearAndSync);
		ctx.lifecycle().onHeroClear(EntityControlLock::releaseOwnedBy);

		// The two rows of the old registerTickHandlers() — they used to register after
		// all module ticks, so they sit at the end of the post-module call.
		// ResourceController.tick + AbilityRouter.tickActive stay the last player-phase ticks,
		// after AASync; regen lands before the per-ability drain like the old single row did.
		ctx.ticks().global(PassiveReconciler::serverTick);
		ctx.ticks().player((server, p, data) -> ResourceController.tick(p));
		ctx.ticks().player((server, p, data) -> AbilityRouter.tickActive(p));
	}
}
