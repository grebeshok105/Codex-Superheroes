package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code PUBLIC_HERO} arrives via attachment sync with no client-side change callback, and a
 * remote player's {@code dimensions} only recalculates on a pose change. Watches the synced hero
 * id of every tracked player and calls {@code refreshDimensions()} the moment it changes, so a
 * Battle Beast hitbox (or its removal) takes effect without waiting for a crouch (audit B14).
 */
public final class ClientHeroDimsWatcher {
	private static final Map<UUID, ResourceLocation> LAST_SEEN = new HashMap<>();

	private ClientHeroDimsWatcher() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(ClientHeroDimsWatcher::tick);
	}

	private static void tick(Minecraft client) {
		if (client.level == null) {
			LAST_SEEN.clear();
			return;
		}
		for (AbstractClientPlayer player : client.level.players()) {
			ResourceLocation heroId = player.getAttached(CoreAttachments.PUBLIC_HERO);
			ResourceLocation previous = LAST_SEEN.put(player.getUUID(), heroId);
			if (!Objects.equals(previous, heroId)) {
				player.refreshDimensions();
			}
		}
	}
}
