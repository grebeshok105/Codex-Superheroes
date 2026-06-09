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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ScaramoucheElectroSwirlAbility implements Ability {
	private static final int COOLDOWN_TICKS = 7 * 20;
	private static final double RANGE = 24.0;
	private static final double CONE_DOT = 0.48;
	private static final double SCAN_INFLATE = 3.4;
	private static final float BASE_DAMAGE = 12.0f;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.28f, 1.0f, 0.84f), 1.25f);
	private static final DustParticleOptions ELECTRO_DUST = new DustParticleOptions(new Vector3f(0.58f, 0.36f, 1.0f), 1.1f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCARAMOUCHE_ELECTRO_SWIRL;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 70f;
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

		AABB scan = new AABB(eye, end).inflate(SCAN_INFLATE);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
				target -> isValidTarget(player, target))) {
			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = targetCenter.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;
			Vec3 directionToTarget = toTarget.scale(1.0 / distance);
			double dot = directionToTarget.dot(forward);
			if (dot < CONE_DOT) continue;

			float damage = BASE_DAMAGE + (float) ((dot - CONE_DOT) / (1.0 - CONE_DOT) * 6.0);
			target.hurt(level.damageSources().playerAttack(player), damage);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, true, true));

			Vec3 push = forward.scale(0.85).add(0.0, 0.28, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}

			level.sendParticles(ELECTRO_DUST,
					targetCenter.x, targetCenter.y, targetCenter.z, 28, 0.35, 0.45, 0.35, 0.0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					targetCenter.x, targetCenter.y, targetCenter.z, 18, 0.3, 0.35, 0.3, 0.06);
		}

		int steps = (int) Math.max(18, RANGE * 2.0);
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			double spread = 0.08 + t * 1.4;
			Vec3 point = eye.add(forward.scale(t * RANGE));
			level.sendParticles(ANEMO_DUST,
					point.x, point.y, point.z, 2, spread, spread * 0.28, spread, 0.0);
			if (i % 2 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						point.x, point.y, point.z, 1, spread * 0.35, spread * 0.18, spread * 0.35, 0.02);
			}
			if (i % 5 == 0) {
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,
						point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.35f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.45f, 1.8f);

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
