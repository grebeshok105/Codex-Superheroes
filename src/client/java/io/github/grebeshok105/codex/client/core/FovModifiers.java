package io.github.grebeshok105.codex.client.core;

import net.minecraft.client.Camera;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Module-registered fov adjustments (zoom channels, cinematic reads, …). Each registered
 * {@link FovModifier} receives the fov computed so far; an inactive modifier returns its
 * input untouched, so with nothing registered {@link #applyAll} is the identity.
 */
public final class FovModifiers {
	private static final List<FovModifier> MODIFIERS = new CopyOnWriteArrayList<>();

	private FovModifiers() {
	}

	public static void register(FovModifier modifier) {
		MODIFIERS.add(modifier);
	}

	public static double applyAll(Camera camera, float partialTick, double fov) {
		double value = fov;
		for (FovModifier modifier : MODIFIERS) {
			value = modifier.apply(camera, partialTick, value);
		}
		return value;
	}
}
