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

import java.util.List;

public final class KazuhaChihayaburuAbility implements Ability {
	private static final int COOLDOWN_TICKS = 160;
	private static final double RADIUS = 6.0;
	private static final double PULL_STRENGTH = 1.25;
	private static final float DAMAGE = 5.0f;
	private static final DustParticleOptions MAPLE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.45f, 0.18f), 1.25f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KAZUHA_CHIHAYABURU;
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
		Vec3 center = player.position().add(0, 0.7, 0);
		Vec3 look = player.getLookAngle().normalize();
		if (look.lengthSqr() < 1.0e-4) {
			look = new Vec3(0, 0, 1);
		}
		Vec3 launch = look.scale(0.55).add(0, 1.05, 0);
		player.setDeltaMovement(launch);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));

		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()));
		for (LivingEntity target : targets) {
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 24, 0, true, true, true));
			Vec3 motion = pullMotion(center, target);
			target.setDeltaMovement(motion);
			target.hurtMarked = true;
			target.hasImpulse = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}

		level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, center.x, center.y, center.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 80, RADIUS * 0.35, 0.45, RADIUS * 0.35, 0.12);
		level.sendParticles(MAPLE_DUST, center.x, center.y + 0.2, center.z, 55, RADIUS * 0.28, 0.6, RADIUS * 0.28, 0.04);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 1.3f, 1.35f);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0f, 1.15f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static Vec3 pullMotion(Vec3 center, LivingEntity target) {
		Vec3 pull = center.subtract(target.position());
		double horizontal = Math.sqrt(pull.x * pull.x + pull.z * pull.z);
		if (horizontal < 0.001) {
			return new Vec3(0, 0.75, 0);
		}
		return new Vec3(pull.x / horizontal * PULL_STRENGTH, 0.75, pull.z / horizontal * PULL_STRENGTH);
	}
}
