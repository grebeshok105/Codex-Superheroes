package com.example.superheroes.network;

import com.example.superheroes.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Джарвис сообщает Железному Человеку об обнаруженном герое:
 * ник игрока, id героя и дистанция (в блоках) на момент обнаружения.
 * Клиент проигрывает голосовую реплику и показывает инфопанель
 * строго после окончания звука.
 */
public record JarvisDetectionS2CPayload(String playerName, ResourceLocation heroId, int distance)
		implements CustomPacketPayload {
	public static final Type<JarvisDetectionS2CPayload> TYPE = new Type<>(ModId.of("jarvis_detection"));

	public static final StreamCodec<RegistryFriendlyByteBuf, JarvisDetectionS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, JarvisDetectionS2CPayload::playerName,
					ResourceLocation.STREAM_CODEC, JarvisDetectionS2CPayload::heroId,
					ByteBufCodecs.VAR_INT, JarvisDetectionS2CPayload::distance,
					JarvisDetectionS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
