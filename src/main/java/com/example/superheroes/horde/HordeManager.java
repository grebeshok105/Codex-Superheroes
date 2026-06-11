package com.example.superheroes.horde;

import com.example.superheroes.horde.entity.BaseHordeEntity;
import com.example.superheroes.horde.entity.HordeEntities;
import com.example.superheroes.horde.entity.InfectedHomelanderBossEntity;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages active horde instances. One per activation.
 */
public final class HordeManager {
	private static final Map<UUID, HordeInstance> ACTIVE = new ConcurrentHashMap<>();
	private static final int INTER_WAVE_TICKS = 300; // 15 seconds between waves
	private static final double SPAWN_RING_MIN = 20.0;
	private static final double SPAWN_RING_MAX = 35.0;

	public static class HordeInstance {
		final UUID id;
		final ServerLevel level;
		final Vec3 center;
		final ServerBossEvent waveBossBar;
		int currentWave;
		int mobsAlive;
		int totalMobsThisWave;
		int interWaveTimer;
		boolean finished;

		HordeInstance(UUID id, ServerLevel level, Vec3 center) {
			this.id = id;
			this.level = level;
			this.center = center;
			this.currentWave = 0;
			this.interWaveTimer = 60; // 3s before first wave
			this.waveBossBar = new ServerBossEvent(
					Component.literal("§c§lОрда Паразитов"),
					BossEvent.BossBarColor.RED,
					BossEvent.BossBarOverlay.NOTCHED_10);
		}
	}

	private HordeManager() {
	}

	public static UUID startHorde(ServerLevel level, Vec3 center, ServerPlayer activator) {
		UUID id = UUID.randomUUID();
		HordeInstance instance = new HordeInstance(id, level, center);
		instance.waveBossBar.addPlayer(activator);
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
		if (inst.interWaveTimer > 0) {
			inst.interWaveTimer--;
			if (inst.interWaveTimer == 60) {
				int next = inst.currentWave + 1;
				if (next <= HordeWaves.WAVES.size()) {
					broadcastMessage(inst.level, inst.center,
							"§e⚔ Волна " + next + " через 3 секунды...");
				}
			}
			if (inst.interWaveTimer <= 0) {
				startNextWave(inst);
			}
			return;
		}

		updateBossBar(inst);

		if (inst.mobsAlive <= 0 && inst.currentWave > 0) {
			if (inst.currentWave >= HordeWaves.WAVES.size()) {
				inst.finished = true;
				broadcastMessage(inst.level, inst.center,
						"§6§l★ ВСЕ ВОЛНЫ ПРОЙДЕНЫ! Орда побеждена! ★");
				inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
						SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2.0f, 1.0f);
			} else {
				broadcastMessage(inst.level, inst.center,
						"§a✔ Волна " + inst.currentWave + " очищена!");
				inst.interWaveTimer = INTER_WAVE_TICKS;
			}
		}
	}

	private static void startNextWave(HordeInstance inst) {
		inst.currentWave++;
		if (inst.currentWave > HordeWaves.WAVES.size()) {
			inst.finished = true;
			return;
		}
		HordeWaveDefinition wave = HordeWaves.WAVES.get(inst.currentWave - 1);
		inst.totalMobsThisWave = wave.totalMobCount();
		inst.mobsAlive = inst.totalMobsThisWave;

		broadcastMessage(inst.level, inst.center,
				"§c§l⚔ ВОЛНА " + inst.currentWave + " / " + HordeWaves.WAVES.size() +
						" — " + inst.totalMobsThisWave + " враг" + russianPlural(inst.totalMobsThisWave));
		inst.level.playSound(null, inst.center.x, inst.center.y, inst.center.z,
				SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0f, 0.6f);

		for (HordeWaveDefinition.MobEntry entry : wave.mobs()) {
			for (int i = 0; i < entry.count(); i++) {
				spawnMob(inst, entry);
			}
		}
		if (wave.boss() != null) {
			for (int i = 0; i < wave.boss().count(); i++) {
				spawnMob(inst, wave.boss());
			}
		}
	}

	private static void spawnMob(HordeInstance inst, HordeWaveDefinition.MobEntry entry) {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		double angle = rng.nextDouble() * Math.PI * 2;
		double dist = SPAWN_RING_MIN + rng.nextDouble() * (SPAWN_RING_MAX - SPAWN_RING_MIN);
		double spawnX = inst.center.x + Math.cos(angle) * dist;
		double spawnZ = inst.center.z + Math.sin(angle) * dist;
		int spawnY = inst.level.getHeightmapPos(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				BlockPos.containing(spawnX, 0, spawnZ)).getY();

		Entity entity = entry.type().create(inst.level);
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
		inst.level.sendParticles(ParticleTypes.LARGE_SMOKE, spawnX, spawnY + 0.5, spawnZ,
				8, 0.5, 0.3, 0.5, 0.05);
	}

	public static void onMobKilled(UUID hordeId) {
		HordeInstance inst = ACTIVE.get(hordeId);
		if (inst != null) {
			inst.mobsAlive = Math.max(0, inst.mobsAlive - 1);
		}
	}

	private static void updateBossBar(HordeInstance inst) {
		inst.waveBossBar.setName(Component.literal(
				"§c§lОрда — Волна " + inst.currentWave + "/" + HordeWaves.WAVES.size() +
						" §7[" + inst.mobsAlive + " осталось]"));
		float progress = inst.totalMobsThisWave > 0
				? (float) inst.mobsAlive / inst.totalMobsThisWave
				: 0f;
		inst.waveBossBar.setProgress(Math.max(0f, Math.min(1f, progress)));
	}

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
