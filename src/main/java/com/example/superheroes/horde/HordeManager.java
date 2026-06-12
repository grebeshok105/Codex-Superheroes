package com.example.superheroes.horde;

import com.example.superheroes.horde.entity.BaseHordeEntity;
import com.example.superheroes.horde.entity.InfectedHomelanderBossEntity;
import com.example.superheroes.network.HordeDebugS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages active horde instances. One per activation.
 *
 * <p>Live-mob tracking is by {@link UUID} set (not a bare counter) so a wave
 * always completes even if mobs vanish without a normal death (chunk unload,
 * {@code /kill}, void fall, despawn). A periodic reconciliation prunes dead
 * UUIDs and leashes strays back toward the centre.</p>
 */
public final class HordeManager {
	private static final Map<UUID, HordeInstance> ACTIVE = new ConcurrentHashMap<>();
	/** Players who toggled the debug overlay on (server-wide). */
	private static final Set<UUID> OVERLAY_PLAYERS = ConcurrentHashMap.newKeySet();

	private static final int INTER_WAVE_TICKS = 300; // 15 seconds between waves
	private static final double SPAWN_RING_MIN = 20.0;
	private static final double SPAWN_RING_MAX = 35.0;
	private static final int RECONCILE_INTERVAL = 15;
	private static final double LEASH_MAX_DIST = SPAWN_RING_MAX + 20.0; // strays beyond this snap back
	private static final double LEASH_MIN_Y_DELTA = 25.0; // fell this far below centre -> snap back
	private static final int ELITE_SPAWN_DELAY = 40; // boss appears ~2s after the warning
	private static final int OVERLAY_INTERVAL = 10;

	/** A spawn deferred by a few ticks (reinforcements / staggered elites). */
	private static final class DelayedSpawn {
		final EntityType<?> type;
		final double angleDeg;
		int delay;
		final boolean elite;

		DelayedSpawn(EntityType<?> type, double angleDeg, int delay, boolean elite) {
			this.type = type;
			this.angleDeg = angleDeg;
			this.delay = delay;
			this.elite = elite;
		}
	}

	public static final class HordeInstance {
		final UUID id;
		final ServerLevel level;
		final Vec3 center;
		final ServerBossEvent waveBossBar;
		final Set<UUID> liveMobIds = ConcurrentHashMap.newKeySet();
		final List<DelayedSpawn> delayedSpawns = new ArrayList<>();
		int currentWave;
		int totalMobsThisWave;
		int interWaveTimer;
		int reconcileTimer;
		int overlayTimer;
		float frontYaw;
		boolean waveActive;
		boolean finished;

		HordeInstance(UUID id, ServerLevel level, Vec3 center, float frontYaw) {
			this.id = id;
			this.level = level;
			this.center = center;
			this.frontYaw = frontYaw;
			this.currentWave = 0;
			this.interWaveTimer = 60; // 3s before first wave
			this.waveBossBar = new ServerBossEvent(
					Component.literal("§c§lОрда Паразитов"),
					BossEvent.BossBarColor.RED,
					BossEvent.BossBarOverlay.NOTCHED_10);
		}

		int remaining() {
			return liveMobIds.size() + delayedSpawns.size();
		}
	}

	private HordeManager() {
	}

	// ─────────────────────────────────────────────── lifecycle ────────────

