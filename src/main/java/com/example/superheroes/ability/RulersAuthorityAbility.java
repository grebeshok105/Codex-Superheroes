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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Ruler's Authority — выбрать цель в 15 блоках, подбросить вверх (sin 2.0)
 * и нанести 6 HP + стан 1.5с при пике/контакте.
 *
 *  - Cost: 25 ENERGY
 *  - CD: 300t (15с)
 */
public final class RulersAuthorityAbility implements Ability {
	private static final double RANGE = 15.0;
	private static final int COOLDOWN_TICKS = 300;
	private static final float DAMAGE = 6.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.RULERS_AUTHORITY;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 25f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1f).normalize();

		AABB box = new AABB(eye, eye).inflate(RANGE);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator() && eye.distanceToSqr(e.position()) <= RANGE * RANGE);
		LivingEntity target = candidates.stream()
				.max(Comparator.comparingDouble(e -> {
					Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye).normalize();
					return toE.dot(view);
				}))
				.orElse(null);
		if (target == null) return false;

		target.hurt(level.damageSources().playerAttack(player), DAMAGE);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, true, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 4, true, false, true));
		target.setDeltaMovement(target.getDeltaMovement().x, 2.0, target.getDeltaMovement().z);
		target.hurtMarked = true;
		target.hasImpulse = true;
		if (target instanceof ServerPlayer targetPlayer) {
			targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
		}

		Vec3 p = target.position();
		level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y + 1, p.z, 80, 0.6, 0.6, 0.6, 0.05);
		level.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 1, p.z, 60, 0.6, 1.5, 0.6, 0.4);
		level.playSound(null, p.x, p.y, p.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 0.7f);
		level.playSound(null, p.x, p.y, p.z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.RULERS_AUTHORITY, COOLDOWN_TICKS);
		return true;
	}
}
