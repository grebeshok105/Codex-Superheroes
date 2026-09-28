package io.github.grebeshok105.codex.client.core.vfx;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Opens a {@link VfxChannelEffect} bound to {@code source} aiming at
 * {@code target}. Registered per channel id through
 * {@code HeroClientContext.vfxChannel} / {@link VfxRuntime#registerChannel}.
 */
@FunctionalInterface
public interface VfxChannelFactory {
	VfxChannelEffect open(Entity source, Vec3 target, VfxParams params);
}
