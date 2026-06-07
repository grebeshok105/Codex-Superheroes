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

/**
 * Heaven's Step — мгновенный телепорт за спину цели на которую смотришь
 * (макс 30 блоков). Только при обнажённом мече.
 */
public final class ReinhardTeleportBehindAbility implements Ability {
	private static final double RANGE = 30.0;
	private static final int COOLDOWN_TICKS = 80;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_TELEPORT_BEHIND;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 280f;
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
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, cap, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		if (hit == null) return false;
		LivingEntity target = (LivingEntity) hit.getEntity();

		// За спиной цели: -1.5 от направления взгляда цели
		Vec3 targetLook = target.getViewVector(1f);
		Vec3 behind = target.position().subtract(targetLook.scale(1.5));

		// Выпуск частиц на старте
		level.sendParticles(ParticleTypes.PORTAL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.4, 0.8, 0.4, 0.5);

		player.connection.teleport(behind.x, behind.y, behind.z,
				(target.getYRot() + 180) % 360, 0);
		player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.position().add(0, target.getEyeHeight(), 0));

		level.sendParticles(ParticleTypes.PORTAL,
				behind.x, behind.y + 1.0, behind.z,
				40, 0.4, 0.8, 0.4, 0.5);
		level.sendParticles(ParticleTypes.END_ROD,
				behind.x, behind.y + 1.0, behind.z,
				24, 0.4, 0.6, 0.4, 0.05);
		level.playSound(null, behind.x, behind.y, behind.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
		level.playSound(null, behind.x, behind.y, behind.z,
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
