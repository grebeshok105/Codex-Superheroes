package com.example.superheroes.core.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.Map;

/**
 * Per-player ability visibility computed on the server and synced to the owner through the
 * {@code superheroes:ability_availability} attachment (stage C4 of the client-modules plan).
 * Abilities missing from {@link #entries} are {@link Visibility#AVAILABLE}.
 */
public record AbilityAvailability(Map<ResourceLocation, Visibility> entries) {

	public static final AbilityAvailability EMPTY = new AbilityAvailability(Map.of());

	public static final Codec<AbilityAvailability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(ResourceLocation.CODEC, Visibility.CODEC)
					.optionalFieldOf("entries", Map.of())
					.forGetter(AbilityAvailability::entries)
	).apply(instance, AbilityAvailability::new));

	public static final StreamCodec<ByteBuf, AbilityAvailability> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(java.util.HashMap::new, ResourceLocation.STREAM_CODEC, Visibility.STREAM_CODEC),
			AbilityAvailability::entries,
			AbilityAvailability::new);

	public AbilityAvailability {
		entries = Map.copyOf(entries);
	}

	public Visibility visibilityOf(ResourceLocation abilityId) {
		return entries.getOrDefault(abilityId, Visibility.AVAILABLE);
	}

	/** How an ability renders on the owner's HUD: usable, shown but locked, or removed entirely. */
	public enum Visibility implements StringRepresentable {
		AVAILABLE, LOCKED, HIDDEN;

		public static final Codec<Visibility> CODEC = StringRepresentable.fromEnum(Visibility::values);
		public static final StreamCodec<ByteBuf, Visibility> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
