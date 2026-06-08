package com.example.superheroes.effect;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReinhardSpeedJudgmentController {
	private static final Map<UUID, PendingStrike> PENDING = new ConcurrentHashMap<>();

	private ReinhardSpeedJudgmentController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ReinhardSpeedJudgmentController::tick);
	}

	public static void start(ServerPlayer attacker, ServerPlayer target, long delayMs, float damage) {
		teleportBehind(attacker, target);
		PENDING.put(attacker.getUUID(), new PendingStrike(target.getUUID(), System.currentTimeMillis() + delayMs, damage));
		ReinhardTimeSlowController.triggerAbilitySlow(attacker);
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) return;
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<UUID, PendingStrike>> it = PENDING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, PendingStrike> entry = it.next();
			ServerPlayer attacker = server.getPlayerList().getPlayer(entry.getKey());
			ServerPlayer target = server.getPlayerList().getPlayer(entry.getValue().targetId());
			if (!isValid(attacker, target)) {
				it.remove();
				continue;
			}
			if (now < entry.getValue().strikeAtMs()) {
				teleportBehind(attacker, target);
				continue;
			}
			teleportBehind(attacker, target);
			strike(attacker, target, entry.getValue().damage());
			it.remove();
		}
	}

	private static boolean isValid(ServerPlayer attacker, ServerPlayer target) {
		if (attacker == null || target == null) return false;
		if (!attacker.isAlive() || !target.isAlive() || target.isSpectator()) return false;
		if (attacker.serverLevel() != target.serverLevel()) return false;
		return ReinhardController.isReinhard(attacker);
	}

	private static void teleportBehind(ServerPlayer attacker, ServerPlayer target) {
		Vec3 look = target.getViewVector(1f);
		Vec3 behind = target.position().subtract(look.scale(1.6));
		attacker.connection.teleport(behind.x, behind.y, behind.z, (target.getYRot() + 180f) % 360f, 0f);
		attacker.lookAt(EntityAnchorArgument.Anchor.EYES, target.position().add(0, target.getEyeHeight(), 0));
	}

	private static void strike(ServerPlayer attacker, ServerPlayer target, float damage) {
		ServerLevel level = attacker.serverLevel();
		level.sendParticles(ParticleTypes.FLASH,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				40, 0.4, 0.6, 0.4, 0.1);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.4f, 1.0f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 1.8f);
		target.invulnerableTime = 0;
		ReinhardSwordDeathMarkController.hurtBypassingMark(target, level.damageSources().playerAttack(attacker), damage);
	}

	private record PendingStrike(UUID targetId, long strikeAtMs, float damage) {
	}
}
