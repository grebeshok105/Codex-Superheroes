package io.github.grebeshok105.codex.client.core;

import net.minecraft.client.Camera;

/**
 * One link in the {@link FovModifiers} chain: takes the fov computed so far and returns the
 * value the next link sees. Registered per hero module; inactive links must return
 * {@code fov} unchanged. Runs inside {@code GameRenderer#getFov} at RETURN.
 */
@FunctionalInterface
public interface FovModifier {
	double apply(Camera camera, float partialTick, double fov);
}
