package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.AbilityCooldownS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cooldown deadlines by level game time, stored on the player in
 * {@link ModAttachments#ABILITY_COOLDOWNS} so a hero swap or relog cannot reset them
 * (audit B5). Death resets them — the attachment is not copied to the respawned entity.
 */
public final class AbilityCooldowns {
	private AbilityCooldowns() {
	}

	public static void setCooldownTicks(ServerPlayer player, ResourceLocation abilityId, int ticks) {
		long deadline = player.level().getGameTime() + ticks;
		Map<ResourceLocation, Long> cooldowns = new HashMap<>(attached(player));
		cooldowns.put(abilityId, deadline);
		player.setAttached(ModAttachments.ABILITY_COOLDOWNS, Map.copyOf(cooldowns));
		ServerPlayNetworking.send(player, new AbilityCooldownS2CPayload(abilityId, ticks));
	}

	public static boolean isOnCooldown(ServerPlayer player, ResourceLocation abilityId) {
		Long deadline = attached(player).get(abilityId);
		return deadline != null && player.level().getGameTime() < deadline;
	}

	public static int remainingTicks(ServerPlayer player, ResourceLocation abilityId) {
		Long deadline = attached(player).get(abilityId);
		if (deadline == null) {
			return 0;
		}
		long left = deadline - player.level().getGameTime();
		return left > 0 ? (int) left : 0;
	}

	/** Drops all cooldowns for the player and tells the client each one is over. */
	public static void clearAndSync(ServerPlayer player) {
		Map<ResourceLocation, Long> cooldowns = player.getAttached(ModAttachments.ABILITY_COOLDOWNS);
		if (cooldowns == null || cooldowns.isEmpty()) {
			return;
		}
		player.setAttached(ModAttachments.ABILITY_COOLDOWNS, null);
		for (ResourceLocation abilityId : cooldowns.keySet()) {
			ServerPlayNetworking.send(player, new AbilityCooldownS2CPayload(abilityId, 0));
		}
	}

	/** Join hook — resends every still-running cooldown so the client HUD matches the server. */
	public static void syncAll(ServerPlayer player) {
		for (Map.Entry<ResourceLocation, Long> entry : attached(player).entrySet()) {
			long left = entry.getValue() - player.level().getGameTime();
			if (left > 0) {
				ServerPlayNetworking.send(player, new AbilityCooldownS2CPayload(entry.getKey(), (int) left));
			}
		}
	}

	private static Map<ResourceLocation, Long> attached(ServerPlayer player) {
		Map<ResourceLocation, Long> cooldowns = player.getAttached(ModAttachments.ABILITY_COOLDOWNS);
		return cooldowns == null ? Map.of() : cooldowns;
	}
}
