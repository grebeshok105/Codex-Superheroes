package io.github.grebeshok105.codex.network;

import io.github.grebeshok105.codex.core.net.CoreNetworking;
import io.github.grebeshok105.codex.effect.HeroMeleeImpactController;
import io.github.grebeshok105.codex.effect.SuperJumpController;
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

		PayloadTypeRegistry.playS2C().register(io.github.grebeshok105.codex.mechanic.flight.FlightStateS2CPayload.TYPE, io.github.grebeshok105.codex.mechanic.flight.FlightStateS2CPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SuperJumpC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SuperJumpController.activate(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(HeroMeleeChargeC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			HeroMeleeImpactController.handleChargeInput(player, payload);
		});
	}


}
