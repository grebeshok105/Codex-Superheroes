package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.core.model.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registrations and senders for the core payloads (hero activation/binding, hero
 * data and resources, cooldowns, screen shake, wall-impact debris). Hero and
 * mechanic payloads register through {@code HeroModuleContext.payloads()} from
 * their modules and {@code bootstrap.SharedMechanics}; the shared receivers live
 * on mechanic classes this package may not depend on.
 */
public final class CoreNetworking {
	private CoreNetworking() {
	}

	public static void init() {
		init(PayloadRegistrar.FABRIC);
	}

	static void init(PayloadRegistrar registrar) {
		registrar.s2c(ResourceUpdateS2CPayload.TYPE, ResourceUpdateS2CPayload.STREAM_CODEC);
		registrar.s2c(HeroDataSyncS2CPayload.TYPE, HeroDataSyncS2CPayload.STREAM_CODEC);
		registrar.s2c(ScreenShakeS2CPayload.TYPE, ScreenShakeS2CPayload.STREAM_CODEC);
		registrar.s2c(AbilityCooldownS2CPayload.TYPE, AbilityCooldownS2CPayload.STREAM_CODEC);
		registrar.s2c(WallImpactDebrisS2CPayload.TYPE, WallImpactDebrisS2CPayload.STREAM_CODEC);
		registrar.s2c(BeamFxS2CPayload.TYPE, BeamFxS2CPayload.STREAM_CODEC);
	}

	public static void syncResources(ServerPlayer player, HeroData data) {
		ServerPlayNetworking.send(player, new ResourceUpdateS2CPayload(data.energy(), data.mana()));
	}

	public static void syncHeroData(ServerPlayer player, HeroData data) {
		ServerPlayNetworking.send(player, new HeroDataSyncS2CPayload(data));
	}
}
