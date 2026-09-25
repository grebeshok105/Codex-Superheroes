package com.example.superheroes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Таймеры кулдаунов способностей на клиенте — сроки в тиках игрового времени
 * уровня ({@code ClientLevel#getGameTime()}), а не {@code LocalPlayer#tickCount}:
 * тот обнуляется при респавне, из-за чего HUD рисовал кулдауны в часы (аудит B15).
 * Формат пакета не менялся: сервер по-прежнему шлёт «сколько тиков осталось».
 */
public final class ClientAbilityCooldowns {
	private static final Map<ResourceLocation, Long> DEADLINES = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, Integer> TOTALS = new ConcurrentHashMap<>();

	private static LongSupplier clock = ClientAbilityCooldowns::levelGameTime;

	static {
		ClientSessionState.register(ClientAbilityCooldowns::clear);
	}

	private ClientAbilityCooldowns() {
	}

	public static void update(ResourceLocation abilityId, int remainingTicks) {
		if (remainingTicks <= 0) {
			DEADLINES.remove(abilityId);
			TOTALS.remove(abilityId);
		} else {
			DEADLINES.put(abilityId, clock.getAsLong() + remainingTicks);
			TOTALS.put(abilityId, remainingTicks);
		}
	}

	public static int remainingTicks(ResourceLocation abilityId) {
		Long deadline = DEADLINES.get(abilityId);
		if (deadline == null) return 0;
		long remaining = deadline - clock.getAsLong();
		if (remaining <= 0) {
			DEADLINES.remove(abilityId);
			TOTALS.remove(abilityId);
			return 0;
		}
		return (int) Math.min(Integer.MAX_VALUE, remaining);
	}

	public static int totalTicks(ResourceLocation abilityId) {
		Integer total = TOTALS.get(abilityId);
		return total == null ? 0 : total;
	}

	public static void clear() {
		DEADLINES.clear();
		TOTALS.clear();
	}

	private static long levelGameTime() {
		Minecraft mc = Minecraft.getInstance();
		return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
	}

	static void setClockForTesting(LongSupplier testClock) {
		clock = testClock;
	}

	static void clearClockForTesting() {
		clock = ClientAbilityCooldowns::levelGameTime;
	}
}
