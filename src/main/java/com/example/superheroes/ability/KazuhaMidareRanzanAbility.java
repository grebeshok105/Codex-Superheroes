package com.example.superheroes.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
import org.joml.Vector3f;

import java.util.List;

public final class KazuhaMidareRanzanAbility implements Ability {
	private static final int COOLDOWN_TICKS = 220;
	private static final double RANGE = 8.0;
	private static final double RADIUS = 4.2;
	private static final float GROUND_DAMAGE = 9.0f;
	private static final float AIR_DAMAGE = 13.0f;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.34f, 1.0f, 0.74f), 1.45f);
	private static final DustParticleOptions MAPLE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.34f, 0.12f), 1.25f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KAZUHA_MIDARE_RANZAN;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 65f;
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
		boolean airborne = !player.onGround();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f).normalize();
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 impact = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : eye.add(dir.scale(4.5));
		if (!airborne) {
			impact = new Vec3(impact.x, player.getY() + 0.25, impact.z);
		}

		if (airborne) {
			Vec3 plunge = dir.scale(0.35).add(0, -0.95, 0);
			player.setDeltaMovement(plunge);
			player.hurtMarked = true;
			player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), plunge));
		}
		player.fallDistance = 0f;

		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(impact, impact).inflate(RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()));
		float damage = airborne ? AIR_DAMAGE : GROUND_DAMAGE;
		for (LivingEntity target : targets) {
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), damage);
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0, true, true, true));
			Vec3 push = pushFrom(impact, target, airborne ? 1.45 : 1.05, airborne ? 0.55 : 0.35);
			target.setDeltaMovement(push);
			target.hurtMarked = true;
			target.hasImpulse = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}

		spawnSlash(level, eye, impact);
		level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, impact.x, impact.y + 0.2, impact.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, impact.x, impact.y + 0.6, impact.z, 5, RADIUS * 0.4, 0.25, RADIUS * 0.4, 0);
		level.sendParticles(ANEMO_DUST, impact.x, impact.y + 0.4, impact.z, 70, RADIUS * 0.45, 0.25, RADIUS * 0.45, 0.02);
		level.sendParticles(MAPLE_DUST, impact.x, impact.y + 0.7, impact.z, 45, RADIUS * 0.35, 0.5, RADIUS * 0.35, 0.04);
		level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.85f);
		level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.4f, airborne ? 0.7f : 1.0f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static Vec3 pushFrom(Vec3 center, LivingEntity target, double strength, double upward) {
		Vec3 away = target.position().subtract(center);
		double horizontal = Math.sqrt(away.x * away.x + away.z * away.z);
		if (horizontal < 0.001) {
			return new Vec3(0, upward, 0);
		}
		return new Vec3(away.x / horizontal * strength, upward, away.z / horizontal * strength);
	}

	private static void spawnSlash(ServerLevel level, Vec3 start, Vec3 end) {
		Vec3 delta = end.subtract(start);
		int steps = 18;
		for (int i = 0; i <= steps; i++) {
			Vec3 p = start.add(delta.scale((double) i / steps));
			level.sendParticles(ParticleTypes.GUST, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.01);
			if (i % 2 == 0) {
				level.sendParticles(ANEMO_DUST, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
			}
		}
	}
}
