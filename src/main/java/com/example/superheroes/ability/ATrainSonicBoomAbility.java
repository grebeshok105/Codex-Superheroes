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

public final class ATrainSonicBoomAbility implements Ability {
	private static final int COOLDOWN_TICKS = 9 * 20;
	private static final double RANGE = 18.0;
	private static final double CONE_DOT = 0.58;
	private static final float DAMAGE = 12.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.A_TRAIN_SONIC_BOOM;
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
		AABB scan = new AABB(eye, eye.add(forward.scale(RANGE))).inflate(4.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
				target -> isValidTarget(player, target))) {
			Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = center.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;
			double dot = toTarget.scale(1.0 / distance).dot(forward);
			if (dot < CONE_DOT) continue;
			target.hurt(level.damageSources().playerAttack(player), DAMAGE + (float) (dot * 4.0));
			target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0, true, true, true));
			Vec3 push = forward.scale(2.0).add(0.0, 0.35, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}
		Vec3 origin = eye.add(forward.scale(0.8));
		level.sendParticles(ParticleTypes.SONIC_BOOM, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
		for (int i = 1; i <= 12; i++) {
			Vec3 point = eye.add(forward.scale(i * (RANGE / 12.0)));
			level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z,
					6, 0.2 + i * 0.12, 0.08 + i * 0.03, 0.2 + i * 0.12, 0.04);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.35f);
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
