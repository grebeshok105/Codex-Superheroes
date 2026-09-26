package com.example.superheroes.ability;

import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.lifecycle.LifecycleRegistrar;
import com.example.superheroes.lifecycle.OwnedSessionMap;
import com.example.superheroes.lifecycle.OwnedSessionMap.ClearOn;
import com.example.superheroes.transform.HeroDataStore;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;

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
	private static final OwnedSessionMap<UUID, Float> CHARGE =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));

	private RepulsorChargeController() {
	}

	public static void serverTick(ServerPlayer player) {
		boolean ironMan = IronManHero.ID.equals(HeroDataStore.get(player).heroId());
		Float stored = CHARGE.get(player.getUUID());
		float c = stored == null ? 0f : stored;
		if (ironMan && player.isShiftKeyDown()) {
			c = Math.min(1f, c + 1f / FULL_TICKS);
		} else {
			c = Math.max(0f, c - 3f / FULL_TICKS);
		}
		if (c <= 0.0001f) {
			CHARGE.remove(player.getUUID());
		} else {
			CHARGE.put(player.getUUID(), player.getUUID(), c);
		}
	}

	public static float charge(ServerPlayer player) {
		Float stored = CHARGE.get(player.getUUID());
		return stored == null ? 0f : stored;
	}

	public static void reset(ServerPlayer player) {
		CHARGE.remove(player.getUUID());
	}

}
