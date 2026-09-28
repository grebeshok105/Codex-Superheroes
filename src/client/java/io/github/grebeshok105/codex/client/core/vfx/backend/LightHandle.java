package io.github.grebeshok105.codex.client.core.vfx.backend;

import net.minecraft.world.phys.Vec3;

/** Live handle to a backend-created dynamic light. */
public interface LightHandle {
	void move(Vec3 pos);

	void set(int rgb, float radius, float brightness);

	void remove();
}
