package com.example.superheroes.effect;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GreedCageController {
	private static final List<Cage> ACTIVE = new CopyOnWriteArrayList<>();
	private static final int RING_SLOTS = 8;
	private static final double RING_RADIUS = 2.6;
	private static final int TRACK_DURATION_TICKS = 600;
	private static final float FALL_DAMAGE_PER_BLOCK_PCT = 0.2f;

	private GreedCageController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(GreedCageController::tick);
	}

	public static void create(ServerLevel level, Vec3 center, List<LivingEntity> targets, int cageVisualTicks) {
		long now = level.getGameTime();
		Set<UUID> ids = new HashSet<>();
		Set<UUID> damaged = new HashSet<>();
		for (LivingEntity le : targets) ids.add(le.getUUID());
		ACTIVE.add(new Cage(level.dimension(), center, ids, damaged,
				now + cageVisualTicks, now + TRACK_DURATION_TICKS, 0));

		for (int b = 0; b < RING_SLOTS; b++) {
			double angle = (Math.PI * 2.0 / RING_SLOTS) * b;
			double x = center.x + Math.cos(angle) * RING_RADIUS;
			double z = center.z + Math.sin(angle) * RING_RADIUS;
			spawnFrozenLightning(level, x, center.y, z);
		}
		level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.5, center.z, 4, 1.5, 1.0, 1.5, 0.0);
	}

	private static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		for (int i = ACTIVE.size() - 1; i >= 0; i--) {
			Cage cage = ACTIVE.get(i);
			ServerLevel level = server.getLevel(cage.dim);
			if (level == null) {
				ACTIVE.remove(i);
				continue;
			}
			long now = level.getGameTime();
			boolean visible = now < cage.visualDeadline;
			boolean expired = now >= cage.trackDeadline;
			cage.tickCount++;

			for (UUID id : cage.targetIds) {
				if (cage.damaged.contains(id)) continue;
				Entity ent = level.getEntity(id);
				if (!(ent instanceof LivingEntity le) || !le.isAlive()) {
					cage.damaged.add(id);
					continue;
				}

				Vec3 c = le.position().add(0, le.getBbHeight() * 0.5, 0);

				if (visible) {
					int slot = cage.tickCount % RING_SLOTS;
					double angle = (Math.PI * 2.0 / RING_SLOTS) * slot;
					double bx = c.x + Math.cos(angle) * RING_RADIUS;
					double bz = c.z + Math.sin(angle) * RING_RADIUS;
					spawnFrozenLightning(level, bx, le.position().y, bz);
					int slot2 = (cage.tickCount + 4) % RING_SLOTS;
					double angle2 = (Math.PI * 2.0 / RING_SLOTS) * slot2;
					double bx2 = c.x + Math.cos(angle2) * RING_RADIUS;
					double bz2 = c.z + Math.sin(angle2) * RING_RADIUS;
					spawnFrozenLightning(level, bx2, le.position().y, bz2);

					for (int s = 0; s < RING_SLOTS; s++) {
						double a = (Math.PI * 2.0 / RING_SLOTS) * s;
						double sx = c.x + Math.cos(a) * RING_RADIUS;
						double sz = c.z + Math.sin(a) * RING_RADIUS;
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, sx, le.position().y + 1.0, sz,
								2, 0.05, 1.2, 0.05, 0.0);
						level.sendParticles(ParticleTypes.END_ROD, sx, le.position().y + 1.0, sz,
								1, 0.05, 1.2, 0.05, 0.0);
					}
				}

				if (le.onGround() && le.fallDistance > 3.0f) {
					float dmg = le.fallDistance * FALL_DAMAGE_PER_BLOCK_PCT * le.getMaxHealth();
					DamageSource src = level.damageSources().fellOutOfWorld();
					le.invulnerableTime = 0;
					le.hurt(src, dmg);
					le.fallDistance = 0;
					level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
							le.getX(), le.getY(), le.getZ(), 1, 0, 0, 0, 0);
					cage.damaged.add(id);
				}
			}

			if (expired) {
				ACTIVE.remove(i);
			}
		}
	}

	private static void spawnFrozenLightning(ServerLevel level, double x, double y, double z) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt == null) return;
		bolt.moveTo(x, y, z);
		bolt.setVisualOnly(true);
		bolt.setSilent(true);
		level.addFreshEntity(bolt);
	}

	private static final class Cage {
		final ResourceKey<Level> dim;
		final Vec3 center;
		final Set<UUID> targetIds;
		final Set<UUID> damaged;
		final long visualDeadline;
		final long trackDeadline;
		int tickCount;

		Cage(ResourceKey<Level> dim, Vec3 center, Set<UUID> targetIds, Set<UUID> damaged,
		     long visualDeadline, long trackDeadline, int tickCount) {
			this.dim = dim;
			this.center = center;
			this.targetIds = targetIds;
			this.damaged = damaged;
			this.visualDeadline = visualDeadline;
			this.trackDeadline = trackDeadline;
			this.tickCount = tickCount;
		}
	}
}
