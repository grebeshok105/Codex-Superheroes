package io.github.grebeshok105.codex.core.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Senders for the typed VFX payloads. {@link #event} is the one-shot path,
 * {@link #channel} drives continuous effects — the source player always sees
 * its own channel, so the audience is tracking + self.
 */
public final class VfxFx {
	private VfxFx() {
	}

	/** Ticks between channel UPDATE packets from a continuous emitter. */
	public static final int CHANNEL_UPDATE_INTERVAL_TICKS = 2;

	public static void event(Entity source, ResourceLocation effect, Vec3 origin, Vec3 target, float scale) {
		VfxEventS2CPayload payload = new VfxEventS2CPayload(effect, source.getId(), origin, target, scale,
				source.level().random.nextInt());
		if (source instanceof ServerPlayer player) {
			FxBroadcast.trackingAndSelf(player, payload);
		} else {
			FxBroadcast.tracking(source, payload);
		}
	}

	public static void eventAround(ServerLevel level, ResourceLocation effect, Vec3 origin, Vec3 target,
			float scale, double radius) {
		FxBroadcast.around(level, origin, radius, new VfxEventS2CPayload(effect, VfxEventS2CPayload.NO_SOURCE,
				origin, target, scale, level.random.nextInt()));
	}

	public static void channel(ServerPlayer source, ResourceLocation channel, byte state, Vec3 target) {
		FxBroadcast.trackingAndSelf(source,
				new VfxChannelS2CPayload(source.getId(), channel, state, target));
	}
}
