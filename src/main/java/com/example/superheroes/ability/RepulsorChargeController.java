package com.example.superheroes.ability;

import com.example.superheroes.api.HeroApi;
import com.example.superheroes.hero.IronManHero;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверный счётчик динамического заряда репульсоров Железного Человека.
 * Пока игрок-Железный-Человек сидит в присяде — заряд растёт 0→1 за
 * {@link #FULL_TICKS} тиков; иначе быстро стекает. {@link RepulsorAbility}
 * читает {@link #charge(ServerPlayer)} в момент выстрела и масштабирует урон,
 * радиус ударной волны, число лучей и отброс. Логика зеркалит клиентский
 * {@code ClientRepulsorChargeState}, поэтому HUD-гейдж совпадает с реальной
 * силой выстрела без отдельного пакета.
 */
public final class RepulsorChargeController {
	public static final int FULL_TICKS = 30;
	private static final Map<UUID, Float> CHARGE = new ConcurrentHashMap<>();

	private RepulsorChargeController() {
	}

	public static void serverTick(ServerPlayer player) {
		boolean ironMan = IronManHero.ID.equals(HeroApi.getCurrentHeroId(player).orElse(null));
		float c = CHARGE.getOrDefault(player.getUUID(), 0f);
		if (ironMan && player.isShiftKeyDown()) {
			c = Math.min(1f, c + 1f / FULL_TICKS);
		} else {
			c = Math.max(0f, c - 3f / FULL_TICKS);
		}
		if (c <= 0.0001f) {
			CHARGE.remove(player.getUUID());
		} else {
			CHARGE.put(player.getUUID(), c);
		}
	}

	public static float charge(ServerPlayer player) {
		return CHARGE.getOrDefault(player.getUUID(), 0f);
	}

	public static void reset(ServerPlayer player) {
		CHARGE.remove(player.getUUID());
	}
}
