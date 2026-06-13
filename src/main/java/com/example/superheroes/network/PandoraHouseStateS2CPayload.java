package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells Pandora's OWN client whether her House of Vanity is currently open (#2).
 *
 * <p>While {@code open == false} the client hides every Pandora ability except the House entry
 * ({@code MIRROR_DIMENSION}); once she is inside her House the dimension-only abilities are
 * revealed. Sent only to the caster on House start/stop.
 */
public record PandoraHouseStateS2CPayload(boolean open) implements CustomPacketPayload {

	public static final Type<PandoraHouseStateS2CPayload> TYPE = new Type<>(ModId.of("pandora_house_state"));

	public static final StreamCodec<ByteBuf, PandoraHouseStateS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PandoraHouseStateS2CPayload::open,
			PandoraHouseStateS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
