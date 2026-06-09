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

public final class BattleBeastPredatorLeapAbility implements Ability {
	private static final int COOLDOWN_TICKS = 8 * 20;
	private static final double DISTANCE = 14.0;
	private static final double HIT_RADIUS = 2.2;
	private static final float DAMAGE = 20.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.BATTLE_BEAST_PREDATOR_LEAP;
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
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}
		Vec3 motion = forward.scale(1.75).add(0.0, player.onGround() ? 0.65 : 0.25, 0.0);
		player.setDeltaMovement(motion);
		player.fallDistance = 0f;
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		Vec3 from = player.position();
		Vec3 to = from.add(forward.scale(DISTANCE));
		AABB sweep = new AABB(from, to).inflate(HIT_RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep,
				target -> isValidTarget(player, target))) {
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), BattleBeastCurseController.scaleDamage(player, DAMAGE));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1, true, true, true));
			Vec3 push = forward.scale(2.2).add(0.0, 0.55, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			level.sendParticles(ParticleTypes.CRIT,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					18, 0.35, 0.35, 0.35, 0.14);
		}

		level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.25, player.getZ(),
				34, 0.6, 0.2, 0.6, 0.12);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.2f, 0.75f);
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
