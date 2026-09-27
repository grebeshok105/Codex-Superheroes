package io.github.grebeshok105.codex.hero.captainamerica.registry;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Captain America's particle types — registered directly per R23 (no {@code ModParticles} indirection). */
public final class CaptainAmericaParticles {
	public static final SimpleParticleType CAP_SHIELD_TRAIL = register("cap_shield_trail");
	public static final SimpleParticleType CAP_SHIELD_SLAM_BURST = register("cap_shield_slam_burst");

	private CaptainAmericaParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModId.of(name), FabricParticleTypes.simple());
	}

	/** Forces class initialization during the module's {@code register} so the types exist. */
	public static void register() {
	}
}
