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

public final class RemOniKickAbility implements Ability {
	private static final int COOLDOWN_TICKS = 5 * 20;
	private static final double DISTANCE = 12.0;
	private static final float DAMAGE = 16.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REM_ONI_KICK;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 35f;
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
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}
		Vec3 motion = new Vec3(forward.x * 2.6, Math.max(0.03, forward.y * 0.12), forward.z * 2.6);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		Vec3 from = player.position();
		Vec3 to = from.add(forward.scale(DISTANCE));
		AABB sweep = new AABB(from, to).inflate(1.4);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, target -> isValidTarget(player, target))) {
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, true, true));
			Vec3 push = forward.scale(2.1).add(0.0, 0.3, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			level.sendParticles(ParticleTypes.CRIT,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					16, 0.25, 0.3, 0.25, 0.14);
		}
		for (int i = 0; i < 10; i++) {
			Vec3 point = from.add(forward.scale(i * (DISTANCE / 10.0)));
			level.sendParticles(ParticleTypes.CLOUD, point.x, point.y + 0.1, point.z, 3, 0.2, 0.04, 0.2, 0.02);
			level.sendParticles(ParticleTypes.CHERRY_LEAVES, point.x, point.y + 0.7, point.z, 1, 0.08, 0.08, 0.08, 0.01);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 1.35f);
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
