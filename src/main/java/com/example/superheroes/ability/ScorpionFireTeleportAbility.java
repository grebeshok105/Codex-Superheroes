package com.example.superheroes.ability;

import com.example.superheroes.effect.ScorpionFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Hellport ("Toasty!") — Scorpion sinks into hellfire and reappears behind his
 * target with a burning slash, or blinks to the aimed spot if nobody is there.
 */
public final class ScorpionFireTeleportAbility implements Ability {
	private static final int COOLDOWN_TICKS = 7 * 20;
	private static final double BLINK_RANGE = 28.0;
	private static final double TARGET_SCAN = 20.0;
	private static final float AMBUSH_DAMAGE = 6.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCORPION_FIRE_TELEPORT;
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
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);

		LivingEntity target = findTargetInCone(player, level, eye, dir);

		Vec3 origin = player.position();
		spawnFlameBurst(level, origin);
		ScorpionFx.teleport(level, origin.add(0.0, 1.0, 0.0));
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.55f);

		Vec3 dest;
		if (target != null) {
			Vec3 behind = target.getLookAngle().normalize().scale(-1.2);
			dest = target.position().add(behind.x, 0, behind.z);
		} else {
			Vec3 end = eye.add(dir.scale(BLINK_RANGE));
			BlockHitResult hit = level.clip(new ClipContext(eye, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (hit.getType() == HitResult.Type.BLOCK) {
				dest = hit.getLocation().subtract(dir.scale(0.6)).subtract(0, 1.5, 0);
			} else {
				dest = end.subtract(0, 1.5, 0);
			}
		}

		if (target != null) {
			float yawTo = (float) Math.toDegrees(Math.atan2(
					target.getZ() - dest.z, target.getX() - dest.x)) - 90f;
			player.teleportTo(level, dest.x, dest.y, dest.z,
					Set.of(RelativeMovement.X_ROT), yawTo, 0f);
		} else {
			player.teleportTo(level, dest.x, dest.y, dest.z,
					Set.of(RelativeMovement.Y_ROT, RelativeMovement.X_ROT), 0f, 0f);
		}

		spawnFlameBurst(level, dest);
		ScorpionFx.teleport(level, dest.add(0.0, 1.0, 0.0));
		level.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.1f, 0.8f);
		level.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 0.55f);

		if (target != null) {
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), AMBUSH_DAMAGE);
			target.igniteForSeconds(4f);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
			level.sendParticles(ParticleTypes.FLAME,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					24, 0.4, 0.6, 0.4, 0.05);
			level.playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.4f, 0.8f);
		}

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1, true, false, true));

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void spawnFlameBurst(ServerLevel level, Vec3 pos) {
		level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.0, pos.z, 50, 0.45, 0.9, 0.45, 0.06);
		level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 6, 0.35, 0.5, 0.35, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 1.0, pos.z, 12, 0.4, 0.8, 0.4, 0.02);
	}

	private static LivingEntity findTargetInCone(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 dir) {
		AABB box = new AABB(eye.subtract(TARGET_SCAN, TARGET_SCAN, TARGET_SCAN),
				eye.add(TARGET_SCAN, TARGET_SCAN, TARGET_SCAN));
		LivingEntity best = null;
		double bestScore = -1.0;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.isCreative()))) {
			Vec3 toE = le.position().add(0, le.getBbHeight() * 0.5, 0).subtract(eye);
			double dist = toE.length();
			if (dist > TARGET_SCAN || dist < 0.5) {
				continue;
			}
			double dot = toE.normalize().dot(dir);
			if (dot < 0.6) {
				continue;
			}
			double score = dot - dist * 0.02;
			if (le instanceof Player) {
				score += 2.0;
			}
			if (score > bestScore) {
				bestScore = score;
				best = le;
			}
		}
		return best;
	}
}
