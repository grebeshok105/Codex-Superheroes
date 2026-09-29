package io.github.grebeshok105.codex.core.particle;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * {@code superheroes:silent} — a render-nothing particle type substituted into
 * the {@code Level.explode(..., ParticleOptions small, ParticleOptions large,
 * Holder<SoundEvent>)} overload whenever a Visual Core event owns the
 * presentation. {@code assets/superheroes/particles/silent.json} declares an
 * empty {@code textures} list and the client registers a no-op provider, so
 * nothing spawns and nothing renders — that is the point.
 */
public final class SilentParticles {
	public static final SimpleParticleType SILENT = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, ModId.of("silent"), FabricParticleTypes.simple());

	private SilentParticles() {
	}

	/** Forces class initialization so the type is registered. */
	public static void init() {
	}
}
