package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.core.net.PhotonFx;
import io.github.grebeshok105.codex.core.net.PhotonFxS2CPayload.RotationMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Regulus' Photon fx ids and the tiny choreography helpers built on {@link PhotonFx}.
 * The .fx assets ship under {@code assets/vfxlab/fx/} and were authored through the
 * VFXLab fx-edit lane (clone → patch → validate → register).
 */
public final class RegulusFx {
	public static final ResourceLocation SUN_RING_BURST =
			ResourceLocation.fromNamespaceAndPath("vfxlab", "sun_ring_burst");
	public static final ResourceLocation SOLAR_FLASH =
			ResourceLocation.fromNamespaceAndPath("vfxlab", "solar_flash");
	public static final ResourceLocation SOLAR_AURA_RING =
			ResourceLocation.fromNamespaceAndPath("vfxlab", "solar_aura_ring");
	public static final ResourceLocation GREED_RING =
			ResourceLocation.fromNamespaceAndPath("vfxlab", "greed_ring");

	private static final Vec3 FEET = new Vec3(0, -1.15, 0);
	private static final Vec3 CROWN = new Vec3(0, 0.55, 0);
	// Ground-level fx callers pass feet/anchor positions that sit exactly on a block
	// surface — spawn a hair above so the flat ring isn't z-fighting it.
	private static final Vec3 GROUND_RING = new Vec3(0, 0.25, 0);
	private static final Vec3 GROUND_FLASH = new Vec3(0, 0.6, 0);

	private RegulusFx() {
	}

	/** Golden ground shockwave at the entity's feet. */
	public static void ringBurst(Entity anchor) {
		PhotonFx.follow(anchor, SUN_RING_BURST, FEET, RotationMode.NONE, 1f, 0, true);
	}

	/** Sharp solar pop at crown height. */
	public static void flash(Entity anchor) {
		PhotonFx.follow(anchor, SOLAR_FLASH, CROWN, RotationMode.NONE, 1f, 0, true);
	}

	/** Sustained heartbeat ring — one pulse per aura tick. */
	public static void auraPulse(Entity anchor) {
		PhotonFx.follow(anchor, SOLAR_AURA_RING, FEET, RotationMode.NONE, 1f, 0, true);
	}

	/** Crimson ring pulse (greed mark / release). */
	public static void greedPulse(Entity anchor) {
		PhotonFx.follow(anchor, GREED_RING, FEET, RotationMode.NONE, 1f, 0, true);
	}

	public static void ringBurstAt(ServerLevel level, Vec3 pos) {
		PhotonFx.at(level, pos, SUN_RING_BURST, GROUND_RING, 1f, 0, true);
	}

	public static void flashAt(ServerLevel level, Vec3 pos) {
		PhotonFx.at(level, pos, SOLAR_FLASH, GROUND_FLASH, 1f, 0, true);
	}

	public static void greedPulseAt(ServerLevel level, Vec3 pos) {
		PhotonFx.at(level, pos, GREED_RING, GROUND_RING, 1f, 0, true);
	}
}
