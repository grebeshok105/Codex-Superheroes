package io.github.grebeshok105.codex.client.fx;

import io.github.grebeshok105.codex.core.particle.SilentParticles;
import io.github.grebeshok105.codex.particle.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.EndRodParticle;

/**
 * Shared FX wiring: the particle factories for every {@link ModParticles} type (plain
 * EndRod provider or the config-gated {@link CustomParticleGate}) and the per-tick hooks
 * of the screen-shake / wall-debris / flight-trail managers.
 */
public final class CoreFx {
	private CoreFx() {
	}

	public static void init() {
		ParticleFactoryRegistry.getInstance().register(ModParticles.TRANSFORM_SPARK, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.LASER_SPARK, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.WHITE_BOOM,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SWORD_EXPLOSION,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SPARKS,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.DARK_STAR,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.PURPLE_FLAME,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLACK_FLAME,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.DAZZLING,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SUN_PARTICLE,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SOUL_SPARK,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.NIGHTFALL,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.CHAOS_ORB,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.ANOMALY_SLICE,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.JIWALD_EFFECT,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.FULA_PARTICLE,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SHAMAK,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLUE_FLAME,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.MOONVEIL,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		// superheroes:silent mutes vanilla explosion particles — the provider
		// returns null so nothing renders (ParticleEngine handles a null result).
		ParticleFactoryRegistry.getInstance().register(SilentParticles.SILENT,
				(type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> null);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ScreenShakeManager.tick();
			WallImpactDebrisManager.tick(client.level);
			FlightTrailManager.tick(client);
		});
	}
}
