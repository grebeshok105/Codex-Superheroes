package com.example.superheroes.client.fx;

import com.example.superheroes.client.ClientFlightState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Чистый клиентский след полёта на ванильных частицах (END_ROD + CLOUD):
 * без кастомных текстур, ничего оранжевого и никаких missing-texture квадратов.
 */
public final class FlightTrailManager {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final double MIN_SPEED_SQR = 0.02;

	private FlightTrailManager() {
	}

	public static void tick(Minecraft client) {
		if (client.level == null || client.player == null || client.isPaused()) {
			return;
		}
		for (Player player : client.level.players()) {
			if (ClientFlightState.get(player.getId()) == null) {
				continue;
			}
			Vec3 velocity = player.getDeltaMovement();
			if (velocity.lengthSqr() < MIN_SPEED_SQR) {
				continue;
			}
			spawnTrail(client, player, velocity);
		}
	}

	private static void spawnTrail(Minecraft client, Player player, Vec3 velocity) {
		Vec3 back = player.position()
				.add(0.0, player.getBbHeight() * 0.45, 0.0)
				.subtract(velocity.normalize().scale(0.6));
		for (int i = 0; i < 2; i++) {
			client.level.addParticle(ParticleTypes.END_ROD,
					back.x + (RANDOM.nextDouble() - 0.5) * 0.35,
					back.y + (RANDOM.nextDouble() - 0.5) * 0.35,
					back.z + (RANDOM.nextDouble() - 0.5) * 0.35,
					-velocity.x * 0.05, -velocity.y * 0.05, -velocity.z * 0.05);
		}
		if (RANDOM.nextInt(3) == 0) {
			client.level.addParticle(ParticleTypes.CLOUD,
					back.x, back.y, back.z,
					-velocity.x * 0.02, -velocity.y * 0.02, -velocity.z * 0.02);
		}
	}
}
