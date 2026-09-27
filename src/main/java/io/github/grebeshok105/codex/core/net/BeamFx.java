package io.github.grebeshok105.codex.core.net;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Senders for the unified {@link BeamFxS2CPayload}. The audience asymmetry is part
 * of the visual contract: lasers are tracking-only (the shooter draws a local
 * first-person overlay instead), repulsor/cosmic beams go to tracking + self.
 */
public final class BeamFx {
	private BeamFx() {
	}

	public static void laser(Entity shooter, Vec3 start, Vec3 end) {
		FxBroadcast.tracking(shooter,
				new BeamFxS2CPayload(BeamFxS2CPayload.STYLE_LASER, shooter.getUUID(), start, end));
	}

	public static void repulsor(ServerPlayer shooter, Vec3 start, Vec3 end) {
		FxBroadcast.trackingAndSelf(shooter,
				new BeamFxS2CPayload(BeamFxS2CPayload.STYLE_REPULSOR, shooter.getUUID(), start, end));
	}

	public static void cosmicBeam(ServerPlayer shooter, Vec3 start, Vec3 end) {
		FxBroadcast.trackingAndSelf(shooter,
				new BeamFxS2CPayload(BeamFxS2CPayload.STYLE_COSMIC_BEAM, shooter.getUUID(), start, end));
	}
}
