package com.example.superheroes.ability;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ViltrumiteThunderClapAbility implements Ability {
	private static final int COOLDOWN_TICKS = 180;
	private static final int SLOW_TICKS = 70;
	private static final double RANGE = 18.0;
	private static final double CONE_DOT = 0.66;
	private static final double HIT_SCAN_INFLATE = 3.2;
	private static final double KNOCKBACK = 2.7;
	private static final float DAMAGE = 16.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.VILTRUMITE_THUNDER_CLAP;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 45f;
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
		AABB scan = new AABB(eye, end).inflate(HIT_SCAN_INFLATE);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan, this::isValidTarget)) {
			if (target == player) continue;

			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = targetCenter.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;

			Vec3 directionToTarget = toTarget.scale(1.0 / distance);
			double dot = directionToTarget.dot(forward);
			if (dot < CONE_DOT) continue;

			float damage = (float) (DAMAGE * (0.75 + 0.25 * dot));
			target.hurt(level.damageSources().playerAttack(player), damage);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1, true, true, true));

			double falloff = 1.0 - Math.min(distance / RANGE, 1.0) * 0.35;
			Vec3 push = forward.scale(KNOCKBACK * falloff).add(0.0, 0.42 * falloff, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}

			level.sendParticles(ParticleTypes.CRIT,
					targetCenter.x, targetCenter.y, targetCenter.z,
					10, 0.35, 0.35, 0.35, 0.12);
		}

		sendBlastEffects(level, eye, forward, player);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private boolean isValidTarget(LivingEntity target) {
		return target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}

	private void sendBlastEffects(ServerLevel level, Vec3 eye, Vec3 forward, ServerPlayer player) {
		Vec3 clap = eye.add(forward.scale(0.9));
		level.sendParticles(ParticleTypes.SONIC_BOOM, clap.x, clap.y, clap.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION,
				clap.x, clap.y, clap.z,
				2, 0.25, 0.25, 0.25, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				clap.x, clap.y - 0.15, clap.z,
				4, 0.35, 0.2, 0.35, 0.0);

		for (int i = 1; i <= 12; i++) {
			double distance = i * (RANGE / 12.0);
			double spread = 0.18 + i * 0.18;
			Vec3 point = eye.add(forward.scale(distance));
			level.sendParticles(ParticleTypes.CLOUD,
					point.x, point.y, point.z,
					6, spread, spread * 0.35, spread, 0.04);
			if (i % 2 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						point.x, point.y, point.z,
						2, spread * 0.35, spread * 0.2, spread * 0.35, 0.02);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.25f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.85f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 0.65f);
	}
}
