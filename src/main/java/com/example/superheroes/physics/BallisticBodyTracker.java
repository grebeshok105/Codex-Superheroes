package com.example.superheroes.physics;

import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.network.WallImpactDebrisS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Трекер баллистических тел — сущности, пробивающие стены по инерции
 */
public final class BallisticBodyTracker {
	private static final int MAX_LIFE_TICKS = 60;
	private static final double MIN_SPEED = 0.55;
	private static final float MAX_HARDNESS = 12.0f;
	private static final int MAX_BLOCKS_PER_TICK = 64;
	private static final int DEBRIS_STATE_LIMIT = 8;
	private static final double STEP_SIZE = 0.5;
	private static final double INFLATE = 0.35;
	private static final double SHAKE_RADIUS = 24.0;
	private static final double DEBRIS_RADIUS = 48.0;

	private static final HashMap<UUID, TrackedState> tracked = new HashMap<>();

	private BallisticBodyTracker() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(BallisticBodyTracker::tick);
	}

	public static void launch(LivingEntity body, Vec3 velocity, double power, @Nullable ServerPlayer source) {
		if (body == null || body.isRemoved() || body.isSpectator() || power <= 0) return;
		if (!(body.level() instanceof ServerLevel level)) return;
		UUID id = body.getUUID();
		TrackedState existing = tracked.get(id);
		if (existing != null) {
			existing.power = Math.max(power, existing.power);
			existing.lifeTicks = 0;
			return;
		}
		TrackedState state = new TrackedState(body, level, power, source);
		tracked.put(id, state);
		try {
			if (!tickBody(state)) {
				tracked.remove(id);
			}
		} catch (Exception ignored) {
			tracked.remove(id);
		}
	}

	public static void clear(LivingEntity body) {
		if (body != null) {
			tracked.remove(body.getUUID());
		}
	}

	private static void tick(MinecraftServer server) {
		if (tracked.isEmpty()) return;
		Iterator<Map.Entry<UUID, TrackedState>> it = tracked.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, TrackedState> entry = it.next();
			try {
				if (!tickBody(entry.getValue())) {
					it.remove();
				}
			} catch (Exception ignored) {
				it.remove();
			}
		}
	}

	private static boolean tickBody(TrackedState state) {
		LivingEntity body = state.body;
		if (body.isRemoved() || !body.isAlive()) return false;
		if (body.level() != state.level) return false;
		if (state.lifeTicks > MAX_LIFE_TICKS) return false;
		if (state.power <= 0) return false;

		Vec3 delta = body.getDeltaMovement();
		double speed = delta.length();
		if (speed < MIN_SPEED) return false;

		state.lifeTicks++;

		ServerLevel level = state.level;
		Vec3 start = body.position();
		Vec3 dir = delta.normalize();
		double totalDist = speed;
		int steps = Math.max(1, (int) Math.ceil(totalDist / STEP_SIZE));
		int blocksDestroyed = 0;

		for (int step = 0; step < steps && blocksDestroyed < MAX_BLOCKS_PER_TICK; step++) {
			double t = Math.min((step + 1) * STEP_SIZE, totalDist);
			Vec3 stepPos = start.add(dir.scale(t));
			AABB box = body.getBoundingBox().move(stepPos.subtract(body.position())).inflate(INFLATE, 0.0, INFLATE);

			BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
			BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

			boolean layerBrokeAny = false;
			float maxHardnessBroken = 0f;
			List<Integer> layerIds = new ArrayList<>();
			boolean hardStop = false;

			BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
			for (int x = min.getX(); x <= max.getX() && !hardStop; x++) {
				for (int y = min.getY(); y <= max.getY() && !hardStop; y++) {
					for (int z = min.getZ(); z <= max.getZ() && !hardStop; z++) {
						mpos.set(x, y, z);
						BlockState blockState = level.getBlockState(mpos);
						if (blockState.isAir() || !blockState.getFluidState().isEmpty()) continue;

						if (BlockBreakPolicy.canImpactBreak(level, mpos, blockState, MAX_HARDNESS)) {
							if (blocksDestroyed >= MAX_BLOCKS_PER_TICK) continue;
							if (layerIds.size() < DEBRIS_STATE_LIMIT) {
								layerIds.add(Block.getId(blockState));
							}
							float hardness = blockState.getDestroySpeed(level, mpos);
							if (hardness > maxHardnessBroken) maxHardnessBroken = hardness;
							if (level.destroyBlock(mpos.immutable(), false, state.source != null ? state.source : body)) {
								blocksDestroyed++;
							}
							layerBrokeAny = true;
						} else {
							float dmg = (float) (speed * 4.0);
							body.hurt(level.damageSources().flyIntoWall(), dmg);
							body.setDeltaMovement(0, body.getDeltaMovement().y, 0);
							body.hurtMarked = true;
							if (body instanceof ServerPlayer player) {
								player.connection.send(new ClientboundSetEntityMotionPacket(player));
							}
							sendWallFx(level, stepPos, dir, speed, layerIds);
							hardStop = true;
						}
					}
				}
			}

			if (hardStop) return false;

			if (layerBrokeAny) {
				state.power -= 6.0 + maxHardnessBroken;

				Vec3 vel = body.getDeltaMovement();
				body.setDeltaMovement(vel.scale(0.78));
				body.hurtMarked = true;
				if (body instanceof ServerPlayer player) {
					player.connection.send(new ClientboundSetEntityMotionPacket(player));
				}

				body.invulnerableTime = 0;
				body.hurt(level.damageSources().flyIntoWall(), (float) (2.0 + 0.6 * maxHardnessBroken));

				sendWallFx(level, stepPos, dir, speed, layerIds);

				speed = body.getDeltaMovement().length();
				if (speed < MIN_SPEED || state.power <= 0) return false;
			}
		}

		return true;
	}

	private static void sendWallFx(ServerLevel level, Vec3 pos, Vec3 dir, double speed, List<Integer> stateIds) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 14, 0.6, 0.6, 0.6, 0.02);
		level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, 0.6, 0.6, 0.6, 0.02);

		for (int id : stateIds) {
			BlockState blockState = Block.stateById(id);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
					pos.x, pos.y, pos.z, 6, 0.6, 0.6, 0.6, 0.05);
		}

		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7f, 0.75f);

		float intensity = Math.min(1.2f, 0.5f + (float) speed * 0.08f);
		int[] ids = stateIds.stream().mapToInt(Integer::intValue).toArray();
		WallImpactDebrisS2CPayload payload = new WallImpactDebrisS2CPayload(pos, dir, intensity, ids);
		for (ServerPlayer nearby : PlayerLookup.around(level, pos, DEBRIS_RADIUS)) {
			ServerPlayNetworking.send(nearby, payload);
		}

		for (ServerPlayer nearby : PlayerLookup.around(level, pos, SHAKE_RADIUS)) {
			double distance = nearby.position().distanceTo(pos);
			float falloff = (float) Math.max(0.0, 1.0 - distance / SHAKE_RADIUS);
			float shakeIntensity = 0.6f * (0.25f + falloff * 0.75f);
			ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(shakeIntensity, 10));
		}
	}

	private static final class TrackedState {
		final LivingEntity body;
		final ServerLevel level;
		double power;
		int lifeTicks;
		@Nullable
		final ServerPlayer source;

		TrackedState(LivingEntity body, ServerLevel level, double power, @Nullable ServerPlayer source) {
			this.body = body;
			this.level = level;
			this.power = power;
			this.lifeTicks = 0;
			this.source = source;
		}
	}
}
