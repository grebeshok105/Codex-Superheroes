package io.github.grebeshok105.codex.content.admin;

import io.github.grebeshok105.codex.core.module.ContentModuleContext;
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

	public static void register(ContentModuleContext ctx) {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				send(handler.getPlayer()));
	}

	public static void send(ServerPlayer player) {
		boolean enabled = player.getAttachedOrCreate(AdminAttachments.ADMIN_BUILD);
		ServerPlayNetworking.send(player, new AdminBuildS2CPayload(enabled));
	}
}
