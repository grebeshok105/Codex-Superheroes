package io.github.grebeshok105.codex.hero.kratos.registry;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Kratos's particle types, registered directly (R23 — no shared {@code ModParticles} row).
 * The client module owns the matching factory registrations.
 */
public final class KratosParticles {
	public static final SimpleParticleType KRATOS_HAND_BURST_1 = register("kratos_hand_burst_1");
	public static final SimpleParticleType KRATOS_HAND_BURST_2 = register("kratos_hand_burst_2");
	public static final SimpleParticleType KRATOS_HAND_BURST_3 = register("kratos_hand_burst_3");

	private KratosParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModId.of(name), FabricParticleTypes.simple());
	}

	/** Forces class init so the static registrations run; called from the module register. */
	public static void register() {
	}
}
