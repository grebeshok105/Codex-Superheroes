package io.github.grebeshok105.codex.hero.ironman.runtime;

import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.ironman.net.SuitVariantS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Синхронизация варианта костюма Железного Человека со всеми клиентами:
 * при входе игрока — рассылка его костюма всем и всех костюмов ему,
 * при смене костюма — broadcast через {@link #broadcast(ServerPlayer)}.
 */
public final class IronManSuitSyncController {
	private IronManSuitSyncController() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.lifecycle().onJoin(joining -> {
			for (ServerPlayer other : joining.server.getPlayerList().getPlayers()) {
				int variant = other.getAttachedOrCreate(IronManSuitVariant.ATTACHMENT);
				if (variant != 0) {
					ServerPlayNetworking.send(joining, new SuitVariantS2CPayload(other.getUUID(), variant));
				}
			}
			broadcast(joining);
		});
	}

	public static void broadcast(ServerPlayer player) {
		int variant = player.getAttachedOrCreate(IronManSuitVariant.ATTACHMENT);
		SuitVariantS2CPayload payload = new SuitVariantS2CPayload(player.getUUID(), variant);
		for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}
}
