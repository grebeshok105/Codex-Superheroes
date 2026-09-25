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
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
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
		com.example.superheroes.effect.ScorpionController.init();
		com.example.superheroes.effect.MirrorDimensionController.init();
		com.example.superheroes.effect.SpatialBindController.init();
		ModNetworking.init();
		HeroDataStore.init();
		ResourceController.init();
		com.example.superheroes.effect.MadnessFlightController.init();
		com.example.superheroes.effect.MadnessAftermathController.init();
		com.example.superheroes.effect.UnibeamController.init();
		com.example.superheroes.effect.HeroLandingTracker.init();
		com.example.superheroes.effect.HeroEquipmentLock.init();
		com.example.superheroes.effect.IronManReactorTracker.init();
		com.example.superheroes.effect.IronManAutoEjectController.init();
		com.example.superheroes.effect.IronManJarvisController.init();
		com.example.superheroes.ability.ironman.IronManNanoFormController.init();
		com.example.superheroes.effect.RegulusTotemController.init();
		com.example.superheroes.effect.RegulusGreedController.init();
		com.example.superheroes.effect.GreedCageController.init();
		com.example.superheroes.effect.RegulusMadnessController.init();
		com.example.superheroes.effect.SuperJumpController.init();
		com.example.superheroes.effect.AutoSaturationController.init();
		com.example.superheroes.effect.HomelanderRegenController.init();
		com.example.superheroes.effect.HeroPassiveRegenController.init();
		com.example.superheroes.effect.IronFistsController.init();
		com.example.superheroes.effect.InvincibleCombatController.init();
		com.example.superheroes.effect.OmnimanMomentumController.init();
		com.example.superheroes.effect.HeroMeleeImpactController.init();
		com.example.superheroes.physics.BallisticBodyTracker.init();
		com.example.superheroes.effect.BattleBeastCurseController.init();
		com.example.superheroes.effect.RemDemonismController.init();
		com.example.superheroes.effect.RamCompanionController.init();
		com.example.superheroes.effect.UraniumDefenseController.init();
		com.example.superheroes.effect.UraniumOffhandController.init();
		com.example.superheroes.effect.FlightController.init();
		com.example.superheroes.effect.SungJinwooController.init();
		com.example.superheroes.effect.MonarchsDomainController.init();
		com.example.superheroes.effect.DoomsdayAdaptationController.init();
		com.example.superheroes.effect.DoomsdayFootstepsController.init();
		com.example.superheroes.effect.DoomsdayTierController.init();
		com.example.superheroes.effect.GokuKiStackController.init();
		com.example.superheroes.effect.GokuKiResilienceController.init();
		com.example.superheroes.effect.NarutoWallRunController.init();
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
		com.example.superheroes.effect.ReinhardSwordDrawCeremonyController.init();
		com.example.superheroes.effect.ReinhardSwordDrawGateController.init();
		com.example.superheroes.effect.ReinhardSwordDeathMarkController.init();
		com.example.superheroes.effect.ReinhardSpeedJudgmentController.init();
		com.example.superheroes.effect.RaidenBurstController.init();
		com.example.superheroes.effect.RaidenAuraController.init();
		com.example.superheroes.effect.RaidenPlungingLandingController.init();
		com.example.superheroes.effect.RaidenMusouIsshinController.init();
		com.example.superheroes.effect.HeavensStrikeController.init();
		com.example.superheroes.effect.ThanosSnapWindupController.init();
		SuperheroesCommands.init();

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			com.example.superheroes.effect.DoomGripController.serverTick();
			com.example.superheroes.effect.PandoraDeathController.serverTick(server);
			for (net.minecraft.server.level.ServerLevel sl : server.getAllLevels()) {
				com.example.superheroes.horde.HordeManager.tick(sl);
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				// Dead players stay in the player list until respawn — ability ticks must not
				// run on a corpse (audit B17).
				if (p.isDeadOrDying()) {
					continue;
				}
				com.example.superheroes.ability.ChargeTackleAbility.serverTick(p);
				com.example.superheroes.ability.ViltrumiteChargeAbility.serverTick(p);
				com.example.superheroes.ability.MeteorSlamAbility.serverTick(p);
				com.example.superheroes.ability.OmnimanViltrumiteRushAbility.serverTick(p);
				com.example.superheroes.ability.OmnimanThinkMarkAbility.serverTick(p);
				com.example.superheroes.ability.ironman.IronManNanoFormController.serverTick(p);
				com.example.superheroes.ability.GokuKamehamehaAbility.serverTick(p);
				com.example.superheroes.ability.GokuSpiritBombAbility.serverTick(p);
				com.example.superheroes.ability.NarutoRasenganAbility.serverTick(p);
				com.example.superheroes.ability.NarutoOodamaRasenganAbility.serverTick(p);
				com.example.superheroes.ability.NarutoRasenshurikenAbility.serverTick(p);
				com.example.superheroes.ability.CapShieldSlamAbility.serverTick(p);
				com.example.superheroes.ability.RepulsorChargeController.serverTick(p);
				com.example.superheroes.transform.HeroData data = p
						.getAttachedOrCreate(com.example.superheroes.attachment.ModAttachments.HERO_DATA);
				if (data.isActive(com.example.superheroes.ability.AbilityIds.NARUTO_SAGE_MODE)) {
					com.example.superheroes.ability.NarutoSageModeAbility.serverTick(p);
				}
				if (data.isActive(com.example.superheroes.ability.AbilityIds.GOKU_SUPER_SAIYAN_AURA)) {
					com.example.superheroes.ability.GokuSuperSaiyanAuraAbility.serverTick(p);
				}
			}
		});

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

		EntityTrackingEvents.START_TRACKING.register((tracked, observer) -> {
			if (tracked instanceof ServerPlayer trackedPlayer) {
				ModNetworking.sendRemoteHeroSkinTo(observer, trackedPlayer);
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
		PlayerLifecycle.onLeave(com.example.superheroes.ability.MeteorSlamAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.OmnimanViltrumiteRushAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.GokuKamehamehaAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.GokuSpiritBombAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.NarutoRasenganAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.NarutoOodamaRasenganAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.NarutoRasenshurikenAbility::clear);
		PlayerLifecycle.onLeave(com.example.superheroes.ability.CapShieldSlamAbility::clear);

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
		PlayerLifecycle.onDeath(com.example.superheroes.ability.MeteorSlamAbility::clear);
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
			com.example.superheroes.ability.MeteorSlamAbility.resetAll();
			com.example.superheroes.ability.OmnimanViltrumiteRushAbility.resetAll();
			com.example.superheroes.ability.GokuKamehamehaAbility.resetAll();
			com.example.superheroes.ability.GokuSpiritBombAbility.resetAll();
			com.example.superheroes.ability.NarutoRasenganAbility.resetAll();
			com.example.superheroes.ability.NarutoOodamaRasenganAbility.resetAll();
			com.example.superheroes.ability.NarutoRasenshurikenAbility.resetAll();
			com.example.superheroes.ability.CapShieldSlamAbility.resetAll();
		});
	}
}
