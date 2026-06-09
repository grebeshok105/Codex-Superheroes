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

public final class ScaramoucheWindstepAbility implements Ability {
	private static final int COOLDOWN_TICKS = 5 * 20;
	private static final double RADIUS = 4.0;
	private static final float DAMAGE = 7.0f;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.32f, 1.0f, 0.86f), 1.35f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCARAMOUCHE_WINDSTEP;
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
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}

		Vec3 current = player.getDeltaMovement();
		Vec3 motion = new Vec3(current.x * 0.25, Math.max(current.y, 0.0), current.z * 0.25)
				.add(forward.scale(1.35))
				.add(0.0, player.onGround() ? 0.88 : 0.58, 0.0);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, true, false, true));

		AABB area = player.getBoundingBox().inflate(RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
				target -> isValidTarget(player, target))) {
			Vec3 away = target.position().subtract(player.position());
			double horizontal = Math.max(0.01, Math.sqrt(away.x * away.x + away.z * away.z));
			target.hurt(level.damageSources().playerAttack(player), DAMAGE);
			target.push(away.x / horizontal * 1.15, 0.45, away.z / horizontal * 1.15);
			target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 24, 0, true, true, true));
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}

		level.sendParticles(ANEMO_DUST,
				player.getX(), player.getY() + 0.8, player.getZ(), 70, 0.8, 0.55, 0.8, 0.0);
		level.sendParticles(ParticleTypes.CLOUD,
				player.getX(), player.getY() + 0.25, player.getZ(), 42, 0.9, 0.25, 0.9, 0.16);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 0.9, player.getZ(), 28, 0.8, 0.45, 0.8, 0.06);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.65f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.8f);

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
