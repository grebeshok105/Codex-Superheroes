package com.example.superheroes.client.fx;

import com.example.superheroes.client.config.SuperheroesClientConfig;
import com.example.superheroes.client.config.SuperheroesClientConfig.VfxMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Wrapper: возвращает null если игрок выбрал LEGACY VFX.
 * Так custom-партикл просто не появляется, а legacy-альтернатива (которая
 * шлётся параллельно из ParticlePresets) — отображается.
 */
public final class CustomParticleGate implements ParticleProvider<SimpleParticleType> {
	private final ParticleProvider<SimpleParticleType> delegate;

	public CustomParticleGate(SpriteSet spriteSet,
			java.util.function.Function<SpriteSet, ParticleProvider<SimpleParticleType>> factory) {
		this.delegate = factory.apply(spriteSet);
	}

	@Override
	public Particle createParticle(SimpleParticleType type, ClientLevel level,
			double x, double y, double z, double vx, double vy, double vz) {
		if (SuperheroesClientConfig.vfxMode() == VfxMode.LEGACY) return null;
		return delegate.createParticle(type, level, x, y, z, vx, vy, vz);
	}
}
