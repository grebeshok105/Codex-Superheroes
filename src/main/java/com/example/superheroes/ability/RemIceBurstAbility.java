package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RemIceBurstAbility implements Ability {
	private static final int COOLDOWN_TICKS = 7 * 20;
	private static final double RANGE = 11.0;
	private static final double CONE_DOT = 0.70;
	private static final float DAMAGE = 9.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_ICE_BURST;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 42f;
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
		AABB scan = new AABB(eye, end).inflate(2.6);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
				target -> isValidTarget(player, target))) {
			Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = center.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;
			if (toTarget.scale(1.0 / distance).dot(forward) < CONE_DOT) continue;
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true, true));
			Vec3 push = forward.scale(0.55).add(0.0, 0.12, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					center.x, center.y, center.z, 22, 0.35, 0.35, 0.35, 0.06);
			level.sendParticles(ParticleTypes.CLOUD,
					center.x, center.y - 0.05, center.z, 8, 0.35, 0.14, 0.35, 0.02);
		}
		player.swing(InteractionHand.MAIN_HAND, true);
		for (int i = 1; i <= 14; i++) {
			double t = i / 14.0;
			Vec3 point = eye.add(forward.scale(t * RANGE));
			level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y - 0.35, point.z,
					4, 0.24 * t, 0.08, 0.24 * t, 0.03);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, point.x, point.y - 0.2, point.z,
						2, 0.16, 0.08, 0.16, 0.02);
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.65f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.25f);
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
