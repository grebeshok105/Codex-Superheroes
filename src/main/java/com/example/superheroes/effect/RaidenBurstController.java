package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.transform.HeroData;
import com.example.superheroes.particle.ModParticles;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Контроллер Burst-режима Райден (Q). Управляет 7-секундным окном:
 *  - снимает RAIDEN_BURST модификаторы атрибутов когда burstExpireTick прошёл
 *  - на тике burstFinalSlashTick делает финальный AoE-слэш по всем врагам в радиусе 8
 */
public final class RaidenBurstController {
	private static final double FINAL_SLASH_RADIUS = 8.0;
	private static final float FINAL_SLASH_DAMAGE_PLAYER = 22.0f;
	private static final float FINAL_SLASH_DAMAGE_MOB = 12.0f;

	private RaidenBurstController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!isRaiden(player)) continue;
				tick(player);
			}
		});
	}

	private static void tick(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		long now = player.serverLevel().getGameTime();

		long finalAt = state.burstFinalSlashTick();
		if (finalAt != 0L && now >= finalAt) {
			doFinalSlash(player);
			player.setAttached(ModAttachments.RAIDEN_STATE, state.withBurstFinalSlashTick(0L));
			state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		}

		long burstAt = state.burstExpireTick();
		if (burstAt != 0L && now >= burstAt) {
			HeroAttributes.RAIDEN_BURST.remove(player);
			player.setAttached(ModAttachments.RAIDEN_STATE, state.withBurstExpireTick(0L));
		} else if (burstAt > now) {
			if (now % 4 == 0) {
				ServerLevel level = player.serverLevel();
				level.sendParticles(ModParticles.JIWALD_EFFECT,
						player.getX(), player.getY() + 1.0, player.getZ(),
						3, 0.4, 0.6, 0.4, 0.05);
				level.sendParticles(ModParticles.BLUE_FLAME,
						player.getX(), player.getY() + 1.0, player.getZ(),
						1, 0.3, 0.5, 0.3, 0.02);
			}
		}
	}

	private static void doFinalSlash(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position().add(0, player.getBbHeight() * 0.5, 0);
		double r2 = FINAL_SLASH_RADIUS * FINAL_SLASH_RADIUS;
		AABB box = new AABB(
				origin.x - FINAL_SLASH_RADIUS, origin.y - FINAL_SLASH_RADIUS, origin.z - FINAL_SLASH_RADIUS,
				origin.x + FINAL_SLASH_RADIUS, origin.y + FINAL_SLASH_RADIUS, origin.z + FINAL_SLASH_RADIUS);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.getUUID().equals(player.getUUID()))
						&& e.position().add(0, e.getBbHeight() * 0.5, 0).distanceToSqr(origin) <= r2);
		for (LivingEntity le : targets) {
			float dmg = (le instanceof Player) ? FINAL_SLASH_DAMAGE_PLAYER : FINAL_SLASH_DAMAGE_MOB;
			le.invulnerableTime = 0;
			le.hurt(level.damageSources().playerAttack(player), dmg);
			Vec3 push = le.position().subtract(origin);
			double horiz = Math.max(0.01, Math.sqrt(push.x * push.x + push.z * push.z));
			le.setDeltaMovement(push.x / horiz * 0.6, 0.35, push.z / horiz * 0.6);
			le.hurtMarked = true;
		}

		level.sendParticles(ModParticles.ANOMALY_SLICE, origin.x, origin.y, origin.z, 4, 0.4, 0.3, 0.4, 0.02);
		level.sendParticles(ModParticles.WHITE_BOOM, origin.x, origin.y + 0.5, origin.z, 1, 0, 0, 0, 0);
		level.sendParticles(ModParticles.JIWALD_EFFECT, origin.x, origin.y + 1.0, origin.z,
				200, 2.0, 1.5, 2.0, 0.6);
		level.sendParticles(ModParticles.SWORD_EXPLOSION, origin.x, origin.y + 1.0, origin.z,
				40, 1.5, 0.6, 1.5, 0.15);
		level.sendParticles(ModParticles.BLUE_FLAME, origin.x, origin.y + 1.0, origin.z,
				80, 1.5, 1.5, 1.5, 0.1);
		level.sendParticles(ModParticles.MOONVEIL, origin.x, origin.y + 1.0, origin.z,
				14, 0.8, 0.4, 0.8, 0.05);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.4f, 0.7f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
	}

	private static boolean isRaiden(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && RaidenHero.ID.equals(data.heroId());
	}
}
