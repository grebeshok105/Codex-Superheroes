package com.example.superheroes.ability;

import com.example.superheroes.entity.SmartMissileEntity;
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
 * Залп самонаводящихся нано-ракет Железного Человека (3 штуки). Захватывает
 * цель под прицелом (или ближайшую в конусе) и выпускает веер ракет, которые
 * довернут к цели. Каждая детонирует по площади. Привязана к
 * {@link AbilityIds#IRON_MAN_SMART_MISSILE}.
 */
public final class SmartMissileAbility implements Ability {
	private static final int COUNT = 3;
	private static final double RANGE = 40.0;
	private static final int COOLDOWN_TICKS = 60;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_SMART_MISSILE;
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
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		LivingEntity target = acquireTarget(player, level);

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		Vec3 up = new Vec3(0, 1, 0);
		Vec3 right = look.cross(up).normalize();
		Vec3 localUp = right.cross(look).normalize();

		for (int i = 0; i < COUNT; i++) {
			// веер: -1, 0, +1
			double spread = (i - (COUNT - 1) / 2.0) * 0.18;
			Vec3 dir = look.add(right.scale(spread)).add(localUp.scale(0.06)).normalize();
			Vec3 spawn = eye.add(look.scale(0.6)).add(right.scale(spread * 1.2));
			SmartMissileEntity m = SmartMissileEntity.launch(player, level, spawn, dir, target);
			level.addFreshEntity(m);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.1f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f, 0.8f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private LivingEntity acquireTarget(ServerPlayer player, ServerLevel level) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult bh = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 actualEnd = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(2.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, actualEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		if (hit != null) {
			return (LivingEntity) hit.getEntity();
		}
		// ближайшая живность в конусе перед игроком
		LivingEntity best = null;
		double bestScore = -1;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(RANGE),
				e -> e.isAlive() && !e.isSpectator() && e != player)) {
			Vec3 to = le.position().add(0, le.getBbHeight() * 0.5, 0).subtract(eye);
			double dist = to.length();
			if (dist < 0.1 || dist > RANGE) {
				continue;
			}
			double dot = to.normalize().dot(dir);
			if (dot < 0.6) {
				continue;
			}
			double score = dot - dist / RANGE * 0.3;
			if (score > bestScore) {
				bestScore = score;
				best = le;
			}
		}
		return best;
	}
}
