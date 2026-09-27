package io.github.grebeshok105.codex.mechanic.flight;

import io.github.grebeshok105.codex.core.net.FxBroadcast;
import net.minecraft.server.level.ServerPlayer;

public final class FlightSync {
	private FlightSync() {
	}

	public static void sync(ServerPlayer player, FlightMode mode, FlightPhase phase, float horizontalSpeed, boolean active) {
		FlightStateS2CPayload payload = new FlightStateS2CPayload(
				player.getId(), active, mode.ordinal(), phase.ordinal(), horizontalSpeed);
		FxBroadcast.trackingAndSelf(player, payload);
	}
}
