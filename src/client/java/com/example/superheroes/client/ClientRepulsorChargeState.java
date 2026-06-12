package com.example.superheroes.client;

/**
 * Клиентское «состояние заряда репульсоров» Железного Человека для HUD.
 * Заряд РЕАЛЬНО динамический: пока игрок в присяде (Shift) и он Железный
 * Человек — заряд плавно растёт 0→1 (за {@link #FULL_TICKS} тиков), иначе
 * быстро стекает к 0. При выстреле репульсора заряд сбрасывается и
 * запускается короткая «вспышка разряда» для подсветки прицела.
 *
 * <p>Серверная сторона ({@code RepulsorChargeController}) считает заряд по той
 * же логике (длительность присяда), поэтому индикатор на HUD соответствует
 * реальной силе заряженного выстрела без отдельного пакета.
 */
public final class ClientRepulsorChargeState {
	private static final int FULL_TICKS = 30; // ~1.5s до полного заряда
	private static final long FLASH_MS = 700L;

	private static volatile float charge = 0f;
	private static volatile long lastFireMs = 0L;

	private ClientRepulsorChargeState() {
	}

	/** Тик клиента: наращиваем/стекаем заряд по состоянию присяда. */
	public static void clientTick(boolean isIronMan, boolean sneaking) {
		if (isIronMan && sneaking) {
			charge = Math.min(1f, charge + 1f / FULL_TICKS);
		} else {
			charge = Math.max(0f, charge - 3f / FULL_TICKS);
		}
	}

	/** Вызывается при выстреле репульсора (из рендера трассера). */
	public static void flash() {
		lastFireMs = System.currentTimeMillis();
		charge = 0f;
	}

	/** Текущий накапливаемый заряд 0..1 (для гейджа). */
	public static float charge() {
		return charge;
	}

	/** Короткая вспышка после выстрела 0..1 (для красного кольца прицела). */
	public static float dischargeFlash() {
		long since = System.currentTimeMillis() - lastFireMs;
		if (since >= FLASH_MS) {
			return 0f;
		}
		float t = 1f - since / (float) FLASH_MS;
		return t * t;
	}
}
