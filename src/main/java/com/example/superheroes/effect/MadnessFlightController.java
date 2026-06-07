package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class MadnessFlightController {
	private static final float HARDNESS_LIMIT = 20.0f;
	private static final int CHECKS_PER_SLICE = 22;
	private static final int JITTER_RADIUS = 4;
	private static final double JAGGED_SPHERE_RADIUS_SQ = 9.0;
	private static final float SKIP_PROBABILITY = 0.28f;
	private static final double FEET_PROTECT_RADIUS_XZ = 1.6;
	private static final double FEET_PROTECT_DEPTH = 0.5;
	private static final double[] AHEAD_DISTANCES = { 1.2, 2.4, 3.6, 4.8 };

	private MadnessFlightController() {
	}

	public static void init() {
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	private static void tick(ServerPlayer player) {
		if (!ModEffects.isMadness(player)) {
			return;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !data.isActive(AbilityIds.FLIGHT)) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		Vec3 view = player.getViewVector(1f);
		Vec3 motionH = new Vec3(motion.x, 0, motion.z);
		Vec3 viewH = new Vec3(view.x, 0, view.z);
		Vec3 forwardH;
		if (motionH.lengthSqr() > 0.01) {
			forwardH = motionH.normalize();
		} else if (viewH.lengthSqr() > 1e-6) {
			forwardH = viewH.normalize();
		} else {
			return;
		}
		Vec3 dir = motion.lengthSqr() > 0.01 ? motion.normalize() : view;

		ServerLevel level = player.serverLevel();
		Vec3 chest = player.position().add(0, player.getBbHeight() * 0.6, 0);
		Vec3 playerPos = player.position();
		boolean broke = false;
		for (double d : AHEAD_DISTANCES) {
			Vec3 ahead = chest.add(dir.scale(d));
			BlockPos center = BlockPos.containing(ahead);
			broke |= breakJagged(level, player, center, playerPos, forwardH);
		}
		if (broke) {
			level.sendParticles(ParticleTypes.EXPLOSION,
					player.getX() + dir.x, player.getY() + 1.0 + dir.y, player.getZ() + dir.z,
					1, 0.2, 0.2, 0.2, 0.0);
			if (player.tickCount % 6 == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.45f, 1.7f);
			}
		}
	}

	private static boolean breakJagged(ServerLevel level, ServerPlayer player, BlockPos center, Vec3 playerPos, Vec3 forwardH) {
		RandomSource rand = level.getRandom();
		boolean broke = false;
		double feetY = playerPos.y;
		for (int i = 0; i < CHECKS_PER_SLICE; i++) {
			int dx = rand.nextInt(JITTER_RADIUS * 2 + 1) - JITTER_RADIUS;
			int dy = rand.nextInt(JITTER_RADIUS * 2 + 1) - JITTER_RADIUS;
			int dz = rand.nextInt(JITTER_RADIUS * 2 + 1) - JITTER_RADIUS;
			if (dx * dx + dy * dy + dz * dz > JAGGED_SPHERE_RADIUS_SQ) {
				continue;
			}
			if (rand.nextFloat() < SKIP_PROBABILITY) {
				continue;
			}
			BlockPos pos = center.offset(dx, dy, dz);
			double bx = pos.getX() + 0.5 - playerPos.x;
			double by = pos.getY() + 0.5 - feetY;
			double bz = pos.getZ() + 0.5 - playerPos.z;
			double horizontalDot = bx * forwardH.x + bz * forwardH.z;
			if (horizontalDot <= 0.2) {
				continue;
			}
			double horizontalDist = Math.sqrt(bx * bx + bz * bz);
			if (by < -FEET_PROTECT_DEPTH && horizontalDist < FEET_PROTECT_RADIUS_XZ) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.liquid()) {
				continue;
			}
			float hardness = state.getDestroySpeed(level, pos);
			if (hardness < 0f || hardness >= HARDNESS_LIMIT) {
				continue;
			}
			level.destroyBlock(pos, false, player);
			broke = true;
		}
		return broke;
	}
}
