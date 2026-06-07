package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.effect.ReinhardWorthyOpponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Air Slash — режет воздух духовной волной. 9-блочный raycast, наносит урон
 * + сильный отбрасывание. Только при обнажённом мече.
 */
public final class ReinhardAirSlashAbility implements Ability {
	private static final double RANGE = 9.0;
	private static final float DAMAGE_MOB = 8.0f;
	private static final float DAMAGE_WORTHY = 14.0f;
	private static final int COOLDOWN_TICKS = 30;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_AIR_SLASH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 200f;
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
		Vec3 actualEnd = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(1.2);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, actualEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		if (hit != null) {
			LivingEntity target = (LivingEntity) hit.getEntity();
			boolean worthy = ReinhardWorthyOpponent.isWorthy(target);
			float dmg = worthy ? DAMAGE_WORTHY : DAMAGE_MOB;
			DamageSource src = level.damageSources().playerAttack(player);
			target.hurt(src, dmg);
			Vec3 push = dir.scale(2.0);
			target.push(push.x, 0.6, push.z);
			target.hurtMarked = true;
			actualEnd = hit.getLocation();
		}

		// VFX вдоль луча
		int particleSteps = 24;
		Vec3 step = actualEnd.subtract(eye).scale(1.0 / particleSteps);
		Vec3 cur = eye.add(dir.scale(0.5));
		for (int i = 0; i < particleSteps; i++) {
			level.sendParticles(ParticleTypes.END_ROD,
					cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0.0);
			cur = cur.add(step);
		}
		level.sendParticles(ParticleTypes.FLASH,
				actualEnd.x, actualEnd.y, actualEnd.z, 1, 0, 0, 0, 0);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.8f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.6f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
