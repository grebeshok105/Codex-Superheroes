package com.example.superheroes;

import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.command.SuperheroesCommands;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.item.ModItemGroups;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.resource.ResourceController;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroTransformService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperheroesMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(ModId.MOD_ID);

	@Override
	public void onInitialize() {
		ModAttachments.init();
		ModEffects.init();
		Heroes.init();
		AbilityRegistry.init();
		com.example.superheroes.entity.ModEntities.init();
		ModItems.init();
		ModItemGroups.init();
		ModParticles.init();
		ModSounds.init();
		ModNetworking.init();
		ResourceController.init();
		com.example.superheroes.effect.MadnessFlightController.init();
		com.example.superheroes.effect.MadnessAftermathController.init();
		com.example.superheroes.effect.UnibeamController.init();
		com.example.superheroes.effect.HeroLandingTracker.init();
		com.example.superheroes.effect.HeroEquipmentLock.init();
		com.example.superheroes.effect.IronManReactorTracker.init();
		com.example.superheroes.effect.IronManAutoEjectController.init();
		com.example.superheroes.effect.RegulusTotemController.init();
		com.example.superheroes.effect.RegulusGreedController.init();
		com.example.superheroes.effect.GreedCageController.init();
		com.example.superheroes.effect.RegulusMadnessController.init();
		com.example.superheroes.effect.SuperJumpController.init();
		com.example.superheroes.effect.AutoSaturationController.init();
		com.example.superheroes.effect.HomelanderRegenController.init();
		com.example.superheroes.effect.HeroPassiveRegenController.init();
		com.example.superheroes.effect.IronFistsController.init();
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
		com.example.superheroes.effect.KratosRageController.init();
		com.example.superheroes.effect.KratosHandStrikeFxController.init();
		com.example.superheroes.effect.DoomsdayKryptoniteController.init();
		com.example.superheroes.effect.ThanosStoneRewardController.init();
		com.example.superheroes.effect.ReinhardTimeSlowController.init();
		com.example.superheroes.effect.ReinhardController.init();
		com.example.superheroes.effect.ReinhardSwordDrawCeremonyController.init();
		com.example.superheroes.effect.ReinhardSwordDrawGateController.init();
		com.example.superheroes.effect.ReinhardSwordDeathMarkController.init();
		com.example.superheroes.effect.RaidenBurstController.init();
		com.example.superheroes.effect.RaidenAuraController.init();
		com.example.superheroes.effect.RaidenPlungingLandingController.init();
		com.example.superheroes.effect.HeavensStrikeController.init();
		com.example.superheroes.effect.ThanosSnapWindupController.init();
		SuperheroesCommands.init();

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			com.example.superheroes.effect.DoomGripController.serverTick();
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				com.example.superheroes.ability.ChargeTackleAbility.serverTick(p);
				com.example.superheroes.ability.GokuKamehamehaAbility.serverTick(p);
				com.example.superheroes.ability.GokuSpiritBombAbility.serverTick(p);
				com.example.superheroes.ability.NarutoRasenganAbility.serverTick(p);
				com.example.superheroes.ability.NarutoOodamaRasenganAbility.serverTick(p);
				com.example.superheroes.ability.NarutoRasenshurikenAbility.serverTick(p);
				com.example.superheroes.ability.CapShieldSlamAbility.serverTick(p);
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

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			HeroTransformService.onPlayerJoin(handler.getPlayer());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			HeroTransformService.onPlayerDisconnect(handler.getPlayer());
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

		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(
				(oldPlayer, newPlayer, alive) -> HeroTransformService.onPlayerRespawn(newPlayer));

		EntityTrackingEvents.START_TRACKING.register((tracked, observer) -> {
			if (tracked instanceof ServerPlayer trackedPlayer) {
				ModNetworking.sendRemoteHeroSkinTo(observer, trackedPlayer);
			}
		});

		LOGGER.info("Superheroes mod initialized");
	}
}
