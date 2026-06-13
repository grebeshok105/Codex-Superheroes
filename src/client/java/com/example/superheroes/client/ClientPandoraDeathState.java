package com.example.superheroes.client;

/**
 * Client-side state for Pandora's revival cut-scene (reworked V4). Server-authoritative: toggled
 * on/off by START/END packets ({@code PandoraCinematicS2CPayload}).
 *
 * <p>While active the player is frozen — mouse + keyboard input and camera rotation are blocked by
 * dedicated mixins ({@code PandoraCinematic*Mixin}) — and the title overlay
 * ({@link com.example.superheroes.client.hud.PandoraDeathTitleHud}) shows «Ты думал меня так
 * легко убить?» at the top of the screen. No shaderpack, no forced F1.
 *
 * <p>A hard safety timeout force-clears the state so a dropped END packet can never leave the
 * player permanently frozen / input-locked.
 */
public final class ClientPandoraDeathState {
	/** Server runs 96 ticks (~4.8s); allow generous slack before the client self-heals. */
	private static final long SAFETY_TIMEOUT_MS = 7000L;

	private static volatile boolean active;
	private static long startMs;
	private static int pandoraId = -1;
	private static int killerId = -1;

	private ClientPandoraDeathState() {
	}

	public static void start(int pandoraEntityId, int killerEntityId, double x, double y, double z) {
		pandoraId = pandoraEntityId;
		killerId = killerEntityId;
		startMs = System.currentTimeMillis();
		active = true;
	}

	public static void end() {
		clear();
	}

	private static void clear() {
		if (!active) {
			return;
		}
		active = false;
		pandoraId = -1;
		killerId = -1;
	}

	/** Polled each client tick; force-clears on a missed END packet. */
	public static void tick() {
		if (active && System.currentTimeMillis() - startMs > SAFETY_TIMEOUT_MS) {
			clear();
		}
	}

	public static boolean active() {
		return active;
	}

	public static int pandoraId() {
		return pandoraId;
	}

	public static int killerId() {
		return killerId;
	}

	public static void onDisconnect() {
		active = false;
		pandoraId = -1;
		killerId = -1;
	}
}
