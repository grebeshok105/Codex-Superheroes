package com.example.superheroes.client;

/**
 * Клиентское «состояние репульсоров» Железного Человека для HUD: когда репульсор
 * стреляет (добавляется трассер), значение вспыхивает к 1.0 и плавно остывает.
 * Чисто визуальный сигнал «энергии в ладонях» для индикатора заряда на HUD.
 */
public final class ClientRepulsorChargeState {
	private static volatile long lastFireMs = 0L;
	private static volatile float lastCharge = 0f;
	private static final long COOL_MS = 900L;

	private ClientRepulsorChargeState() {
	}

	/** Вызывается при выстреле репульсора (из рендера трассера). */
	public static void flash() {
		lastFireMs = System.currentTimeMillis();
		lastCharge = 1f;
	}

	/** Текущий «заряд» 0..1 (пик после выстрела, остывает за COOL_MS). */
	public static float charge() {
		long since = System.currentTimeMillis() - lastFireMs;
		if (since >= COOL_MS) {
			return 0f;
		}
		float t = 1f - (since / (float) COOL_MS);
		// мягкая кривая остывания
		return t * t;
	}
}
