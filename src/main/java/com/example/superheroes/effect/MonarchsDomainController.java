package com.example.superheroes.effect;

import com.example.superheroes.entity.ShadowSoldierEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Активный домен Монарха: 10с тиков урона врагам в радиусе 25 блоков
 * каждые 10 тиков (игнор брони, magic damage).
 *
 *  - После завершения: 100t Weakness II + Slowness II у Сона.
 */
public final class MonarchsDomainController {
	private static final double RADIUS = 25.0;
	private static final int TICK_INTERVAL = 10;
	private static final float TICK_DAMAGE = 4.0f;

	private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

	private MonarchsDomainController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Long end = ACTIVE_UNTIL.get(player.getUUID());
				if (end == null) continue;
				if (player.level().getGameTime() >= end) {
					expire(player);
					continue;
				}
				if (player.level().getGameTime() % TICK_INTERVAL == 0) {
					tick(player);
				}
			}
		});
	}

	public static void activate(ServerPlayer player, int durationTicks) {
		ACTIVE_UNTIL.put(player.getUUID(), player.level().getGameTime() + durationTicks);
	}

	public static void clear(UUID id) {
		ACTIVE_UNTIL.remove(id);
	}

	private static void tick(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(RADIUS);
		List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !(e instanceof ShadowSoldierEntity)
						&& !(e instanceof Player p && p.getUUID().equals(player.getUUID())));
		for (LivingEntity hit : hits) {
			if (hit.distanceToSqr(player) > RADIUS * RADIUS) continue;
			hit.hurt(level.damageSources().magic(), TICK_DAMAGE);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					hit.getX(), hit.getY() + hit.getBbHeight() * 0.5, hit.getZ(),
					6, 0.3, 0.4, 0.3, 0.05);
		}
		level.sendParticles(ParticleTypes.PORTAL,
				player.getX(), player.getY() + 1, player.getZ(), 12, 1.2, 0.5, 1.2, 0.2);
	}

	private static void expire(ServerPlayer player) {
		ACTIVE_UNTIL.remove(player.getUUID());
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, true, true, true));
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7f, 0.7f);
	}

	public static boolean isActive(ServerPlayer player) {
		Long end = ACTIVE_UNTIL.get(player.getUUID());
		return end != null && player.level().getGameTime() < end;
	}
}
