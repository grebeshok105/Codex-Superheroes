package com.example.superheroes.effect;

import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.entity.RamEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Жизненный цикл Рам — помощницы Рем в демонизме:
 *  - появляется при активации демонизма;
 *  - исчезает при его окончании (сам RamEntity следит за этим тоже);
 *  - при гибели Рам в течение сессии демонизма повторно не призывается,
 *    а Рем получает дебафф скорби (Weakness II + Slowness I на 30с).
 *
 * Контроллер хранит только UUID призванной Рам, а не живую ссылку:
 * выгрузка чанка удаляет объект сущности, и по ссылке её не отличить от
 * убитой — из-за этого раньше призывалась вторая Рам (audit, потенциальные).
 */
public final class RamCompanionController {
	private static final int WEAKNESS_TICKS = 30 * 20;
	private static final double SEARCH_RADIUS = 64.0;
	/** ownerId -> ram entity UUID (not a live reference). */
	private static final Map<UUID, UUID> ACTIVE_RAM = new ConcurrentHashMap<>();
	private static final Set<UUID> FALLEN_THIS_SESSION = ConcurrentHashMap.newKeySet();

	private RamCompanionController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});
	}

	private static void tickPlayer(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean demonActive = RemDemonismController.isActive(player);
		if (!demonActive) {
			FALLEN_THIS_SESSION.remove(id);
			RamEntity ram = removeTracked(id, player.serverLevel());
			if (ram != null) {
				ram.dismiss();
			}
			return;
		}
		if (FALLEN_THIS_SESSION.contains(id)) {
			return;
		}
		if (findRam(player) == null) {
			spawnRam(player);
		}
	}

	/**
	 * The player's live ram, if any: the tracked UUID resolved in the level, or
	 * an unregistered live ram with the same owner adopted into tracking (and any
	 * extra duplicates discarded — e.g. a legacy entity reloaded from NBT).
	 */
	private static RamEntity findRam(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		UUID tracked = ACTIVE_RAM.get(player.getUUID());
		RamEntity keep = null;
		if (tracked != null) {
			Entity e = level.getEntity(tracked);
			if (e instanceof RamEntity ram && ram.isAlive()) {
				keep = ram;
			} else {
				ACTIVE_RAM.remove(player.getUUID(), tracked);
			}
		}
		List<RamEntity> owned = level.getEntitiesOfClass(RamEntity.class,
				player.getBoundingBox().inflate(SEARCH_RADIUS),
				ram -> ram.isAlive() && player.getUUID().equals(ram.getOwnerId()));
		if (keep == null && !owned.isEmpty()) {
			keep = owned.get(0);
		}
		for (RamEntity ram : owned) {
			if (ram != keep) {
				ram.discard();
			}
		}
		if (keep != null) {
			ACTIVE_RAM.put(player.getUUID(), keep.getUUID());
		}
		return keep;
	}

	private static RamEntity removeTracked(UUID ownerId, ServerLevel level) {
		UUID tracked = ACTIVE_RAM.remove(ownerId);
		if (tracked == null) {
			return null;
		}
		Entity e = level.getEntity(tracked);
		return e instanceof RamEntity ram && ram.isAlive() ? ram : null;
	}

	private static void spawnRam(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		RamEntity ram = ModEntities.RAM.create(level);
		if (ram == null) {
			return;
		}
		Vec3 side = Vec3.directionFromRotation(0, player.getYRot() + 90f).scale(1.6);
		ram.moveTo(player.getX() + side.x, player.getY(), player.getZ() + side.z, player.getYRot(), 0f);
		ram.setOwnerId(player.getUUID());
		ram.finalizeSpawn(level, level.getCurrentDifficultyAt(ram.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
		level.addFreshEntity(ram);
		ACTIVE_RAM.put(player.getUUID(), ram.getUUID());
		level.sendParticles(ParticleTypes.CHERRY_LEAVES,
				ram.getX(), ram.getY() + 1.0, ram.getZ(), 24, 0.4, 0.7, 0.4, 0.05);
		level.sendParticles(ParticleTypes.END_ROD,
				ram.getX(), ram.getY() + 1.0, ram.getZ(), 12, 0.3, 0.6, 0.3, 0.03);
		level.playSound(null, ram.getX(), ram.getY(), ram.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8f, 1.4f);
	}

	/** Вызывается из RamEntity.die(): скорбь Рем + блокировка повторного призыва на сессию. */
	public static void onRamDeath(RamEntity ram) {
		UUID ownerId = ram.getOwnerId();
		if (ownerId == null) {
			return;
		}
		FALLEN_THIS_SESSION.add(ownerId);
		ACTIVE_RAM.remove(ownerId);
		Player owner = ram.level().getPlayerByUUID(ownerId);
		if (owner instanceof ServerPlayer rem && RemDemonismController.isActive(rem)) {
			rem.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 1, true, true, true));
			rem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WEAKNESS_TICKS, 0, true, true, true));
			if (rem.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.SCULK_SOUL,
						rem.getX(), rem.getY() + 1.2, rem.getZ(), 16, 0.4, 0.6, 0.4, 0.02);
				level.playSound(null, rem.getX(), rem.getY(), rem.getZ(),
						SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 0.7f);
			}
		}
	}

	public static void clear(UUID ownerId) {
		FALLEN_THIS_SESSION.remove(ownerId);
		ACTIVE_RAM.remove(ownerId);
	}

	/** Test hook: spawn-adoption path without the tick loop. */
	public static RamEntity reconcileForTest(ServerPlayer player) {
		return findRam(player);
	}
}
