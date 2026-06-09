package com.example.superheroes.ability;

import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ScaramoucheSkyfallBurstAbility implements Ability {
	private static final int COOLDOWN_TICKS = 22 * 20;
	private static final double RANGE = 42.0;
	private static final double RADIUS = 7.0;
	private static final float CENTER_DAMAGE = 36.0f;
	private static final float EDGE_DAMAGE = 14.0f;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.30f, 1.0f, 0.84f), 1.45f);
	private static final DustParticleOptions ELECTRO_DUST = new DustParticleOptions(new Vector3f(0.64f, 0.38f, 1.0f), 1.2f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCARAMOUCHE_SKYFALL_BURST;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 180f;
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
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}
		Vec3 end = eye.add(forward.scale(RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 impact = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
		AABB rayBox = new AABB(eye, impact).inflate(2.2);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, impact, rayBox,
				entity -> entity instanceof LivingEntity living && isValidTarget(player, living));
		if (entityHit != null) {
			impact = entityHit.getLocation();
		}

		spawnBeam(level, eye, impact);
		AABB aoe = new AABB(impact, impact).inflate(RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, aoe,
				target -> isValidTarget(player, target))) {
			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			double distance = targetCenter.distanceTo(impact);
			if (distance > RADIUS) continue;
			float falloff = (float) Math.max(0.0, 1.0 - distance / RADIUS);
			float damage = EDGE_DAMAGE + (CENTER_DAMAGE - EDGE_DAMAGE) * falloff;
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), damage);
			target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 1, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, true, true, true));
			Vec3 away = target.position().subtract(impact);
			double horizontal = Math.max(0.01, Math.sqrt(away.x * away.x + away.z * away.z));
			target.push(away.x / horizontal * 1.1, 0.7 + falloff * 0.4, away.z / horizontal * 1.1);
			target.hurtMarked = true;
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
				impact.x, impact.y + 0.5, impact.z, 3, RADIUS * 0.2, RADIUS * 0.15, RADIUS * 0.2, 0.0);
		level.sendParticles(ANEMO_DUST,
				impact.x, impact.y + 0.8, impact.z, 220, RADIUS * 0.45, 1.2, RADIUS * 0.45, 0.0);
		level.sendParticles(ELECTRO_DUST,
				impact.x, impact.y + 0.8, impact.z, 160, RADIUS * 0.35, 1.0, RADIUS * 0.35, 0.0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				impact.x, impact.y + 0.8, impact.z, 90, RADIUS * 0.35, 0.9, RADIUS * 0.35, 0.12);
		level.sendParticles(ParticleTypes.FLASH,
				impact.x, impact.y + 1.0, impact.z, 2, 0.0, 0.0, 0.0, 0.0);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.6f);
		level.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.45f, 1.15f);
		level.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.8f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void spawnBeam(ServerLevel level, Vec3 start, Vec3 end) {
		double distance = start.distanceTo(end);
		if (distance < 0.001) {
			return;
		}
		Vec3 direction = end.subtract(start).scale(1.0 / distance);
		int steps = (int) Math.max(18, distance * 2.0);
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			Vec3 point = start.add(direction.scale(t * distance));
			level.sendParticles(ANEMO_DUST,
					point.x, point.y, point.z, 2, 0.08, 0.08, 0.08, 0.0);
			if (i % 2 == 0) {
				level.sendParticles(ELECTRO_DUST,
						point.x, point.y, point.z, 1, 0.06, 0.06, 0.06, 0.0);
			}
			if (i % 4 == 0) {
				level.sendParticles(ParticleTypes.END_ROD,
						point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
		}
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}
}
