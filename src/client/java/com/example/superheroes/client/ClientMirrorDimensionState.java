package com.example.superheroes.client;

import com.example.superheroes.client.hud.MirrorWarpFlashHud;
import com.example.superheroes.client.iris.IrisShaderBridge;
import com.example.superheroes.network.MirrorDimensionStatusC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Client-side state of being trapped in the Mirror Dimension. Owns the
 * deadman switch: if the server stops sending keepalives (crash, kick,
 * packet loss) the client restores its own shaders after a short grace
 * period instead of staying acid-warped forever.
 *
 * Every Iris reload (apply / mode switch / restore) is hidden behind the black
 * {@link MirrorWarpFlashHud} so the unavoidable pipeline-reload freeze is not
 * visible.
 */
public final class ClientMirrorDimensionState {
	/** 5 seconds without a keepalive -> self-restore. */
	private static final int DEADMAN_TICKS = 100;

	private static boolean active;
	private static int ticksSinceKeepalive;
	private static int activeMode;
	private static int activeScale;

	private ClientMirrorDimensionState() {
	}

	public static void activate(int mode, int scale) {
		activeMode = mode;
		activeScale = scale;
		if (!IrisShaderBridge.canWarp()) {
			// No Iris / no Acid pack -> no reload happens, so no flash; just report.
			sendStatus(IrisShaderBridge.applyAcid(mode, scale));
			return;
		}
		MirrorWarpFlashHud.flashAndRun(() -> {
			int status = IrisShaderBridge.applyAcid(mode, scale);
			sendStatus(status);
			active = status == MirrorDimensionStatusC2SPayload.OK_APPLIED;
			ticksSinceKeepalive = 0;
		});
	}

	/** Change MODE/J on an already-active warp (mode-cycle ability). */
	public static void switchMode(int mode, int scale) {
		activeMode = mode;
		activeScale = scale;
		if (!active || !IrisShaderBridge.canWarp()) {
			return;
		}
		MirrorWarpFlashHud.flashAndRun(() -> IrisShaderBridge.reassertAcid(mode, scale));
	}

	public static void deactivate(boolean reportToServer) {
		if (!active) {
			return;
		}
		active = false;
		ticksSinceKeepalive = 0;
		if (reportToServer && IrisShaderBridge.canWarp()) {
			// Server told us to release -> hide the restore reload behind black too.
			MirrorWarpFlashHud.flashAndRun(() -> {
				if (IrisShaderBridge.restore()) {
					sendStatus(MirrorDimensionStatusC2SPayload.OK_RESTORED);
				}
			});
		} else {
			// Deadman / disconnect: just restore, no point flashing.
			boolean restored = IrisShaderBridge.restore();
			if (reportToServer && restored) {
				sendStatus(MirrorDimensionStatusC2SPayload.OK_RESTORED);
			}
		}
	}

	public static void keepalive() {
		ticksSinceKeepalive = 0;
		// Способность ещё активна, но warp слетел (чужой reload Iris / ручное
		// переключение шейдеров) — молча вернуть его. reassert делает reload только
		// когда warp реально пропал, поэтому обычные keepalive'ы ничего не стоят.
		if (active && !MirrorWarpFlashHud.isCovering() && !IrisShaderBridge.isAcidWarpActive()) {
			MirrorWarpFlashHud.flashAndRun(() -> IrisShaderBridge.reassertAcid(activeMode, activeScale));
		}
	}

	public static void tick(Minecraft client) {
		// Safety net: if a GUI screen stalls rendering, still run the pending reload.
		MirrorWarpFlashHud.fallbackTick();
		if (!active) {
			return;
		}
		// На паузе (альт-таб в одиночке -> встроенный сервер встаёт и не шлёт
		// keepalive) НЕ накручиваем deadman-таймер: иначе через 5с ложно снимем
		// шейдер, хотя способность ещё активна. Шейдер не должен пропадать.
		if (client.isPaused()) {
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
