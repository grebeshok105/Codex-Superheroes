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

public final class KazuhaAutumnWhirlwindAbility implements Ability {
	private static final int EFFECT_TICKS = 40;
	private static final int EXIT_COOLDOWN_TICKS = 120;
	private static final double RADIUS = 4.5;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.28f, 0.95f, 0.72f), 1.2f);
	private static final DustParticleOptions MAPLE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.38f, 0.16f), 1.15f);

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KAZUHA_AUTUMN_WHIRLWIND;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 25f;
	}

	@Override
	public float costPerTick() {
		return 1.4f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		applyWind(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
				player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.9f, 1.45f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		applyWind(player);
		if (player.tickCount % 10 == 0) {
			stirNearby(player);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		player.removeEffect(MobEffects.MOVEMENT_SPEED);
		player.removeEffect(MobEffects.JUMP);
		player.removeEffect(MobEffects.SLOW_FALLING);
		AbilityCooldowns.setCooldownTicks(player, getId(), EXIT_COOLDOWN_TICKS);
	}

	private static void applyWind(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, EFFECT_TICKS, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_TICKS, 0, true, false, true));
		player.fallDistance = 0f;
	}

	private static void stirNearby(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 center = player.position().add(0, 0.8, 0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, true, true));
			Vec3 push = target.position().subtract(player.position());
			double horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
			if (horizontal > 0.001) {
				Vec3 motion = new Vec3(push.x / horizontal * 0.35, 0.12, push.z / horizontal * 0.35);
				target.setDeltaMovement(target.getDeltaMovement().add(motion));
				target.hurtMarked = true;
				target.hasImpulse = true;
				if (target instanceof ServerPlayer targetPlayer) {
					targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
				}
			}
		}
		level.sendParticles(ParticleTypes.GUST,
				center.x, center.y, center.z, 16, RADIUS * 0.45, 0.4, RADIUS * 0.45, 0.03);
		level.sendParticles(ANEMO_DUST,
				center.x, center.y, center.z, 18, RADIUS * 0.4, 0.45, RADIUS * 0.4, 0.01);
		level.sendParticles(MAPLE_DUST,
				center.x, center.y + 0.2, center.z, 10, RADIUS * 0.35, 0.6, RADIUS * 0.35, 0.02);
	}
}
