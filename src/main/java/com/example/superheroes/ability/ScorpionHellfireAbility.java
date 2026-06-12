package com.example.superheroes.ability;

import com.example.superheroes.effect.ScorpionFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hellfire Eruption — a pillar of infernal flame bursts out of the ground at
 * the aimed point, launching and igniting everything around it.
 */
public final class ScorpionHellfireAbility implements Ability {
	private static final int COOLDOWN_TICKS = 10 * 20;
	private static final double CAST_RANGE = 26.0;
	private static final double BLAST_RADIUS = 3.2;
	private static final float DAMAGE = 10.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCORPION_HELLFIRE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 35f;
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
		Vec3 forward = player.getViewVector(1f);
		Vec3 end = eye.add(forward.scale(CAST_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 center = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;

		// Snap down to the ground so the pillar erupts from the floor.
		BlockHitResult ground = level.clip(new ClipContext(center.add(0, 0.5, 0),
				center.add(0, -12.0, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (ground.getType() == HitResult.Type.BLOCK) {
			center = ground.getLocation();
		}

		spawnPillarFx(level, center);
		ScorpionFx.pillar(level, center);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, 0.7f);

		AABB area = new AABB(center, center).inflate(BLAST_RADIUS, BLAST_RADIUS + 1.0, BLAST_RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
				t -> isValidTarget(player, t))) {
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), DAMAGE);
			target.igniteForSeconds(5f);
			Vec3 motion = target.getDeltaMovement();
			target.setDeltaMovement(motion.x * 0.4, Math.max(motion.y, 0.55), motion.z * 0.4);
			target.hurtMarked = true;
		}

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void spawnPillarFx(ServerLevel level, Vec3 center) {
		// Rising column of flame.
		for (int y = 0; y <= 12; y++) {
			double height = y * 0.45;
			double width = Math.max(0.25, BLAST_RADIUS * 0.55 - y * 0.08);
			level.sendParticles(ParticleTypes.FLAME,
					center.x, center.y + height, center.z, 10, width, 0.12, width, 0.05);
			if (y % 3 == 0) {
				level.sendParticles(ParticleTypes.LAVA,
						center.x, center.y + height, center.z, 2, width, 0.1, width, 0.0);
			}
		}
		// Expanding fire ring on the ground.
		for (int i = 0; i < 28; i++) {
			double angle = (Math.PI * 2.0 * i) / 28.0;
			double px = center.x + Math.cos(angle) * BLAST_RADIUS;
			double pz = center.z + Math.sin(angle) * BLAST_RADIUS;
			level.sendParticles(ParticleTypes.FLAME, px, center.y + 0.2, pz, 2, 0.08, 0.10, 0.08, 0.02);
			level.sendParticles(ParticleTypes.SMALL_FLAME, px, center.y + 0.1, pz, 1, 0.05, 0.05, 0.05, 0.01);
		}
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				center.x, center.y + 1.2, center.z, 14, 1.0, 1.2, 1.0, 0.02);
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}
}
