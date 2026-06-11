package com.example.superheroes.ability.ironman;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.SuitVariantS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer joining = handler.getPlayer();
			for (ServerPlayer other : server.getPlayerList().getPlayers()) {
				int variant = other.getAttachedOrCreate(ModAttachments.SUIT_VARIANT);
				if (variant != 0) {
					ServerPlayNetworking.send(joining, new SuitVariantS2CPayload(other.getUUID(), variant));
				}
			}
			broadcast(joining);
		});
	}

	public static void broadcast(ServerPlayer player) {
		int variant = player.getAttachedOrCreate(ModAttachments.SUIT_VARIANT);
		SuitVariantS2CPayload payload = new SuitVariantS2CPayload(player.getUUID(), variant);
		for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}
}
