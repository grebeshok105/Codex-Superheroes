package io.github.grebeshok105.codex.client.network;

import io.github.grebeshok105.codex.client.ClientAbilityCooldowns;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.fx.ScreenShakeManager;
import io.github.grebeshok105.codex.client.fx.WallImpactDebrisManager;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.net.AbilityCooldownS2CPayload;
import io.github.grebeshok105.codex.core.net.WallImpactDebrisS2CPayload;
import io.github.grebeshok105.codex.flight.FlightAbilityState;
import io.github.grebeshok105.codex.core.net.HeroDataSyncS2CPayload;
import io.github.grebeshok105.codex.network.FlightStateS2CPayload;
import io.github.grebeshok105.codex.core.net.ResourceUpdateS2CPayload;
import io.github.grebeshok105.codex.core.net.ScreenShakeS2CPayload;
import io.github.grebeshok105.codex.core.transform.HeroData;
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
						HeroData previous = self.getAttachedOrCreate(CoreAttachments.HERO_DATA);
						self.setAttached(CoreAttachments.HERO_DATA, data);
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
						io.github.grebeshok105.codex.flight.FlightMode.byOrdinal(payload.mode()),
						io.github.grebeshok105.codex.flight.FlightPhase.byOrdinal(payload.phase()),
						payload.horizontalSpeed())));

		ClientPlayNetworking.registerGlobalReceiver(ScreenShakeS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ScreenShakeManager.shake(payload.intensity(), payload.durationTicks())));

		ClientPlayNetworking.registerGlobalReceiver(io.github.grebeshok105.codex.core.net.WallImpactDebrisS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> WallImpactDebrisManager.spawn(
						context.client().level, payload.position(), payload.direction(),
						payload.intensity(), payload.blockStateIds())));

		ClientPlayNetworking.registerGlobalReceiver(io.github.grebeshok105.codex.core.net.AbilityCooldownS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientAbilityCooldowns.update(payload.abilityId(), payload.remainingTicks())));

		ClientPlayNetworking.registerGlobalReceiver(io.github.grebeshok105.codex.network.SuitVariantS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientSuitVariantState.update(payload.playerId(), payload.variant())));

		ClientPlayNetworking.registerGlobalReceiver(io.github.grebeshok105.codex.network.HordeDebugS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.hud.HordeDebugOverlay.update(payload.text())));

		ClientPlayNetworking.registerGlobalReceiver(io.github.grebeshok105.codex.network.AdminBuildS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					io.github.grebeshok105.codex.item.AdminBuildVisibility.setClientVisible(payload.enabled());
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
		io.github.grebeshok105.codex.item.ModItemGroups.SUPERHEROES_TAB.buildContents(parameters);
	}
}
