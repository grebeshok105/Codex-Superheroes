package io.github.grebeshok105.codex.mechanic.boundweapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Stamped on every weapon an ability issues: which player it belongs to and which issue it is.
 * Only the owner's current issue is valid; every older copy is stale.
 */
public record BoundWeaponToken(UUID owner, long issue) {
	public static final Codec<BoundWeaponToken> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("owner").forGetter(BoundWeaponToken::owner),
			Codec.LONG.fieldOf("issue").forGetter(BoundWeaponToken::issue)
	).apply(instance, BoundWeaponToken::new));

	public static final StreamCodec<ByteBuf, BoundWeaponToken> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, BoundWeaponToken::owner,
			ByteBufCodecs.VAR_LONG, BoundWeaponToken::issue,
			BoundWeaponToken::new);
}
