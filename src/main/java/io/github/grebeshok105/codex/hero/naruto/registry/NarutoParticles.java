package io.github.grebeshok105.codex.hero.naruto.registry;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Naruto's particle types — registered directly per R23 (no {@code ModParticles} indirection). */
public final class NarutoParticles {
	public static final SimpleParticleType NARUTO_RASENGAN_SWIRL = register("naruto_rasengan_swirl");
	public static final SimpleParticleType NARUTO_CLONE_POOF = register("naruto_clone_poof");
	public static final SimpleParticleType NARUTO_KAWARIMI_SMOKE = register("naruto_kawarimi_smoke");

	private NarutoParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModId.of(name), FabricParticleTypes.simple());
	}

	/** Forces class initialization during the module's {@code register} so the types exist. */
	public static void register() {
	}
}
