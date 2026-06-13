package com.example.superheroes.client.network;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientFlightState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientMadnessState;
import com.example.superheroes.client.ClientReactorState;
import com.example.superheroes.client.ClientRemDemonismState;
import com.example.superheroes.client.RemoteHeroSkins;
import com.example.superheroes.client.fx.ClientScorpionFx;
import com.example.superheroes.client.fx.ScreenShakeManager;
import com.example.superheroes.client.fx.WallImpactDebrisManager;
import com.example.superheroes.client.hud.BloodRainHud;
import com.example.superheroes.client.render.CosmicBeamRenderer;
import com.example.superheroes.client.render.LaserBeamRenderer;
import com.example.superheroes.client.render.RepulsorBeamRenderer;
import com.example.superheroes.flight.FlightAbilityState;
import com.example.superheroes.network.HeroDataSyncS2CPayload;
import com.example.superheroes.network.FlightStateS2CPayload;
import com.example.superheroes.network.LaserFiredS2CPayload;
import com.example.superheroes.network.ThanosCosmicBeamS2CPayload;
import com.example.superheroes.network.MadnessSyncS2CPayload;
import com.example.superheroes.network.MadnessVisualS2CPayload;
import com.example.superheroes.network.ReactorStateS2CPayload;
import com.example.superheroes.network.RepulsorBlastS2CPayload;
import com.example.superheroes.network.RemoteHeroSkinS2CPayload;
import com.example.superheroes.network.ResourceUpdateS2CPayload;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientNetworking {
	private ClientNetworking() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(HeroDataSyncS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					HeroData data = payload.data();
					if (!data.hasHero() || !data.heroId().equals(ClientHeroState.heroId())) {
						ClientAbilityCooldowns.clear();
					}
					ClientHeroState.update(data);
					LocalPlayer self = Minecraft.getInstance().player;
					if (self != null) {
						HeroData previous = self.getAttachedOrCreate(ModAttachments.HERO_DATA);
						self.setAttached(ModAttachments.HERO_DATA, data);
						if (previous.hasHero() != data.hasHero()
								|| (data.hasHero() && !data.heroId().equals(previous.heroId()))) {
							self.refreshDimensions();
						}
						boolean wasFlight = FlightAbilityState.isActive(previous);
						boolean isFlight = FlightAbilityState.isActive(data);
						if (!wasFlight && isFlight && !self.isFallFlying()) {
							self.startFallFlying();
						}
						if (!isFlight) {
							ClientFlightState.clear(self.getId());
						}
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(ResourceUpdateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientHeroState.updateResources(payload.energy(), payload.mana())));

		ClientPlayNetworking.registerGlobalReceiver(FlightStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientFlightState.update(
						payload.entityId(),
						payload.active(),
						com.example.superheroes.flight.FlightMode.byOrdinal(payload.mode()),
						com.example.superheroes.flight.FlightPhase.byOrdinal(payload.phase()),
						payload.horizontalSpeed())));

		ClientPlayNetworking.registerGlobalReceiver(LaserFiredS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> LaserBeamRenderer.add(payload.start(), payload.end())));

		ClientPlayNetworking.registerGlobalReceiver(RepulsorBlastS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> RepulsorBeamRenderer.add(payload.start(), payload.end())));

		ClientPlayNetworking.registerGlobalReceiver(ThanosCosmicBeamS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> CosmicBeamRenderer.add(payload.start(), payload.end())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ScorpionFxS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientScorpionFx.play(payload)));
		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.MirrorDimensionS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					switch (payload.action()) {
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_ON ->
								com.example.superheroes.client.ClientMirrorDimensionState.activate(payload.mode(), payload.scale());
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_OFF ->
								com.example.superheroes.client.ClientMirrorDimensionState.deactivate(true);
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_KEEPALIVE ->
								com.example.superheroes.client.ClientMirrorDimensionState.keepalive();
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_SWITCH ->
								com.example.superheroes.client.ClientMirrorDimensionState.switchMode(payload.mode(), payload.scale());
						default -> {
						}
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(ScreenShakeS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ScreenShakeManager.shake(payload.intensity(), payload.durationTicks())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.WallImpactDebrisS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> WallImpactDebrisManager.spawn(
						context.client().level, payload.position(), payload.direction(),
						payload.intensity(), payload.blockStateIds())));

		ClientPlayNetworking.registerGlobalReceiver(RemoteHeroSkinS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> RemoteHeroSkins.put(payload.playerId(), payload.heroId().orElse(null))));

		ClientPlayNetworking.registerGlobalReceiver(ReactorStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientReactorState.update(payload.active(), payload.progressTicks(), payload.totalTicks(), payload.hasStock())));

		ClientPlayNetworking.registerGlobalReceiver(MadnessSyncS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientMadnessState.update(
						payload.madness(), payload.bonusLifeAvailable(),
						payload.readingUntilMs(), payload.manaLockUntilMs())));

		ClientPlayNetworking.registerGlobalReceiver(MadnessVisualS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (payload.event() == MadnessVisualS2CPayload.EVENT_ENTER) {
						BloodRainHud.trigger();
					} else if (payload.event() == MadnessVisualS2CPayload.EVENT_EXIT) {
						BloodRainHud.clear();
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.UraniumPressureS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientUraniumPressureState.update(payload.pressuredHomelanders())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.UraniumThreatS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientUraniumThreatState.update(payload.self(), payload.sourceCount())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.SungShadowArmyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientShadowArmyState.update(
						payload.playerId(), payload.hasShadows(), payload.count(), payload.phase2())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.DoomsdayProgressS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientDoomsdayState.update(payload.tier(), payload.adaptations())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.AbilityCooldownS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientAbilityCooldowns.update(payload.abilityId(), payload.remainingTicks())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.JarvisDetectionS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.hud.JarvisDetectionHud.onDetection(
						payload.playerName(), payload.heroId(), payload.distance(),
						payload.threatClass(), payload.jarvisQuote())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ThanosStonesS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientThanosState.update(payload.playerId(), payload.bitmask())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.SuitVariantS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientSuitVariantState.update(payload.playerId(), payload.variant())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.NanoFormS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientNanoFormState.update(payload.playerId(), payload.form())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ThinkMarkS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientThinkMarkState.update(payload.playerId(), payload.active())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.HordeDebugS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.hud.HordeDebugOverlay.update(payload.text())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.AdminBuildS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					com.example.superheroes.item.AdminBuildVisibility.setClientVisible(payload.enabled());
					superheroes$rebuildSuperheroesTab(context.client());
				}));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.KratosRageS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientKratosRageState.update(payload.rage(), payload.active())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.RemDemonismS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientRemDemonismState.update(
						payload.playerId(), payload.charge(), payload.active(), payload.permanent())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ReinhardWishOptionsS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					Minecraft mc = Minecraft.getInstance();
					mc.setScreen(com.example.superheroes.client.screen.ReinhardWishScreen.of(
							payload.damageTypeIds(), payload.adaptedDamageTypeIds(),
							payload.wishesUsed(), payload.wishesMax()));
				}));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ReinhardCeremonyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardCeremonyState.update(
						payload.active(), payload.progress())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ReinhardSwordGateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardSwordGateState.update(
						payload.ready(), payload.progress())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ReinhardSwordKillS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardSwordKillState.update(payload.active())));
		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ReinhardDarknessS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardDarknessState.activate(payload.durationTicks())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.ReinhardTimeSlowS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardTimeSlowState.update(payload.active())));
	}

	/**
	 * Пересобирает содержимое креатив-вкладки Superheroes после смены
	 * состояния админ-билда (vanilla кэширует вкладки и сам не обновит).
	 */
	private static void superheroes$rebuildSuperheroesTab(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		var parameters = new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
				mc.player.connection.enabledFeatures(),
				mc.player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get(),
				mc.level.registryAccess());
		com.example.superheroes.item.ModItemGroups.SUPERHEROES_TAB.buildContents(parameters);
	}
}
