package com.example.superheroes.client;

import com.example.superheroes.client.iris.IrisShaderBridge;
import com.example.superheroes.network.MirrorDimensionStatusC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Client-side state of being trapped in the Mirror Dimension. Owns the
 * deadman switch: if the server stops sending keepalives (crash, kick,
 * packet loss) the client restores its own shaders after a short grace
 * period instead of staying acid-warped forever.
 */
public final class ClientMirrorDimensionState {
	/** 5 seconds without a keepalive -> self-restore. */
	private static final int DEADMAN_TICKS = 100;

	private static boolean active;
	private static int ticksSinceKeepalive;

	private ClientMirrorDimensionState() {
	}

	public static void activate(int mode, int scale) {
		int status = IrisShaderBridge.applyAcid(mode, scale);
		sendStatus(status);
		if (status == MirrorDimensionStatusC2SPayload.OK_APPLIED) {
			active = true;
			ticksSinceKeepalive = 0;
		}
	}

	public static void deactivate(boolean reportToServer) {
		if (!active) {
			return;
		}
		active = false;
		ticksSinceKeepalive = 0;
		boolean restored = IrisShaderBridge.restore();
		if (reportToServer && restored) {
			sendStatus(MirrorDimensionStatusC2SPayload.OK_RESTORED);
		}
	}

	public static void keepalive() {
		ticksSinceKeepalive = 0;
	}

	public static void tick(Minecraft client) {
		if (!active) {
			return;
		}
		ticksSinceKeepalive++;
		if (ticksSinceKeepalive > DEADMAN_TICKS) {
			deactivate(false);
		}
	}

	public static void onDisconnect() {
		deactivate(false);
	}

	private static void sendStatus(int status) {
		if (ClientPlayNetworking.canSend(MirrorDimensionStatusC2SPayload.TYPE)) {
			ClientPlayNetworking.send(new MirrorDimensionStatusC2SPayload(status));
		}
	}
}
