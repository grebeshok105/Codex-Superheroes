package io.github.grebeshok105.codex.client.core.net;

import io.github.grebeshok105.codex.client.ClientAbilityCooldowns;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.core.render.BeamRenderer;
import io.github.grebeshok105.codex.client.core.vfx.VfxRuntime;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.fx.ScreenShakeManager;
import io.github.grebeshok105.codex.client.fx.WallImpactDebrisManager;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.net.AbilityCooldownS2CPayload;
import io.github.grebeshok105.codex.core.net.BeamFxS2CPayload;
import io.github.grebeshok105.codex.core.net.HeroDataSyncS2CPayload;
import io.github.grebeshok105.codex.core.net.ResourceUpdateS2CPayload;
import io.github.grebeshok105.codex.core.net.ScreenShakeS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxEventS2CPayload;
import io.github.grebeshok105.codex.core.net.WallImpactDebrisS2CPayload;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.mechanic.flight.FlightAbilityState;
import io.github.grebeshok105.codex.mechanic.flight.FlightStateS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Receivers for the core S2C payloads — the client mirror of {@code core/net/CoreNetworking}.
 * Hero payloads register through {@code HeroClientContext.receive} in their client modules.
 */
public final class CoreClientReceivers {
	private CoreClientReceivers() {
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
						io.github.grebeshok105.codex.mechanic.flight.FlightMode.byOrdinal(payload.mode()),
						io.github.grebeshok105.codex.mechanic.flight.FlightPhase.byOrdinal(payload.phase()),
						payload.horizontalSpeed())));

		ClientPlayNetworking.registerGlobalReceiver(ScreenShakeS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ScreenShakeManager.shake(payload.intensity(), payload.durationTicks())));

		ClientPlayNetworking.registerGlobalReceiver(WallImpactDebrisS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> WallImpactDebrisManager.spawn(
						context.client().level, payload.position(), payload.direction(),
						payload.intensity(), payload.blockStateIds())));

		ClientPlayNetworking.registerGlobalReceiver(AbilityCooldownS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientAbilityCooldowns.update(payload.abilityId(), payload.remainingTicks())));

		ClientPlayNetworking.registerGlobalReceiver(BeamFxS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> BeamRenderer.add(
						payload.style(), payload.start(), payload.end())));

		ClientPlayNetworking.registerGlobalReceiver(VfxEventS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (context.client().level == null) {
						return;
					}
					Entity source = payload.sourceEntityId() == VfxEventS2CPayload.NO_SOURCE
							? null
							: context.client().level.getEntity(payload.sourceEntityId());
					VfxRuntime.spawn(new VfxSpawn(payload.effect(), source, payload.sourceEntityId(),
							payload.origin(), payload.target(), payload.scale(), payload.seed(),
							VfxParamsLoader.get(payload.effect())));
				}));

		ClientPlayNetworking.registerGlobalReceiver(VfxChannelS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (context.client().level == null) {
						return;
					}
					VfxRuntime.channel(payload.entityId(), payload.channel(), payload.state(), payload.target());
				}));
	}
}
