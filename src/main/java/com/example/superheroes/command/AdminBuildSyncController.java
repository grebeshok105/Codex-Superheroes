package com.example.superheroes.command;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.AdminBuildS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Синхронизирует состояние админ-билда с клиентом игрока — при входе
 * и при каждом переключении {@code /superheroes admin}.
 */
public final class AdminBuildSyncController {
	private AdminBuildSyncController() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				send(handler.getPlayer()));
	}

	public static void send(ServerPlayer player) {
		boolean enabled = player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD);
		ServerPlayNetworking.send(player, new AdminBuildS2CPayload(enabled));
	}
}
