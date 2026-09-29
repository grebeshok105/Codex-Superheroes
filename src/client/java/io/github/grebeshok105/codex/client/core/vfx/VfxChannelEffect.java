package io.github.grebeshok105.codex.client.core.vfx;

import net.minecraft.world.phys.Vec3;

/**
 * Continuous entity-bound effect driven by a {@code VfxChannelS2CPayload}
 * stream: START/UPDATE retarget it, STOP calls {@link #release()}.
 * {@code release()} starts the release phase; {@link #done()} turns true when
 * the release phase ends.
 */
public interface VfxChannelEffect extends VfxEffect {
	void retarget(Vec3 target);

	void release();
}
