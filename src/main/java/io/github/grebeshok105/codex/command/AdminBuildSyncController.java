package io.github.grebeshok105.codex.command;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.network.AdminBuildS2CPayload;
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

	public static void register(HeroModuleContext ctx) {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				send(handler.getPlayer()));
	}

	public static void send(ServerPlayer player) {
		boolean enabled = player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD);
		ServerPlayNetworking.send(player, new AdminBuildS2CPayload(enabled));
	}
}
