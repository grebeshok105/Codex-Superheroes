package io.github.grebeshok105.codex.client.core.vfx.anchor;

import net.minecraft.world.phys.Vec3;

/** World-space positions of a humanoid's two eyes. */
public record EyePair(Vec3 left, Vec3 right) {
}
