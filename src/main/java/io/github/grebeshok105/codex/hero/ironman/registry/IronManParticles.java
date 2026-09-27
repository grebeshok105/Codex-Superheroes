package io.github.grebeshok105.codex.hero.ironman.registry;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Iron Man's particle types — byte-identical to the rows {@code ModParticles} used to own.
 * Registered eagerly by {@code IronManModule.register}.
 */
public final class IronManParticles {
	public static final SimpleParticleType REPULSOR_SPARK = register("repulsor_spark");
	public static final SimpleParticleType UNIBEAM_SPARK = register("unibeam_spark");

	private IronManParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModId.of(name), FabricParticleTypes.simple());
	}

	/** Forces class initialization during module register. */
	public static void init() {
	}
}
