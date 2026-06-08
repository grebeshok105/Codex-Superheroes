package com.example.superheroes.ability;

import com.example.superheroes.effect.ReinhardSpeedJudgmentController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ReinhardSpeedJudgmentAbility implements Ability {
	private static final double RADIUS = 50.0;
	private static final double MIN_SPEED_PER_TICK = 0.03;
	private static final long STRIKE_DELAY_MS = 3_000L;
	private static final float DAMAGE = 50.0f;
	private static final int COOLDOWN_TICKS = 8 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_SPEED_JUDGMENT;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 300f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		ServerPlayer target = findFastestTarget(player, level);
		if (target == null) {
			player.displayClientMessage(Component.translatable("ability.superheroes.reinhard_speed_judgment.no_target"), true);
			return false;
		}

		Vec3 start = player.getEyePosition();
		Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
		Vec3 line = end.subtract(start);
		int steps = Math.max(8, (int) (line.length() * 2.0));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = start.add(line.scale(i / (double) steps));
			level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.02);
			}
		}
		level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 2, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 48, 0.5, 0.7, 0.5, 0.10);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.8f);
		ReinhardSpeedJudgmentController.start(player, target, STRIKE_DELAY_MS, DAMAGE);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static ServerPlayer findFastestTarget(ServerPlayer player, ServerLevel level) {
		AABB box = player.getBoundingBox().inflate(RADIUS);
		ServerPlayer best = null;
		double bestScore = MIN_SPEED_PER_TICK;
		for (ServerPlayer candidate : level.getEntitiesOfClass(ServerPlayer.class, box,
				e -> e.isAlive() && e != player && !e.isSpectator())) {
			double score = speedScore(candidate);
			if (score > bestScore) {
				bestScore = score;
				best = candidate;
			}
		}
		return best;
	}

	private static double speedScore(ServerPlayer entity) {
		Vec3 movement = entity.getDeltaMovement();
		return Math.sqrt(movement.x * movement.x + movement.z * movement.z);
	}
}
