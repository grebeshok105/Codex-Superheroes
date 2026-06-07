package com.example.superheroes.ability;

import com.example.superheroes.effect.SungJinwooController;
import com.example.superheroes.entity.ShadowSoldierEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Arise — поднимает Теневых Солдат из ВСЕХ мёртвых тел в радиусе 50 блоков
 * вокруг Сон Джи Ву.
 *
 *  - Cтоимость: 20 ENERGY (Shadow Charges) — flat per activation, без лимита на кол-во теней.
 *  - Cooldown: 40 тиков (2с).
 *  - Источники для подъёма:
 *      1) DeathEchoes (буфер всех умерших non-player в этом мире со времени активации героя),
 *      2) Резервный режим: добивает ослабленных живых врагов в радиусе и поднимает их.
 *  - Спавнит ОДНОВРЕМЕННО всех теней (без cap), регистрирует их в армии Сона.
 */
public final class AriseAbility implements Ability {
	private static final int COOLDOWN_TICKS = 40;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.ARISE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 20f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		double range = SungJinwooController.ARISE_RANGE;
		if (SungJinwooController.countDeathEchoesInRange(player, range) > 0) return true;
		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(range);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !(e instanceof Player) && !(e instanceof ShadowSoldierEntity));
		return candidates.stream().anyMatch(e -> e.getHealth() / e.getMaxHealth() < 0.25f);
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		double range = SungJinwooController.ARISE_RANGE;
		ServerLevel level = player.serverLevel();
		List<Vec3> spawnPositions = new ArrayList<>();

		// Шаг 1: вытащить ВСЕ накопленные эхо смертей в радиусе 50.
		spawnPositions.addAll(SungJinwooController.drainDeathEchoesInRange(player, range));

		// Шаг 2: добить ослабленных (<25% HP) живых non-player в радиусе и тоже поднять.
		AABB box = player.getBoundingBox().inflate(range);
		List<LivingEntity> weakened = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !(e instanceof Player) && !(e instanceof ShadowSoldierEntity)
						&& e.getHealth() / e.getMaxHealth() < 0.25f);
		for (LivingEntity victim : weakened) {
			Vec3 pos = victim.position();
			SungJinwooController.suppressDeathEcho(victim);
			victim.kill();
			spawnPositions.add(pos);
		}

		if (spawnPositions.isEmpty()) return false;

		for (Vec3 pos : spawnPositions) {
			Vec3 spawn = pos.add(0, 0.5, 0);
			ShadowSoldierEntity shadow = SungJinwooController.spawnOneShadowAt(level, player, spawn);
			if (shadow != null) {
				SungJinwooController.registerExtraShadow(player, shadow);
			}
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawn.x, spawn.y + 1, spawn.z, 30, 0.5, 1.0, 0.5, 0.05);
			level.sendParticles(ParticleTypes.PORTAL, spawn.x, spawn.y + 1, spawn.z, 50, 0.6, 1.0, 0.6, 0.6);
		}

		Vec3 center = player.position();
		level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 0.7f, 0.6f);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.ARISE, COOLDOWN_TICKS);
		return true;
	}
}
