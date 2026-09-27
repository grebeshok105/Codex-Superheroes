package io.github.grebeshok105.codex.client.hero.sungjinwoo.state;

import io.github.grebeshok105.codex.client.ClientSessionState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Состояние армии теней Сон Джи Ву на клиенте.
 *  - Per-player: hasShadows (для авто-свапа фазы скина) и count (для HUD-индикатора).
 */
public final class ClientShadowArmyState {
	private static final class Maps {
		final Map<UUID, ArmyInfo> armies = new HashMap<>();
	}

	private static final Maps MAPS = new Maps();

	static {
		ClientSessionState.register(ClientShadowArmyState::clear);
	}

	private ClientShadowArmyState() {
	}

	public static void update(UUID playerId, boolean hasShadows, int count, boolean phase2) {
		MAPS.armies.put(playerId, new ArmyInfo(hasShadows, count, phase2));
	}

	public static boolean hasShadows(UUID playerId) {
		ArmyInfo info = MAPS.armies.get(playerId);
		return info != null && info.hasShadows();
	}

	public static boolean isPhase2(UUID playerId) {
		ArmyInfo info = MAPS.armies.get(playerId);
		return info != null && info.phase2();
	}

	public static int count(UUID playerId) {
		ArmyInfo info = MAPS.armies.get(playerId);
		return info == null ? 0 : info.count();
	}

	public static void clear() {
		MAPS.armies.clear();
	}

	public record ArmyInfo(boolean hasShadows, int count, boolean phase2) {
	}
}
