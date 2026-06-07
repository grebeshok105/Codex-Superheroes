package com.example.superheroes.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Серверный helper: отправляет параллельно «custom» и «legacy» партиклы.
 * Клиент сам решит, какие рисовать (см. SuperheroesClientConfig).
 *
 * Для простоты сейчас сервер отправляет ОБА варианта; клиентская сторона
 * фильтрует по vfxMode через mixin/render-hook. До тех пор оба видны.
 *
 * Реальная фильтрация: клиентский Particle factory смотрит на ClientConfig
 * и при LEGACY возвращает no-op для custom-particles.
 */
public final class ParticlePresets {
	private ParticlePresets() {
	}

	public static void shockwave(ServerLevel level, double x, double y, double z, double radius, int count) {
		send(level, ModParticles.WHITE_BOOM, ParticleTypes.EXPLOSION, x, y, z, count, radius * 0.4, 0.2, radius * 0.4, 0.05);
	}

	public static void sparks(ServerLevel level, double x, double y, double z, int count) {
		send(level, ModParticles.SPARKS, ParticleTypes.ELECTRIC_SPARK, x, y, z, count, 0.3, 0.3, 0.3, 0.1);
	}

	public static void darkBurst(ServerLevel level, double x, double y, double z, int count) {
		send(level, ModParticles.DARK_STAR, ParticleTypes.LARGE_SMOKE, x, y, z, count, 0.5, 0.5, 0.5, 0.05);
	}

	public static void purpleFlame(ServerLevel level, double x, double y, double z, int count) {
		send(level, ModParticles.PURPLE_FLAME, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, count, 0.2, 0.2, 0.2, 0.02);
	}

	public static void sunBurst(ServerLevel level, double x, double y, double z, int count) {
		send(level, ModParticles.SUN_PARTICLE, ParticleTypes.END_ROD, x, y, z, count, 0.3, 0.3, 0.3, 0.1);
	}

	private static void send(ServerLevel level, ParticleOptions custom, ParticleOptions legacy,
			double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		level.sendParticles(custom, x, y, z, count, dx, dy, dz, speed);
		level.sendParticles(legacy, x, y, z, count, dx, dy, dz, speed);
	}
}
