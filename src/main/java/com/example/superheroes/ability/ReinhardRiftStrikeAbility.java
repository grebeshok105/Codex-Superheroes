package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.effect.ReinhardWorthyOpponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rift Strike — анти-спидстер. Находит самую быструю живую цель в радиусе 32 блоков,
 * мгновенно телепортируется ей за спину со скоростью света и автоматически наносит
 * полноценный удар мечом (как ЛКМ). Только при обнажённом мече и Royal Icicle в руке.
 *
 * Скорость цели измеряется по реальному смещению за тик (deltaMovement.length) —
 * это ловит и эффект SPEED, и кастомные ускорения вроде Iron Man Supersonic / Sung
 * Jinwoo's haste / Naruto Sage Mode и т.д.
 */
public final class ReinhardRiftStrikeAbility implements Ability {
	private static final double RADIUS = 32.0;
	private static final double MIN_SPEED_PER_TICK = 0.20;
	private static final float DAMAGE_MOB = 12.0f;
	private static final float DAMAGE_WORTHY = 22.0f;
	private static final int COOLDOWN_TICKS = 280;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_RIFT_STRIKE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 250f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (AbilityCooldowns.isOnCooldown(player, getId())) return false;
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (!state.swordDrawn()) return false;
		return player.getMainHandItem().getItem() instanceof com.example.superheroes.item.RoyalIcicleItem;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(RADIUS);

		LivingEntity target = null;
		double bestSpeed = MIN_SPEED_PER_TICK;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e != player && !e.isSpectator())) {
			Vec3 d = candidate.getDeltaMovement();
			double speed = Math.sqrt(d.x * d.x + d.z * d.z);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				target = candidate;
			}
		}

		if (target == null) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_rift_strike.no_target"),
					true);
			return false;
		}

		Vec3 from = player.position();
		level.sendParticles(ParticleTypes.PORTAL,
				from.x, from.y + 1.0, from.z,
				40, 0.4, 0.8, 0.4, 0.5);
		level.sendParticles(ParticleTypes.END_ROD,
				from.x, from.y + 1.0, from.z,
				24, 0.3, 0.6, 0.3, 0.05);

		Vec3 targetLook = target.getViewVector(1f);
		Vec3 behind = target.position().subtract(targetLook.scale(1.6));

		player.connection.teleport(behind.x, behind.y, behind.z,
				(target.getYRot() + 180f) % 360f, 0f);
		player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
				target.position().add(0, target.getEyeHeight(), 0));

		level.sendParticles(ParticleTypes.PORTAL,
				behind.x, behind.y + 1.0, behind.z,
				40, 0.4, 0.8, 0.4, 0.5);
		level.sendParticles(ParticleTypes.FLASH,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				40, 0.4, 0.6, 0.4, 0.1);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				1, 0, 0, 0, 0);

		boolean worthy = ReinhardWorthyOpponent.isWorthy(target);
		float dmg = worthy ? DAMAGE_WORTHY : DAMAGE_MOB;
		DamageSource src = level.damageSources().playerAttack(player);
		target.invulnerableTime = 0;
		target.hurt(src, dmg);

		level.playSound(null, behind.x, behind.y, behind.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.6f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.4f, 1.0f);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 1.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
