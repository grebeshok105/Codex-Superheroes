package com.example.superheroes.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Разрушение блоков при соприкосновении рывка с землёй/стеной.
 * Общая логика для вилтрумитских рывков Омнимена и Неуязвимого:
 * небольшая воронка по направлению движения с жёстким лимитом блоков.
 */
public final class RushTerrainBreaker {
	private static final float MAX_HARDNESS = 12.0f;
	private static final int DEBRIS_STATE_LIMIT = 4;

	private RushTerrainBreaker() {
	}

	/**
	 * Ломает блоки в небольшой сфере у точки контакта.
	 *
	 * @return сколько блоков разрушено (0 — путь не пробился, рывок стоит остановить)
	 */
	public static int breakContact(ServerLevel level, ServerPlayer player, Vec3 contact, Vec3 direction,
			double radius, int maxBlocks) {
		Vec3 center = contact.add(direction.normalize().scale(radius * 0.5));
		int blockRadius = (int) Math.ceil(radius);
		double radiusSqr = radius * radius;
		BlockPos centerPos = BlockPos.containing(center);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int destroyed = 0;
		float maxHardnessBroken = 0f;
		int debrisStates = 0;

		for (int dx = -blockRadius; dx <= blockRadius && destroyed < maxBlocks; dx++) {
			for (int dy = -blockRadius; dy <= blockRadius && destroyed < maxBlocks; dy++) {
				for (int dz = -blockRadius; dz <= blockRadius && destroyed < maxBlocks; dz++) {
					double distSqr = dx * dx + dy * dy + dz * dz;
					if (distSqr > radiusSqr) {
						continue;
					}
					pos.set(centerPos.getX() + dx, centerPos.getY() + dy, centerPos.getZ() + dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || !state.getFluidState().isEmpty()) {
						continue;
					}
					if (!BlockBreakPolicy.canImpactBreak(level, pos, state, MAX_HARDNESS)) {
						continue;
					}
					if (debrisStates < DEBRIS_STATE_LIMIT) {
						level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
								center.x, center.y, center.z, 5, radius * 0.4, radius * 0.4, radius * 0.4, 0.06);
						debrisStates++;
					}
					float hardness = state.getDestroySpeed(level, pos);
					if (hardness > maxHardnessBroken) {
						maxHardnessBroken = hardness;
					}
					if (level.destroyBlock(pos.immutable(), false, player)) {
						destroyed++;
					}
				}
			}
		}

		if (destroyed > 0) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
					10, radius * 0.5, radius * 0.5, radius * 0.5, 0.02);
			level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
					8, radius * 0.5, radius * 0.5, radius * 0.5, 0.02);
			level.playSound(null, center.x, center.y, center.z,
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
					Math.min(1.1f, 0.5f + maxHardnessBroken * 0.1f), 0.7f);
		}
		return destroyed;
	}

	/** Контакт с террейном: упёрся в стену, в пол или стоит на земле. */
	public static boolean touchingTerrain(ServerPlayer player) {
		return player.horizontalCollision || player.verticalCollision || player.onGround();
	}
}
