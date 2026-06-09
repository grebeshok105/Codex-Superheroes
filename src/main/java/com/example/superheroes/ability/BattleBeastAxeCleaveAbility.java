package com.example.superheroes.ability;

import com.example.superheroes.effect.BattleBeastCurseController;
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

public final class BattleBeastAxeCleaveAbility implements Ability {
	private static final int COOLDOWN_TICKS = 6 * 20;
	private static final double RANGE = 7.0;
	private static final double CONE_DOT = 0.35;
	private static final float BASE_DAMAGE = 18.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.BATTLE_BEAST_AXE_CLEAVE;
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
		AABB scan = new AABB(eye, end).inflate(4.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
				target -> isValidTarget(player, target))) {
			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = targetCenter.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > RANGE) continue;
			double dot = toTarget.scale(1.0 / distance).dot(forward);
			if (dot < CONE_DOT) continue;

			float damage = BASE_DAMAGE + (float) ((1.0 - Math.min(distance / RANGE, 1.0)) * 8.0);
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), BattleBeastCurseController.scaleDamage(player, damage));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, true, true));
			Vec3 push = forward.scale(1.5).add(0.0, 0.35, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					targetCenter.x, targetCenter.y, targetCenter.z, 2, 0.3, 0.2, 0.3, 0.0);
		}

		for (int i = 1; i <= 10; i++) {
			Vec3 point = eye.add(forward.scale(i * (RANGE / 10.0)));
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 3, 0.18, 0.18, 0.18, 0.04);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.55f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.65f, 1.35f);
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
