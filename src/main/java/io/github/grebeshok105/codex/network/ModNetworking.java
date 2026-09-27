package io.github.grebeshok105.codex.network;

import io.github.grebeshok105.codex.core.net.CoreNetworking;
import io.github.grebeshok105.codex.core.net.FxBroadcast;
import io.github.grebeshok105.codex.effect.HeroMeleeImpactController;
import io.github.grebeshok105.codex.effect.SuperJumpController;
import io.github.grebeshok105.codex.hero.ironman.net.JarvisDetectionS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.NanoFormS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.ReactorStateS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.SuitVariantS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		CoreNetworking.init();

		PayloadTypeRegistry.playC2S().register(SuperJumpC2SPayload.TYPE, SuperJumpC2SPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(HeroMeleeChargeC2SPayload.TYPE, HeroMeleeChargeC2SPayload.STREAM_CODEC);

		PayloadTypeRegistry.playS2C().register(FlightStateS2CPayload.TYPE, FlightStateS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReactorStateS2CPayload.TYPE, ReactorStateS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(UraniumPressureS2CPayload.TYPE, UraniumPressureS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(UraniumThreatS2CPayload.TYPE, UraniumThreatS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(JarvisDetectionS2CPayload.TYPE, JarvisDetectionS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SuitVariantS2CPayload.TYPE, SuitVariantS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(NanoFormS2CPayload.TYPE, NanoFormS2CPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SuperJumpC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SuperJumpController.activate(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(HeroMeleeChargeC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			HeroMeleeImpactController.handleChargeInput(player, payload);
		});
	}

	public static void syncFlightState(ServerPlayer player, io.github.grebeshok105.codex.flight.FlightMode mode,
			io.github.grebeshok105.codex.flight.FlightPhase phase, float horizontalSpeed, boolean active) {
		FlightStateS2CPayload payload = new FlightStateS2CPayload(
				player.getId(), active, mode.ordinal(), phase.ordinal(), horizontalSpeed);
		FxBroadcast.trackingAndSelf(player, payload);
	}

}
