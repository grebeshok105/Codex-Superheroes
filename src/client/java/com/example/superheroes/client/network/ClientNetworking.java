package com.example.superheroes.client.network;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientFlightState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.fx.ScreenShakeManager;
import com.example.superheroes.client.fx.WallImpactDebrisManager;
import com.example.superheroes.flight.FlightAbilityState;
import com.example.superheroes.network.HeroDataSyncS2CPayload;
import com.example.superheroes.network.FlightStateS2CPayload;
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

		ClientPlayNetworking.registerGlobalReceiver(ScreenShakeS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ScreenShakeManager.shake(payload.intensity(), payload.durationTicks())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.WallImpactDebrisS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> WallImpactDebrisManager.spawn(
						context.client().level, payload.position(), payload.direction(),
						payload.intensity(), payload.blockStateIds())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.AbilityCooldownS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientAbilityCooldowns.update(payload.abilityId(), payload.remainingTicks())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.SuitVariantS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientSuitVariantState.update(payload.playerId(), payload.variant())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.HordeDebugS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.hud.HordeDebugOverlay.update(payload.text())));

		ClientPlayNetworking.registerGlobalReceiver(com.example.superheroes.network.AdminBuildS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					com.example.superheroes.item.AdminBuildVisibility.setClientVisible(payload.enabled());
					superheroes$rebuildSuperheroesTab(context.client());
				}));
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
