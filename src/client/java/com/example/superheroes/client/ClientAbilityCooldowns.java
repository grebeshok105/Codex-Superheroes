package com.example.superheroes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientAbilityCooldowns {
	private static final Map<ResourceLocation, Integer> DEADLINES = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, Integer> TOTALS = new ConcurrentHashMap<>();

	private ClientAbilityCooldowns() {
	}

	public static void update(ResourceLocation abilityId, int remainingTicks) {
		Minecraft mc = Minecraft.getInstance();
		int now = mc.player != null ? mc.player.tickCount : 0;
		if (remainingTicks <= 0) {
			DEADLINES.remove(abilityId);
			TOTALS.remove(abilityId);
		} else {
			DEADLINES.put(abilityId, now + remainingTicks);
			TOTALS.put(abilityId, remainingTicks);
		}
	}

	public static int remainingTicks(ResourceLocation abilityId) {
		Minecraft mc = Minecraft.getInstance();
		int now = mc.player != null ? mc.player.tickCount : 0;
		Integer deadline = DEADLINES.get(abilityId);
		if (deadline == null) return 0;
		int remaining = deadline - now;
		if (remaining <= 0) {
			DEADLINES.remove(abilityId);
			TOTALS.remove(abilityId);
			return 0;
		}
		return remaining;
	}

	public static int totalTicks(ResourceLocation abilityId) {
		Integer total = TOTALS.get(abilityId);
		return total == null ? 0 : total;
	}

	public static void clear() {
		DEADLINES.clear();
		TOTALS.clear();
	}
}
