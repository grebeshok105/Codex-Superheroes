package com.example.superheroes.resource;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum ResourceKind implements StringRepresentable {
	ENERGY("energy"),
	MANA("mana");

	public static final Codec<ResourceKind> CODEC = StringRepresentable.fromEnum(ResourceKind::values);
	public static final IntFunction<ResourceKind> BY_ID = id -> values()[Math.floorMod(id, values().length)];
	public static final StreamCodec<ByteBuf, ResourceKind> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ResourceKind::ordinal);

	private final String name;

	ResourceKind(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
