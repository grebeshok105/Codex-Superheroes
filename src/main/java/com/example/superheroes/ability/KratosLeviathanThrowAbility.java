package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
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

public final class KratosLeviathanThrowAbility implements Ability {
	private static final int COOLDOWN_TICKS = 240;
	private static final double RANGE = 32.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KRATOS_LEVIATHAN_THROW;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 150f;
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
		Vec3 viewDir = player.getViewVector(1f);

		LivingEntity target = pickTarget(player, level, eye, viewDir);

		Vec3 dir;
		Vec3 impact;
		if (target != null) {
			impact = target.position().add(0, target.getBbHeight() / 2, 0);
			dir = impact.subtract(eye).normalize();
		} else {
			dir = viewDir;
			Vec3 end = eye.add(dir.scale(RANGE));
			BlockHitResult bh = level.clip(new ClipContext(eye, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			impact = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		}

		double traveled = eye.distanceTo(impact);
		int steps = (int) Math.max(12, traveled * 3);
		for (int i = 0; i < steps; i++) {
			double t = (double) i / steps;
			Vec3 p = eye.add(dir.scale(t * traveled));
			level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 3, 0.2, 0.2, 0.2, 0.05);
			if (i % 4 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		if (target != null) {
			final LivingEntity primary = target;
			primary.hurt(ModDamageTypes.kratosLeviathan(level, player), 36.0f);
			primary.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, true, true, true));
			primary.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, true, true, true));
			AABB splash = primary.getBoundingBox().inflate(3.5);
			for (LivingEntity neighbor : level.getEntitiesOfClass(LivingEntity.class, splash,
					e -> e != player && e != primary && e.isAlive()
							&& !(e instanceof Player p2 && p2.getUUID().equals(player.getUUID())))) {
				neighbor.hurt(ModDamageTypes.kratosLeviathan(level, player), 16.0f);
				neighbor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, true, true, true));
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE, impact.x, impact.y, impact.z, 160, 1.4, 1.0, 1.4, 0.2);
			level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 4, 0.8, 0.5, 0.8, 0.0);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity pickTarget(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 viewDir) {
		AABB scan = player.getBoundingBox().inflate(RANGE);
		LivingEntity best = null;
		double bestScore = -Double.MAX_VALUE;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			Vec3 toEntity = le.position().add(0, le.getBbHeight() / 2, 0).subtract(eye);
			double dist = toEntity.length();
			if (dist < 0.001 || dist > RANGE) continue;
			double dot = toEntity.scale(1.0 / dist).dot(viewDir);
			if (dot < 0.0) continue;
			double score = dot * 1.5 - dist / RANGE;
			if (score > bestScore) {
				bestScore = score;
				best = le;
			}
		}
		return best;
	}
}
