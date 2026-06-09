package com.example.superheroes.client.fx;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class WallImpactDebrisManager {
	private static final int MAX_ACTIVE_IMPACTS = 32;
	private static final int STRONG_TRAIL_TICKS = 100;
	private static final DustParticleOptions GRAY_DUST = new DustParticleOptions(new Vector3f(0.48f, 0.45f, 0.40f), 1.15f);
	private static final List<Impact> IMPACTS = new ArrayList<>();
	private static final Random RNG = new Random();

	private WallImpactDebrisManager() {
	}

	public static void spawn(Level level, Vec3 pos, Vec3 normalOrDirection, float intensity, int[] blockStateIds) {
		if (level == null || !level.isClientSide || pos == null) return;
		float power = Mth.clamp(intensity, 0.15f, 1.5f);
		Vec3 normal = normalize(normalOrDirection);
		BlockState[] states = resolveStates(blockStateIds);
		spawnInitialBurst(level, pos, normal, power, states);
		if (power >= 0.45f) {
			if (IMPACTS.size() >= MAX_ACTIVE_IMPACTS) {
				IMPACTS.remove(0);
			}
			IMPACTS.add(new Impact(pos, normal, power, states, 24 + Math.round(power * 76.0f)));
		}
	}

	public static void spawn(Level level, Vec3 pos, Vec3 normalOrDirection, float intensity) {
		spawn(level, pos, normalOrDirection, intensity, null);
	}

	public static void tick(Level level) {
		if (level == null || !level.isClientSide || IMPACTS.isEmpty()) return;
		Iterator<Impact> iterator = IMPACTS.iterator();
		while (iterator.hasNext()) {
			Impact impact = iterator.next();
			if (impact.tick(level)) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		IMPACTS.clear();
	}

	private static void spawnInitialBurst(Level level, Vec3 pos, Vec3 normal, float power, BlockState[] states) {
		int debris = 10 + Math.round(power * 22.0f);
		int dust = 8 + Math.round(power * 26.0f);
		for (int i = 0; i < debris; i++) {
			Vec3 velocity = scatterVelocity(normal, power, 0.18, 0.55);
			Vec3 start = pos.add(normal.scale(0.08 + RNG.nextDouble() * 0.18)).add(randomDisk(normal, 0.45 + power * 0.25));
			level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, pick(states)),
					start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
		}
		for (int i = 0; i < dust; i++) {
			Vec3 velocity = scatterVelocity(normal, power, 0.04, 0.16);
			Vec3 start = pos.add(randomDisk(normal, 0.65 + power * 0.35));
			level.addParticle(i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD,
					start.x, start.y, start.z, velocity.x, velocity.y + 0.02, velocity.z);
		}
		if (power >= 0.85f) {
			int smoke = 8 + Math.round(power * 12.0f);
			for (int i = 0; i < smoke; i++) {
				Vec3 velocity = scatterVelocity(normal, power, 0.02, 0.08);
				Vec3 start = pos.add(randomDisk(normal, 0.75 + power * 0.35));
				level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z, velocity.x, velocity.y + 0.04, velocity.z);
			}
		}
	}

	private static Vec3 scatterVelocity(Vec3 normal, float power, double base, double burst) {
		Vec3 tangent = randomDisk(normal, 1.0);
		double out = base + RNG.nextDouble() * burst * power;
		double side = (0.05 + RNG.nextDouble() * 0.22) * power;
		double lift = (RNG.nextDouble() - 0.25) * 0.18 * power;
		return normal.scale(out).add(tangent.scale(side)).add(0.0, lift, 0.0);
	}

	private static Vec3 randomDisk(Vec3 normal, double radius) {
		Vec3 up = Math.abs(normal.y) > 0.85 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 right = normal.cross(up).normalize();
		Vec3 tangent = right.cross(normal).normalize();
		double angle = RNG.nextDouble() * Math.PI * 2.0;
		double distance = Math.sqrt(RNG.nextDouble()) * radius;
		return right.scale(Math.cos(angle) * distance).add(tangent.scale(Math.sin(angle) * distance));
	}

	private static Vec3 normalize(Vec3 value) {
		if (value == null || value.lengthSqr() < 1.0E-5) {
			return new Vec3(0.0, 1.0, 0.0);
		}
		return value.normalize();
	}

	private static BlockState[] resolveStates(int[] ids) {
		if (ids == null || ids.length == 0) {
			return new BlockState[] { Blocks.STONE.defaultBlockState() };
		}
		List<BlockState> states = new ArrayList<>(Math.min(ids.length, 8));
		for (int id : ids) {
			BlockState state = Block.stateById(id);
			if (state != null && !state.isAir()) {
				states.add(state);
			}
			if (states.size() >= 8) break;
		}
		if (states.isEmpty()) {
			states.add(Blocks.STONE.defaultBlockState());
		}
		return states.toArray(BlockState[]::new);
	}

	private static BlockState pick(BlockState[] states) {
		return states[RNG.nextInt(states.length)];
	}

	private static final class Impact {
		private final Vec3 pos;
		private final Vec3 normal;
		private final float power;
		private final BlockState[] states;
		private final int maxAge;
		private int age;

		private Impact(Vec3 pos, Vec3 normal, float power, BlockState[] states, int maxAge) {
			this.pos = pos;
			this.normal = normal;
			this.power = power;
			this.states = states;
			this.maxAge = maxAge;
		}

		private boolean tick(Level level) {
			age++;
			if (age >= maxAge) return true;
			float fade = 1.0f - (float) age / (float) Math.max(1, maxAge);
			if (age % 5 == 0) {
				int count = power >= 0.85f ? 3 : 1;
				for (int i = 0; i < count; i++) {
					Vec3 drift = scatterVelocity(normal, power * fade, 0.01, 0.05);
					Vec3 start = pos.add(normal.scale(0.05)).add(randomDisk(normal, 0.45 + power * 0.45));
					level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z, drift.x, drift.y + 0.025, drift.z);
				}
			}
			if (power >= 0.75f && age % 12 == 4 && age < STRONG_TRAIL_TICKS) {
				Vec3 start = pos.add(randomDisk(normal, 0.35 + RNG.nextDouble() * power));
				level.addParticle(GRAY_DUST, start.x, start.y, start.z,
						(RNG.nextDouble() - 0.5) * 0.025, 0.018 + RNG.nextDouble() * 0.025, (RNG.nextDouble() - 0.5) * 0.025);
			}
			if (power >= 1.0f && (age == 8 || age == 18)) {
				for (int i = 0; i < 5; i++) {
					Vec3 velocity = scatterVelocity(normal, power * fade, 0.08, 0.22);
					Vec3 start = pos.add(randomDisk(normal, 0.5 + power * 0.25));
					level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, pick(states)),
							start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
				}
			}
			return false;
		}
	}
}
