package com.example.superheroes.item;

/**
 * Клиентский флаг видимости админ-предметов в креатив-вкладке.
 * Живёт в main-сорссете, чтобы {@code displayItems} вкладки мог его читать;
 * выставляется только из клиентского приёмника {@code AdminBuildS2CPayload}.
 */
public final class AdminBuildVisibility {
	private static volatile boolean clientVisible;

	private AdminBuildVisibility() {
	}

	public static boolean isClientVisible() {
		return clientVisible;
	}

	public static void setClientVisible(boolean visible) {
		clientVisible = visible;
	}
}
