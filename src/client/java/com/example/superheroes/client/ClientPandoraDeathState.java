package com.example.superheroes.client;

import com.example.superheroes.client.iris.PandoraVanitasBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side state for Pandora's death cinematic (#6–#10). Server-authoritative: it is turned
 * on/off by START/END packets ({@code PandoraCinematicS2CPayload}).
 *
 * <p>While active:
 * <ul>
 *   <li>vanilla HUD is force-hidden (F1) and the mod's own overlays bail out (#8);</li>
 *   <li>mouse + keyboard input and camera rotation are blocked by dedicated mixins (#7, #10);</li>
 *   <li>the Iris "Vanitas" shaderpack is swapped in (black silhouettes + red sky, #6);</li>
 *   <li>black soundless lightning is drawn around Pandora's anchor (#9).</li>
 * </ul>
 *
 * <p>A hard safety timeout force-clears the state so a dropped END packet can never leave the
 * player permanently frozen / input-locked.
 */
public final class ClientPandoraDeathState {
	/** Server runs 100 ticks (5s); allow generous slack before the client self-heals. */
	private static final long SAFETY_TIMEOUT_MS = 9000L;

	private static volatile boolean active;
	private static long startMs;
	private static int pandoraId = -1;
	private static int killerId = -1;
	private static Vec3 anchor = Vec3.ZERO;
	private static boolean hudWasHidden;

	private ClientPandoraDeathState() {
	}

	public static void start(int pandoraEntityId, int killerEntityId, double x, double y, double z) {
		boolean wasActive = active;
		pandoraId = pandoraEntityId;
		killerId = killerEntityId;
		anchor = new Vec3(x, y, z);
		startMs = System.currentTimeMillis();
		active = true;
		if (!wasActive) {
			Minecraft mc = Minecraft.getInstance();
			hudWasHidden = mc.options.hideGui;
			mc.options.hideGui = true; // forced F1 — ALL HUD elements vanish (#8)
			PandoraVanitasBridge.apply(); // black-silhouette + red-gradient shaderpack (#6)
		}
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
		Minecraft.getInstance().options.hideGui = hudWasHidden;
		PandoraVanitasBridge.restore();
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

	public static Vec3 anchor() {
		return anchor;
	}

	/** Is the local player the one being "killed" (Pandora)? */
	public static boolean localIsPandora() {
		Minecraft mc = Minecraft.getInstance();
		return active && mc.player != null && mc.player.getId() == pandoraId;
	}

	public static void onDisconnect() {
		// Drop straight out without touching shader config across a disconnect; the Iris
		// restore-on-crash snapshot heals the shader state on next login if needed.
		active = false;
		pandoraId = -1;
		killerId = -1;
		Minecraft.getInstance().options.hideGui = hudWasHidden;
	}
}
