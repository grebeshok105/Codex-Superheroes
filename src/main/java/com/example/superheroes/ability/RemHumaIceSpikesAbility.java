package com.example.superheroes.ability;

import com.example.superheroes.effect.RemDemonismController;
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

public final class RemHumaIceSpikesAbility implements Ability {
	private static final int COOLDOWN_TICKS = 9 * 20;
	private static final double RANGE = 20.0;
	private static final double CONE_DOT = 0.68;
	private static final float DAMAGE = 13.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_HUMA_ICE_SPIKES;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 55f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return RemDemonismController.isActive(player) && !AbilityCooldowns.isOnCooldown(player, getId());
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
		AABB scan = new AABB(eye, end).inflate(3.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
				target -> isValidTarget(player, target))) {
			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = targetCenter.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;
			double dot = toTarget.scale(1.0 / distance).dot(forward);
			if (dot < CONE_DOT) continue;
			target.hurt(level.damageSources().playerAttack(player), DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1, true, true, true));
			target.push(forward.x * 0.45, 0.22, forward.z * 0.45);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					targetCenter.x, targetCenter.y, targetCenter.z, 36, 0.35, 0.45, 0.35, 0.08);
			level.sendParticles(ParticleTypes.END_ROD,
					targetCenter.x, targetCenter.y + 0.2, targetCenter.z, 18, 0.25, 0.35, 0.25, 0.04);
			level.sendParticles(ParticleTypes.CLOUD,
					targetCenter.x, targetCenter.y, targetCenter.z, 14, 0.35, 0.25, 0.35, 0.02);
		}
		Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
		if (right.lengthSqr() < 1.0e-4) {
			right = new Vec3(1.0, 0.0, 0.0);
		} else {
			right = right.normalize();
		}
		for (int i = 1; i <= 24; i++) {
			double t = i / 24.0;
			Vec3 base = eye.add(forward.scale(t * RANGE));
			double wave = Math.sin(t * Math.PI * 4.0) * 0.55;
			Vec3 point = base.add(right.scale(wave));
			level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 5, 0.14, 0.14, 0.14, 0.035);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 0.05, point.z, 2, 0.08, 0.08, 0.08, 0.02);
				level.sendParticles(ParticleTypes.CLOUD, point.x, point.y - 0.05, point.z, 2, 0.18, 0.05, 0.18, 0.01);
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.1f, 1.55f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.75f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.25f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}
}
