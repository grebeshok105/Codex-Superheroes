package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Mark of Judgment — повесить метку на цель. Метку видит только Рейнхард.
 * Цель получает +30% урона от Рейнхарда, +25% к скорости от Рейнхарда (преследование),
 * виден ему сквозь стены через Glowing-эффект (только Рейнхарду).
 * Длится 30 секунд. Только одна метка одновременно.
 */
public final class ReinhardJudgmentMarkAbility implements Ability {
	private static final double RANGE = 40.0;
	private static final int DURATION_TICKS = 600; // 30s
	private static final int COOLDOWN_TICKS = 200; // 10s

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_JUDGMENT_MARK;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 220f;
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
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult bh = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 cap = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(0.8);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, cap, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		if (hit == null) return false;
		LivingEntity target = (LivingEntity) hit.getEntity();

		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		long expire = level.getGameTime() + DURATION_TICKS;
		player.setAttached(ModAttachments.REINHARD_STATE,
				state.withJudgmentTarget(Optional.of(target.getUUID()), expire));

		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				target.getX(), target.getY() + target.getBbHeight() + 0.6, target.getZ(),
				30, 0.3, 0.2, 0.3, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6f, 1.6f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
