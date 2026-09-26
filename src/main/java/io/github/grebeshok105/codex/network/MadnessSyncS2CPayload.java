package io.github.grebeshok105.codex.network;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MadnessSyncS2CPayload(
		boolean madness,
		boolean bonusLifeAvailable,
		long readingRemainingMs,
		long manaLockRemainingMs
) implements CustomPacketPayload {
	public static final Type<MadnessSyncS2CPayload> TYPE = new Type<>(ModId.of("madness_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MadnessSyncS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BOOL, MadnessSyncS2CPayload::madness,
					ByteBufCodecs.BOOL, MadnessSyncS2CPayload::bonusLifeAvailable,
					ByteBufCodecs.VAR_LONG, MadnessSyncS2CPayload::readingRemainingMs,
					ByteBufCodecs.VAR_LONG, MadnessSyncS2CPayload::manaLockRemainingMs,
					MadnessSyncS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
