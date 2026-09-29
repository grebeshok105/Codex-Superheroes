package io.github.grebeshok105.codex.client.core.vfx;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a {@link VfxEffectFactory} needs to create a one-shot effect.
 * {@code source} is {@code null} for world-fixed effects
 * ({@code VfxEventS2CPayload.NO_SOURCE}) or a source entity that already left
 * the client level; {@code sourceEntityId} always carries the raw wire id so
 * effects can key state on it even before the entity resolves client-side.
 * {@code seed} mirrors the server's random seed so every observer re-derives
 * the same effect randomness.
 */
public record VfxSpawn(ResourceLocation effect, @Nullable Entity source, int sourceEntityId,
		Vec3 origin, Vec3 target, float scale, int seed, VfxParams params) {
}
