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

public final class KazuhaMapleStormAbility implements Ability {
	private static final int COOLDOWN_TICKS = 560;
	private static final double RADIUS = 12.0;
	private static final double PULL_STRENGTH = 1.1;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.36f, 1.0f, 0.78f), 1.7f);
	private static final DustParticleOptions MAPLE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.28f, 0.08f), 1.55f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KAZUHA_MAPLE_STORM;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 110f;
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
		Vec3 center = player.position().add(0, 0.8, 0);
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false, true));
		player.fallDistance = 0f;

		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()));
		for (LivingEntity target : targets) {
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 0, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, true));
			Vec3 motion = pullMotion(center, target);
			target.setDeltaMovement(motion);
			target.hurtMarked = true;
			target.hasImpulse = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}

		level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.5, center.z, 2, 0.2, 0.2, 0.2, 0);
		level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, center.x, center.y + 0.2, center.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.GUST, center.x, center.y + 0.4, center.z, 120, RADIUS * 0.45, 0.8, RADIUS * 0.45, 0.08);
		level.sendParticles(ANEMO_DUST, center.x, center.y + 0.4, center.z, 140, RADIUS * 0.5, 1.0, RADIUS * 0.5, 0.03);
		level.sendParticles(MAPLE_DUST, center.x, center.y + 0.8, center.z, 120, RADIUS * 0.45, 1.2, RADIUS * 0.45, 0.05);
		for (int i = 0; i < 28; i++) {
			double angle = (Math.PI * 2.0 * i) / 28.0;
			double x = center.x + Math.cos(angle) * RADIUS * 0.72;
			double z = center.z + Math.sin(angle) * RADIUS * 0.72;
			level.sendParticles(MAPLE_DUST, x, center.y + 0.35, z, 2, 0.08, 0.15, 0.08, 0.0);
		}
		level.playSound(null, center.x, center.y, center.z, SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 1.4f, 0.75f);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.8f, 0.65f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static Vec3 pullMotion(Vec3 center, LivingEntity target) {
		Vec3 pull = center.subtract(target.position());
		double horizontal = Math.sqrt(pull.x * pull.x + pull.z * pull.z);
		if (horizontal < 0.001) {
			return new Vec3(0, 0.45, 0);
		}
		return new Vec3(pull.x / horizontal * PULL_STRENGTH, 0.45, pull.z / horizontal * PULL_STRENGTH);
	}
}
