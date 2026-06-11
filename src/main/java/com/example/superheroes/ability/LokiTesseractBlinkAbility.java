package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LokiTesseractBlinkAbility implements Ability {
	private static final int COOLDOWN_TICKS = 30;
	private static final double RANGE = 60.0;
	private static final double TARGET_SCAN = 24.0;
	private static final float BACKSTAB_DAMAGE = 40.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.LOKI_TESSERACT_BLINK;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 30f;
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
		level.sendParticles(ParticleTypes.PORTAL,
				origin.x, origin.y + 1, origin.z, 60, 0.5, 1.0, 0.5, 0.6);
		level.sendParticles(ModParticles.DARK_STAR,
				origin.x, origin.y + 1, origin.z, 30, 0.4, 0.8, 0.4, 0.05);
		level.sendParticles(ModParticles.PURPLE_FLAME,
				origin.x, origin.y + 1, origin.z, 24, 0.4, 0.8, 0.4, 0.05);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.6f);

		Vec3 dest;
		if (target != null) {
			Vec3 tgtPos = target.position();
			Vec3 behind = target.getLookAngle().normalize().scale(-1.2);
			dest = tgtPos.add(behind.x, 0, behind.z);
		} else {
			Vec3 end = eye.add(dir.scale(RANGE));
			BlockHitResult bh = level.clip(new ClipContext(eye, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (bh.getType() == HitResult.Type.BLOCK) {
				Vec3 hit = bh.getLocation();
				dest = hit.subtract(dir.scale(0.6)).subtract(0, 1.5, 0);
			} else {
				dest = end.subtract(0, 1.5, 0);
			}
		}

		// Single clean teleport: keep client rotation relative (no forced camera
		// snap — the old teleport + setYRot + double resetPosition desynced the
		// client camera and flipped/stuck the screen).
		if (target != null) {
			float yawTo = (float) Math.toDegrees(Math.atan2(
					target.getZ() - dest.z, target.getX() - dest.x)) - 90f;
			player.teleportTo(level, dest.x, dest.y, dest.z,
					java.util.Set.of(net.minecraft.world.entity.RelativeMovement.X_ROT), yawTo, 0f);
		} else {
			player.teleportTo(level, dest.x, dest.y, dest.z,
					java.util.Set.of(net.minecraft.world.entity.RelativeMovement.Y_ROT,
							net.minecraft.world.entity.RelativeMovement.X_ROT), 0f, 0f);
		}

		level.sendParticles(ParticleTypes.PORTAL,
				dest.x, dest.y + 1, dest.z, 80, 0.6, 1.0, 0.6, 0.7);
		level.sendParticles(ModParticles.DARK_STAR,
				dest.x, dest.y + 1, dest.z, 40, 0.5, 1.0, 0.5, 0.06);

		if (target != null) {
			target.hurt(ModDamageTypes.lokiChaos(level, player), BACKSTAB_DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true, true));
			level.sendParticles(ModParticles.PURPLE_FLAME,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					30, 0.4, 0.6, 0.4, 0.04);
			level.playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.6f, 0.7f);
		}

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, true, false, true));

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity findTargetInCone(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 dir) {
		AABB box = new AABB(eye.subtract(TARGET_SCAN, TARGET_SCAN, TARGET_SCAN),
				eye.add(TARGET_SCAN, TARGET_SCAN, TARGET_SCAN));
		LivingEntity best = null;
		double bestScore = -1.0;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive())) {
			if (le instanceof Player p && p.getUUID().equals(player.getUUID())) continue;
			Vec3 toE = le.position().add(0, le.getBbHeight() * 0.5, 0).subtract(eye);
			double dist = toE.length();
			if (dist > TARGET_SCAN || dist < 0.5) continue;
			double dot = toE.normalize().dot(dir);
			if (dot < 0.6) continue;
			double score = dot - dist * 0.02;
			if (le instanceof Player) score += 2.0;
			if (score > bestScore) {
				bestScore = score;
				best = le;
			}
		}
		return best;
	}
}
