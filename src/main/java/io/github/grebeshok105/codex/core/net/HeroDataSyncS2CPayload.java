package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HeroDataSyncS2CPayload(HeroData data) implements CustomPacketPayload {
	public static final Type<HeroDataSyncS2CPayload> TYPE = new Type<>(ModId.of("hero_data_sync"));

	public static final StreamCodec<ByteBuf, HeroDataSyncS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.fromCodec(HeroData.CODEC), HeroDataSyncS2CPayload::data,
			HeroDataSyncS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
