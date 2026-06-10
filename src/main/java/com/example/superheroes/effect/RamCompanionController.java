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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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
 */
public final class RamCompanionController {
	private static final int WEAKNESS_TICKS = 30 * 20;
	private static final Map<UUID, RamEntity> ACTIVE_RAM = new ConcurrentHashMap<>();
	private static final Set<UUID> FALLEN_THIS_SESSION = ConcurrentHashMap.newKeySet();

	private RamCompanionController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				UUID id = player.getUUID();
				boolean demonActive = RemDemonismController.isActive(player);
				if (!demonActive) {
					FALLEN_THIS_SESSION.remove(id);
					RamEntity ram = ACTIVE_RAM.remove(id);
					if (ram != null && ram.isAlive()) {
						ram.dismiss();
					}
					continue;
				}
				if (FALLEN_THIS_SESSION.contains(id)) {
					continue;
				}
				RamEntity ram = ACTIVE_RAM.get(id);
				if (ram == null || !ram.isAlive() || ram.isRemoved()) {
					spawnRam(player);
				}
			}
		});
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
		ACTIVE_RAM.put(player.getUUID(), ram);
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
		RamEntity ram = ACTIVE_RAM.remove(ownerId);
		if (ram != null && ram.isAlive()) {
			ram.dismiss();
		}
	}
}
