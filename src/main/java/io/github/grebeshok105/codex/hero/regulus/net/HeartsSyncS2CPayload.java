package io.github.grebeshok105.codex.hero.regulus.net;

import io.github.grebeshok105.codex.ModId;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Owner-unicast little-king view: bearer entity network ids (for client-side lookup),
 * whether lion heart is currently active, and its overheat counter. Sent on the 20-tick
 * cadence only when ANY field changed — an overheat change alone re-sends the packet
 * (per the plan's dirty rule), which keeps the ids list consistent with the owner set.
 */
public record HeartsSyncS2CPayload(
		List<Integer> heartEntityIds,
		boolean lionHeartActive,
		int overheatTicks
) implements CustomPacketPayload {
	public static final Type<HeartsSyncS2CPayload> TYPE = new Type<>(ModId.of("hearts_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HeartsSyncS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), HeartsSyncS2CPayload::heartEntityIds,
					ByteBufCodecs.BOOL, HeartsSyncS2CPayload::lionHeartActive,
					ByteBufCodecs.VAR_INT, HeartsSyncS2CPayload::overheatTicks,
					HeartsSyncS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
