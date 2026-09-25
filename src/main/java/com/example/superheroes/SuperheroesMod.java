package com.example.superheroes;

import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.command.SuperheroesCommands;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.item.ModItemGroups;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.lifecycle.EntityControlLock;
import com.example.superheroes.lifecycle.PlayerLifecycle;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.resource.ResourceController;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroDataStore;
import com.example.superheroes.transform.HeroTransformService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperheroesMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(ModId.MOD_ID);

	@Override
	public void onInitialize() {
		ModAttachments.init();
		EntityControlLock.init();
		PlayerLifecycle.init();
		com.example.superheroes.lifecycle.PassiveReconciler.init();
		registerPlayerLifecycle();
		ModEffects.init();
		Heroes.init();
		AbilityRegistry.init();
		com.example.superheroes.entity.ModEntities.init();
		com.example.superheroes.horde.entity.HordeEntities.init();
		com.example.superheroes.item.ModDataComponents.init();
		ModItems.init();
		ModItemGroups.init();
		ModParticles.init();
		ModSounds.init();
		com.example.superheroes.effect.MirrorDimensionController.init();
		ModNetworking.init();
		HeroDataStore.init();
		com.example.superheroes.effect.MadnessFlightController.init();
		com.example.superheroes.effect.HeroLandingTracker.init();
		com.example.superheroes.effect.HeroEquipmentLock.init();
		com.example.superheroes.effect.IronManJarvisController.init();
		com.example.superheroes.ability.ironman.IronManNanoFormController.init();
		com.example.superheroes.effect.RegulusTotemController.init();
		com.example.superheroes.effect.RegulusGreedController.init();
		com.example.superheroes.effect.RegulusMadnessController.init();
		com.example.superheroes.effect.IronFistsController.init();
		com.example.superheroes.effect.InvincibleCombatController.init();
		com.example.superheroes.effect.OmnimanMomentumController.init();
		com.example.superheroes.effect.HeroMeleeImpactController.init();
		com.example.superheroes.effect.RemDemonismController.init();
		com.example.superheroes.effect.SungJinwooController.init();
		com.example.superheroes.effect.DoomsdayAdaptationController.init();
		com.example.superheroes.effect.DoomsdayTierController.init();
		com.example.superheroes.effect.GokuKiStackController.init();
		com.example.superheroes.effect.KawarimiController.init();
		com.example.superheroes.effect.ThanosGauntletStateController.init();
		com.example.superheroes.ability.ironman.IronManSuitSyncController.init();
		com.example.superheroes.command.AdminBuildSyncController.init();
		com.example.superheroes.effect.KratosRageController.init();
		com.example.superheroes.effect.KratosHandStrikeFxController.init();
		com.example.superheroes.effect.DoomsdayKryptoniteController.init();
		com.example.superheroes.effect.ThanosStoneRewardController.init();
		com.example.superheroes.effect.ReinhardTimeSlowController.init();
		com.example.superheroes.effect.ReinhardController.init();
		com.example.superheroes.effect.ReinhardSwordDeathMarkController.init();
		com.example.superheroes.effect.RaidenPlungingLandingController.init();
		SuperheroesCommands.init();

		registerTickHandlers();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(com.example.superheroes.lifecycle.HeroTickDispatcher::tick);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			// Pandora never dies — lethal hits trigger her cinematic instead (#7).
			return com.example.superheroes.effect.PandoraDeathController.allowDamage(entity, source, amount);
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer serverPlayer) {
				return com.example.superheroes.effect.PandoraDeathController.allowDeath(serverPlayer, source);
			}
			return true;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer serverPlayer) {
				com.example.superheroes.transform.HeroData data = serverPlayer
						.getAttachedOrCreate(com.example.superheroes.attachment.ModAttachments.HERO_DATA);
				if (data.hasHero()
						&& com.example.superheroes.hero.DoomsdayHero.ID.equals(data.heroId())) {
					return;
				}
				HeroTransformService.forceUntransform(serverPlayer);
			}
		});

		LOGGER.info("Superheroes mod initialized");
	}

	/**
	 * Every controller cleanup routed through {@link PlayerLifecycle} — the single dispatch
	 * point wired to server-thread events ({@code LEAVE} fires before the player is saved and
	 * removed, unlike {@code DISCONNECT} which may run on the Netty thread).
	 */
	private static void registerPlayerLifecycle() {
		// join — server thread; reconcile session-scoped state on the relogged entity.
		PlayerLifecycle.onJoin(HeroTransformService::onPlayerJoin);
		PlayerLifecycle.onJoin(HeroDataStore::syncPublicHero);

		// hero clear — hero-scoped session state dropped on swap/untransform/leave/death.
		// Ordering is visible here; add new clears to this table (audit debt 1).
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(p -> com.example.superheroes.effect.UnibeamController.clearState(p.getUUID()));
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(p -> com.example.superheroes.effect.RegulusTotemController.clear(p.getUUID()));
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.effect.RegulusMadnessController::clearMadness);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.effect.ReinhardController::clearAdaptations);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.effect.RaidenLifecycleController::clearOnUntransform);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.effect.RemDemonismController::clear);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.effect.PandoraDeathController::resetOnHeroTaken);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.effect.DoomGripController::clear);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(com.example.superheroes.ability.OmnimanThinkMarkAbility::clear);
		com.example.superheroes.lifecycle.HeroLifecycle.onClear(EntityControlLock::releaseOwnedBy);
		com.example.superheroes.lifecycle.HeroLifecycle.onTransformed(com.example.superheroes.effect.HeroReactionController::onTransformed);
		PlayerLifecycle.onJoin(com.example.superheroes.effect.ReinhardController::onPlayerJoin);
		PlayerLifecycle.onJoin(com.example.superheroes.effect.BattleBeastCurseController::reapplyOnJoin);
		PlayerLifecycle.onJoin(com.example.superheroes.effect.RegulusMadnessController::clearMadness);
		PlayerLifecycle.onJoin(com.example.superheroes.ability.AbilityCooldowns::syncAll);

		// leave — drop session-scoped state; never runs gameplay deactivate side-effects.
		PlayerLifecycle.onLeave(HeroTransformService::onPlayerLeave);
		PlayerLifecycle.onLeave(EntityControlLock::releaseOwnedBy);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.PandoraDeathController::onPlayerLeave);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.DoomGripController::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.OmnimanThinkMarkAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.ReinhardSwordDrawCeremonyController::cancelCeremony);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.RegulusGreedController::onPlayerGone);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.RegulusMadnessController::clearMadness);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.KratosRageController::onPlayerGone);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.ReinhardTimeSlowController::onPlayerGone);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.ThanosGauntletStateController::onPlayerReset);
		PlayerLifecycle.onLeave(p -> com.example.superheroes.effect.UnibeamController.clearState(p.getUUID()));
		PlayerLifecycle.onLeave(p -> com.example.superheroes.effect.MonarchsDomainController.clear(p.getUUID()));
		PlayerLifecycle.onLeave(p -> com.example.superheroes.effect.ThanosSnapWindupController.cancel(p.getUUID()));
		PlayerLifecycle.onLeave(p -> com.example.superheroes.effect.ReinhardSwordDeathMarkController.cancelVictim(p.getUUID()));
		PlayerLifecycle.onLeave(com.example.superheroes.ability.RepulsorChargeController::reset);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.ChargeTackleAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.ViltrumiteChargeAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.OmnimanViltrumiteRushAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.GokuKamehamehaAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.GokuSpiritBombAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.NarutoRasenganAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.NarutoOodamaRasenganAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.NarutoRasenshurikenAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.CapShieldSlamAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.MirrorDimensionController::onPlayerGone);
		PlayerLifecycle.onLeave(com.example.superheroes.effect.SpatialBindController::onPlayerGone);

		// death — charge/lock/session state must not outlive the entity (audit B17).
		PlayerLifecycle.onDeath(EntityControlLock::releaseOwnedBy);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.DoomGripController::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.OmnimanThinkMarkAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.ReinhardSwordDrawCeremonyController::cancelCeremony);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.RegulusGreedController::onPlayerGone);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.RegulusMadnessController::clearMadness);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.KratosRageController::onPlayerGone);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.ReinhardTimeSlowController::onPlayerGone);
		PlayerLifecycle.onDeath(com.example.superheroes.effect.ThanosGauntletStateController::onPlayerReset);
		PlayerLifecycle.onDeath(p -> com.example.superheroes.effect.UnibeamController.clearState(p.getUUID()));
		PlayerLifecycle.onDeath(p -> com.example.superheroes.effect.MonarchsDomainController.clear(p.getUUID()));
		PlayerLifecycle.onDeath(p -> com.example.superheroes.effect.ThanosSnapWindupController.cancel(p.getUUID()));
		PlayerLifecycle.onDeath(p -> com.example.superheroes.effect.ReinhardSwordDeathMarkController.cancelVictim(p.getUUID()));
		PlayerLifecycle.onDeath(com.example.superheroes.ability.RepulsorChargeController::reset);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.ChargeTackleAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.ViltrumiteChargeAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.OmnimanViltrumiteRushAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.GokuKamehamehaAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.GokuSpiritBombAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.NarutoRasenganAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.NarutoOodamaRasenganAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.NarutoRasenshurikenAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.CapShieldSlamAbility::clear);
		PlayerLifecycle.onDeath(com.example.superheroes.ability.AbilityCooldowns::clearAndSync);

		// respawn — reconcile the fresh entity with state that outlives death.
		PlayerLifecycle.onRespawn(HeroTransformService::onPlayerRespawn);
		PlayerLifecycle.onRespawn(com.example.superheroes.effect.ThanosGauntletStateController::onPlayerReset);
		PlayerLifecycle.onRespawn(com.example.superheroes.effect.RegulusMadnessController::clearMadness);

		// world shutdown — static session state must not leak into a new world (audit B8).
		PlayerLifecycle.onServerStopped(server -> {
			com.example.superheroes.horde.HordeManager.resetAll();
			com.example.superheroes.effect.SungJinwooController.resetAll();
			com.example.superheroes.effect.MonarchsDomainController.resetAll();
			com.example.superheroes.resource.EnergyLocks.resetAll();
			com.example.superheroes.effect.RemDemonismController.resetAll();
			com.example.superheroes.effect.UnibeamController.resetAll();
			com.example.superheroes.effect.ThanosSnapWindupController.resetAll();
			com.example.superheroes.effect.ReinhardSwordDeathMarkController.resetAll();
			com.example.superheroes.effect.ReinhardSwordDrawCeremonyController.resetAll();
			com.example.superheroes.effect.RegulusGreedController.resetAll();
			com.example.superheroes.effect.RegulusMadnessController.resetAll();
			com.example.superheroes.effect.ReinhardTimeSlowController.resetAll(server);
			com.example.superheroes.effect.DoomGripController.resetAll();
			com.example.superheroes.ability.OmnimanThinkMarkAbility.resetAll();
			com.example.superheroes.effect.KratosRageController.resetAll();
			com.example.superheroes.effect.BattleBeastCurseController.resetAll();
			com.example.superheroes.effect.ThanosGauntletStateController.resetAll();
			com.example.superheroes.effect.PandoraDeathController.resetAll();
			com.example.superheroes.ability.RepulsorChargeController.resetAll();
			com.example.superheroes.ability.ChargeTackleAbility.resetAll();
			com.example.superheroes.ability.ViltrumiteChargeAbility.resetAll();
			com.example.superheroes.ability.OmnimanViltrumiteRushAbility.resetAll();
			com.example.superheroes.ability.GokuKamehamehaAbility.resetAll();
			com.example.superheroes.ability.GokuSpiritBombAbility.resetAll();
			com.example.superheroes.ability.NarutoRasenganAbility.resetAll();
			com.example.superheroes.ability.NarutoOodamaRasenganAbility.resetAll();
			com.example.superheroes.ability.NarutoRasenshurikenAbility.resetAll();
			com.example.superheroes.ability.CapShieldSlamAbility.resetAll();
			com.example.superheroes.effect.MirrorDimensionController.resetAll();
			com.example.superheroes.effect.SpatialBindController.resetAll();
		});
	}

	/**
	 * Tick wiring for {@link com.example.superheroes.lifecycle.HeroTickDispatcher} — the
	 * single {@code END_SERVER_TICK} registration. Phase order is GLOBAL → LEVELS →
	 * PLAYERS → ABILITY_ACTIVE; within a phase tasks run in registration order, so this
	 * table preserves the ordering the old inline lambda had (audit debt 3).
	 */
	private static void registerTickHandlers() {
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(server -> com.example.superheroes.effect.DoomGripController.serverTick());
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.PandoraDeathController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onLevelTick((server, level) -> com.example.superheroes.horde.HordeManager.tick(level));

		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.ChargeTackleAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.ViltrumiteChargeAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.OmnimanViltrumiteRushAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.OmnimanThinkMarkAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.ironman.IronManNanoFormController.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.GokuKamehamehaAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.GokuSpiritBombAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.NarutoRasenganAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.NarutoOodamaRasenganAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.NarutoRasenshurikenAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.CapShieldSlamAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.ability.RepulsorChargeController.serverTick(p));

		com.example.superheroes.lifecycle.HeroTickDispatcher.onActiveAbilityTick(
				com.example.superheroes.ability.AbilityIds.NARUTO_SAGE_MODE,
				(server, p, data) -> com.example.superheroes.ability.NarutoSageModeAbility.serverTick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher.onActiveAbilityTick(
				com.example.superheroes.ability.AbilityIds.GOKU_SUPER_SAIYAN_AURA,
				(server, p, data) -> com.example.superheroes.ability.GokuSuperSaiyanAuraAbility.serverTick(p));

		// --- audit debt 3 remainder: controllers migrated from self-registered
		// ServerTickEvents onto HeroTickDispatcher; order mirrors init() order.
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.lifecycle.PassiveReconciler::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.ScorpionController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.SpatialBindController::tick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.MadnessAftermathController::pruneGonePlayers);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.UnibeamController::pruneGonePlayers);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.HeroLandingTracker::pruneGonePlayers);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.IronManJarvisController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.RegulusGreedController::tickFreezes);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.GreedCageController::tick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.RegulusMadnessController::tickCounters);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.InvincibleCombatController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.OmnimanMomentumController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.HeroMeleeImpactController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.physics.BallisticBodyTracker::tick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.RemDemonismController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.UraniumDefenseController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.UraniumOffhandController::pruneGonePlayers);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.FlightController::cleanup);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.KratosRageController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.DoomsdayKryptoniteController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.ReinhardTimeSlowController::tick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.ReinhardSwordDrawGateController::pruneGonePlayers);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.ReinhardSpeedJudgmentController::tick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.RaidenMusouIsshinController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.HeavensStrikeController::serverTick);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onGlobalTick(com.example.superheroes.effect.ThanosSnapWindupController::serverTick);

		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.ScorpionController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.resource.ResourceController.tick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.MadnessAftermathController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.UnibeamController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.HeroLandingTracker::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.HeroEquipmentLock::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.effect.IronManReactorTracker.tick(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.effect.IronManAutoEjectController
						.tick(p, server.getTickCount()));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RegulusGreedController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RegulusMadnessController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.SuperJumpController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.AutoSaturationController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.HomelanderRegenController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.HeroPassiveRegenController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.IronFistsController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.OmnimanMomentumController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.BattleBeastCurseController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RemDemonismController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RamCompanionController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.UraniumOffhandController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.FlightController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.SungJinwooController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.MonarchsDomainController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick((server, p, data) -> com.example.superheroes.effect.DoomsdayFootstepsController
						.tickPlayer(p));
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.GokuKiResilienceController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.NarutoWallRunController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.ThanosGauntletStateController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.ReinhardController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.ReinhardSwordDrawCeremonyController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.ReinhardSwordDrawGateController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RaidenBurstController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RaidenAuraController::tickPlayer);
		com.example.superheroes.lifecycle.HeroTickDispatcher
				.onPlayerTick(com.example.superheroes.effect.RaidenPlungingLandingController::tickPlayer);
	}
}
