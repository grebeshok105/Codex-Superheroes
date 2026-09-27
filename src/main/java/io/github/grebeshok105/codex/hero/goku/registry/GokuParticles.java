package io.github.grebeshok105.codex.hero.goku.registry;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Goku's particle types — registered directly per R23 (no {@code ModParticles} indirection). */
public final class GokuParticles {
	public static final SimpleParticleType GOKU_KI_AURA = register("goku_ki_aura");
	public static final SimpleParticleType GOKU_KAMEHAMEHA_CORE = register("goku_kamehameha_core");
	public static final SimpleParticleType GOKU_KAMEHAMEHA_TRAIL = register("goku_kamehameha_trail");

	private GokuParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModId.of(name), FabricParticleTypes.simple());
	}

	/** Forces class initialization during the module's {@code register} so the types exist. */
	public static void register() {
	}
}