	public static UUID startHorde(ServerLevel level, Vec3 center, ServerPlayer activator) {
		UUID id = UUID.randomUUID();
		float frontYaw = activator != null ? activator.getYRot() : 0f;
		HordeInstance instance = new HordeInstance(id, level, center, frontYaw);
		if (activator != null) {
			instance.waveBossBar.addPlayer(activator);
		}
		for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
			if (p.level() == level && p.distanceToSqr(center) < 100 * 100) {
				instance.waveBossBar.addPlayer(p);
			}
		}
		ACTIVE.put(id, instance);
		broadcastMessage(level, center, "§c§l⚠ ОРДА ПАРАЗИТОВ АКТИВИРОВАНА! Приготовьтесь!");
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.5f);
		return id;
	}

	public static void tick(ServerLevel level) {
		ACTIVE.values().removeIf(inst -> {
			if (inst.level != level) return false;
			if (inst.finished) {
				inst.waveBossBar.removeAllPlayers();
				return true;
			}
			tickInstance(inst);
			return false;
		});
	}

	private static void tickInstance(HordeInstance inst) {
		// 1) Reconcile the live set (source of truth) + leash strays.
		if (++inst.reconcileTimer >= RECONCILE_INTERVAL) {
			inst.reconcileTimer = 0;
			reconcile(inst);
		}

		// 2) Process deferred spawns (reinforcements / staggered elites).
		if (!inst.delayedSpawns.isEmpty()) {
			var it = inst.delayedSpawns.iterator();
			while (it.hasNext()) {
				DelayedSpawn ds = it.next();
				if (--ds.delay <= 0) {
					spawnMobAt(inst, ds.type, ds.angleDeg, ds.elite);
					it.remove();
				}
			}
		}

		// 3) Inter-wave countdown.
		if (inst.interWaveTimer > 0) {
			inst.interWaveTimer--;
			if (inst.interWaveTimer == 60) {
				int next = inst.currentWave + 1;
				if (next <= HordeWaves.WAVES.size()) {
					broadcastMessage(inst.level, inst.center, "§e⚔ Волна " + next + " через 3 секунды...");
					inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
							SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 1.5f, 0.7f);
				}
			}
			if (inst.interWaveTimer <= 0) {
				startNextWave(inst);
			}
			pushOverlay(inst);
			return;
		}

		updateBossBar(inst);
		pushOverlay(inst);

		// 4) Wave complete? Only once spawns are done and nothing is alive.
		if (inst.waveActive && inst.currentWave > 0
				&& inst.liveMobIds.isEmpty() && inst.delayedSpawns.isEmpty()) {
			inst.waveActive = false;
			if (inst.currentWave >= HordeWaves.WAVES.size()) {
				inst.finished = true;
				broadcastMessage(inst.level, inst.center, "§6§l★ ВСЕ ВОЛНЫ ПРОЙДЕНЫ! Орда побеждена! ★");
				inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
						SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2.0f, 1.0f);
			} else {
				broadcastMessage(inst.level, inst.center, "§a✔ Волна " + inst.currentWave + " очищена!");
				inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
						SoundEvents.UI_TOAST_OUT, SoundSource.HOSTILE, 1.4f, 1.3f);
				inst.interWaveTimer = INTER_WAVE_TICKS;
			}
		}
	}

	/** Prune dead/removed UUIDs and snap strays back toward the centre. */
	private static void reconcile(HordeInstance inst) {
		inst.liveMobIds.removeIf(uuid -> {
			Entity e = inst.level.getEntity(uuid);
			if (e == null || e.isRemoved() || !e.isAlive()) {
				return true;
			}
			// Leash: too far out or fell into the void -> teleport near the centre.
			double dx = e.getX() - inst.center.x;
			double dz = e.getZ() - inst.center.z;
			boolean tooFar = (dx * dx + dz * dz) > LEASH_MAX_DIST * LEASH_MAX_DIST;
			boolean fell = e.getY() < inst.center.y - LEASH_MIN_Y_DELTA;
			if ((tooFar || fell) && e instanceof BaseHordeEntity) {
				snapToRing(inst, e);
			}
			return false;
		});
	}

	private static void snapToRing(HordeInstance inst, Entity e) {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		double angle = rng.nextDouble() * Math.PI * 2;
		double dist = SPAWN_RING_MIN + rng.nextDouble() * (SPAWN_RING_MAX - SPAWN_RING_MIN);
		double x = inst.center.x + Math.cos(angle) * dist;
		double z = inst.center.z + Math.sin(angle) * dist;
		int y = inst.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(int) Math.floor(x), (int) Math.floor(z));
		e.teleportTo(x, y, z);
		e.setDeltaMovement(Vec3.ZERO);
		inst.level.sendParticles(ParticleTypes.PORTAL, x, y + 0.8, z, 12, 0.3, 0.5, 0.3, 0.05);
	}

	private static void startNextWave(HordeInstance inst) {
		inst.currentWave++;
		if (inst.currentWave > HordeWaves.WAVES.size()) {
			inst.finished = true;
			return;
		}
		HordeWaveDefinition wave = HordeWaves.WAVES.get(inst.currentWave - 1);

		// Build the flat list of regular spawn units (scaled), then bucket by side.
		List<EntityType<?>> units = new ArrayList<>();
		for (HordeWaveDefinition.MobEntry entry : wave.mobs()) {
			int n = scaledCount(entry.count());
			for (int i = 0; i < n; i++) {
				units.add(entry.type());
			}
		}

		int total = units.size();
		if (wave.boss() != null) {
			total += wave.boss().count();
		}
		inst.totalMobsThisWave = total;
		inst.waveActive = true;

		broadcastMessage(inst.level, inst.center,
				"§c§l⚔ ВОЛНА " + inst.currentWave + " / " + HordeWaves.WAVES.size() +
						" — " + total + " враг" + russianPlural(total));
		inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
				SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0f, 0.6f);

		ThreadLocalRandom rng = ThreadLocalRandom.current();
		for (EntityType<?> type : units) {
			double roll = rng.nextDouble();
			if (roll < 0.40) { // 40% front
				spawnMobAt(inst, type, frontSector(inst, rng), false);
			} else if (roll < 0.65) { // 25% flanks
				double side = rng.nextBoolean() ? 1 : -1;
				double a = inst.frontYaw + side * (55 + rng.nextDouble() * 70); // 55..125°
				spawnMobAt(inst, type, a, false);
			} else if (roll < 0.85) { // 20% rear
				double a = inst.frontYaw + 180 + (rng.nextDouble() * 70 - 35);
				spawnMobAt(inst, type, a, false);
			} else { // 15% delayed reinforcements from a random side
				double a = inst.frontYaw + (rng.nextDouble() * 360 - 180);
				int delay = 20 + rng.nextInt(50); // 1..3.5s later
				inst.delayedSpawns.add(new DelayedSpawn(type, a, delay, false));
			}
		}

		// Minibosses: announce "elite incoming", then drop them in slightly later.
		if (wave.boss() != null) {
			broadcastMessage(inst.level, inst.center, "§4§l☠ ЭЛИТА НА ПОДХОДЕ!");
			inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
					SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0f, 0.6f);
			for (int i = 0; i < wave.boss().count(); i++) {
				inst.delayedSpawns.add(new DelayedSpawn(wave.boss().type(),
						frontSector(inst, rng), ELITE_SPAWN_DELAY + i * 10, true));
			}
		}
	}

	private static double frontSector(HordeInstance inst, ThreadLocalRandom rng) {
		return inst.frontYaw + (rng.nextDouble() * 70 - 35); // ±35° around front
	}

	private static int scaledCount(int base) {
		return Math.max(base + 1, (int) Math.ceil(base * 1.6));
	}

	/** Spawn one mob at a ring position determined by a Minecraft yaw angle. */
	private static void spawnMobAt(HordeInstance inst, EntityType<?> type, double angleDeg, boolean elite) {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		double rad = Math.toRadians(angleDeg);
		// Minecraft yaw → direction vector.
		double dirX = -Math.sin(rad);
		double dirZ = Math.cos(rad);
		double dist = SPAWN_RING_MIN + rng.nextDouble() * (SPAWN_RING_MAX - SPAWN_RING_MIN);
		double spawnX = inst.center.x + dirX * dist;
		double spawnZ = inst.center.z + dirZ * dist;
		int spawnY = inst.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(int) Math.floor(spawnX), (int) Math.floor(spawnZ));

		Entity entity = type.create(inst.level);
		if (entity == null) return;
		entity.moveTo(spawnX, spawnY, spawnZ, rng.nextFloat() * 360f - 180f, 0);

		if (entity instanceof BaseHordeEntity horde) {
			horde.setHordeId(inst.id);
			horde.finalizeSpawn(inst.level, inst.level.getCurrentDifficultyAt(entity.blockPosition()),
					MobSpawnType.EVENT, null);
		} else if (entity instanceof InfectedHomelanderBossEntity boss) {
			boss.setHordeId(inst.id);
			boss.finalizeSpawn(inst.level, inst.level.getCurrentDifficultyAt(entity.blockPosition()),
					MobSpawnType.EVENT, null);
		}

		inst.level.addFreshEntity(entity);
		inst.liveMobIds.add(entity.getUUID());

		// Straight into the fight: aggro the nearest valid player at spawn.
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			net.minecraft.world.entity.player.Player nearest = inst.level.getNearestPlayer(entity, 80.0);
			if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
				mob.setTarget(nearest);
			}
		}

		if (elite) {
			inst.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnX, spawnY + 1.0, spawnZ,
					30, 0.6, 0.8, 0.6, 0.05);
			inst.level.playSound(null, spawnX, spawnY, spawnZ,
					SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.6f, 0.7f);
		} else {
			inst.level.sendParticles(ParticleTypes.LARGE_SMOKE, spawnX, spawnY + 0.5, spawnZ,
					8, 0.5, 0.3, 0.5, 0.05);
		}
	}

	/** Called from BaseHordeEntity.die — remove immediately by entity UUID. */
	public static void onMobDied(UUID hordeId, UUID entityId) {
		HordeInstance inst = ACTIVE.get(hordeId);
		if (inst != null) {
			inst.liveMobIds.remove(entityId);
		}
	}

	private static void updateBossBar(HordeInstance inst) {
		int remaining = inst.remaining();
		inst.waveBossBar.setName(Component.literal(
				"§c§lОрда — Волна " + inst.currentWave + "/" + HordeWaves.WAVES.size() +
						" §7[" + remaining + " осталось]"));
		float progress = inst.totalMobsThisWave > 0
				? (float) remaining / inst.totalMobsThisWave
				: 0f;
		inst.waveBossBar.setProgress(Math.max(0f, Math.min(1f, progress)));
	}

	// ─────────────────────────────────────────── debug / commands ─────────

	private static HordeInstance activeIn(ServerLevel level) {
		for (HordeInstance inst : ACTIVE.values()) {
			if (inst.level == level && !inst.finished) {
				return inst;
			}
		}
		return null;
	}

	/** Finish the active horde in this level (also clears its mobs). Returns true if one existed. */
	public static boolean stopHorde(ServerLevel level) {
		HordeInstance inst = activeIn(level);
		if (inst == null) return false;
		clearLiveMobs(inst);
		inst.delayedSpawns.clear();
		inst.finished = true;
		broadcastMessage(level, inst.center, "§7Орда остановлена администратором.");
		return true;
	}

	/** Kill all currently-live mobs of the active horde. Returns how many were removed. */
	public static int clearMobs(ServerLevel level) {
		HordeInstance inst = activeIn(level);
		if (inst == null) return 0;
		int n = clearLiveMobs(inst);
		inst.delayedSpawns.clear();
		return n;
	}

	private static int clearLiveMobs(HordeInstance inst) {
		int n = 0;
		for (UUID uuid : List.copyOf(inst.liveMobIds)) {
			Entity e = inst.level.getEntity(uuid);
			if (e != null) {
				e.discard();
				n++;
			}
		}
		inst.liveMobIds.clear();
		return n;
	}

	/** Force-skip to the next wave: clears the current one and starts the next immediately. */
	public static boolean forceNextWave(ServerLevel level) {
		HordeInstance inst = activeIn(level);
		if (inst == null) return false;
		clearLiveMobs(inst);
		inst.delayedSpawns.clear();
		inst.waveActive = false;
		inst.interWaveTimer = 0;
		startNextWave(inst);
		return true;
	}

	/** Spawn standalone horde mobs near a position (not bound to a wave). Returns spawned count. */
	public static int spawnSingle(ServerLevel level, EntityType<?> type, Vec3 pos, int count) {
		int n = 0;
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			double ox = pos.x + (rng.nextDouble() * 6 - 3);
			double oz = pos.z + (rng.nextDouble() * 6 - 3);
			int oy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(int) Math.floor(ox), (int) Math.floor(oz));
			Entity entity = type.create(level);
			if (entity == null) continue;
			entity.moveTo(ox, oy, oz, rng.nextFloat() * 360f - 180f, 0);
			if (entity instanceof net.minecraft.world.entity.Mob mob) {
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()),
						MobSpawnType.COMMAND, null);
			}
			level.addFreshEntity(entity);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, ox, oy + 0.5, oz, 6, 0.4, 0.3, 0.4, 0.04);
			n++;
		}
		return n;
	}

	public static String getDebugStatus(ServerLevel level) {
		HordeInstance inst = activeIn(level);
		if (inst == null) {
			return "§7Активной орды в этом мире нет.";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("§6Орда §7| §fволна §e").append(inst.currentWave).append("§7/§e").append(HordeWaves.WAVES.size());
		sb.append(" §7| §fживых §a").append(inst.liveMobIds.size());
		sb.append("§7/§f").append(inst.totalMobsThisWave);
		sb.append(" §7| §fрезерв §b").append(inst.delayedSpawns.size());
		if (inst.interWaveTimer > 0) {
			sb.append(" §7| §fслед. волна через §e").append(inst.interWaveTimer / 20).append("с");
		}
		Map<String, Integer> byType = liveBreakdown(inst);
		if (!byType.isEmpty()) {
			sb.append("\n§7Типы: ");
			byType.forEach((k, v) -> sb.append("§f").append(k).append("§7×§e").append(v).append("  "));
		}
		return sb.toString();
	}

	private static Map<String, Integer> liveBreakdown(HordeInstance inst) {
		Map<String, Integer> byType = new LinkedHashMap<>();
		for (UUID uuid : inst.liveMobIds) {
			Entity e = inst.level.getEntity(uuid);
			if (e == null) continue;
			String name = e.getType().getDescriptionId();
			int idx = name.lastIndexOf('.');
			if (idx >= 0) name = name.substring(idx + 1);
			byType.merge(name.replace("horde_", ""), 1, Integer::sum);
		}
		return byType;
	}

	// ───────────────────────────────────────────── overlay ────────────────

	public static boolean toggleOverlay(ServerPlayer player) {
		boolean nowOn;
		if (OVERLAY_PLAYERS.contains(player.getUUID())) {
			OVERLAY_PLAYERS.remove(player.getUUID());
			nowOn = false;
		} else {
			OVERLAY_PLAYERS.add(player.getUUID());
			nowOn = true;
		}
		if (!nowOn) {
			ServerPlayNetworking.send(player, new HordeDebugS2CPayload(""));
		}
		return nowOn;
	}

	public static void setOverlay(ServerPlayer player, boolean enabled) {
		if (enabled) {
			OVERLAY_PLAYERS.add(player.getUUID());
		} else {
			OVERLAY_PLAYERS.remove(player.getUUID());
			ServerPlayNetworking.send(player, new HordeDebugS2CPayload(""));
		}
	}

	private static void pushOverlay(HordeInstance inst) {
		if (OVERLAY_PLAYERS.isEmpty()) return;
		if (++inst.overlayTimer < OVERLAY_INTERVAL) return;
		inst.overlayTimer = 0;
		String text = buildOverlayText(inst);
		for (ServerPlayer p : inst.level.getServer().getPlayerList().getPlayers()) {
			if (OVERLAY_PLAYERS.contains(p.getUUID()) && p.level() == inst.level) {
				ServerPlayNetworking.send(p, new HordeDebugS2CPayload(text));
			}
		}
	}

	private static String buildOverlayText(HordeInstance inst) {
		StringBuilder sb = new StringBuilder();
		sb.append("§c§lОРДА — ОТЛАДКА\n");
		sb.append("§7Волна: §f").append(inst.currentWave).append("/").append(HordeWaves.WAVES.size()).append("\n");
		sb.append("§7Живых: §a").append(inst.liveMobIds.size()).append("§7/§f").append(inst.totalMobsThisWave).append("\n");
		sb.append("§7Резерв: §b").append(inst.delayedSpawns.size()).append("\n");
		if (inst.interWaveTimer > 0) {
			sb.append("§7След. волна: §e").append(inst.interWaveTimer / 20).append("с\n");
		} else {
			sb.append("§7Статус: §6в бою\n");
		}
		Map<String, Integer> byType = liveBreakdown(inst);
		byType.forEach((k, v) -> sb.append("§8• §f").append(k).append(" §7×").append(v).append("\n"));
		return sb.toString();
	}

	// ───────────────────────────────────────────── helpers ────────────────

	private static void broadcastMessage(ServerLevel level, Vec3 center, String msg) {
		for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
			if (p.level() == level && p.distanceToSqr(center) < 150 * 150) {
				p.sendSystemMessage(Component.literal(msg));
			}
		}
	}

	private static String russianPlural(int n) {
		if (n % 10 == 1 && n % 100 != 11) return "";
		if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "а";
		return "ов";
	}

	public static boolean hasActiveHorde(Level level) {
		return ACTIVE.values().stream().anyMatch(i -> i.level == level && !i.finished);
	}
}
