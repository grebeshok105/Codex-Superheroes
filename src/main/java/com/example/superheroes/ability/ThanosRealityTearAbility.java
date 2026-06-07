package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
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

public final class ThanosRealityTearAbility implements Ability {
	private static final int COOLDOWN_TICKS = 160;
	private static final double RADIUS = 14.0;
	private static final float PULL_STRENGTH = 1.6f;
	private static final float DAMAGE_PER_TARGET = 16.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.THANOS_REALITY_TEAR;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 100f;
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
		Vec3 anchor = player.position().add(player.getViewVector(1f).scale(6.0));

		AABB aoe = new AABB(
				anchor.x - RADIUS, anchor.y - 4, anchor.z - RADIUS,
				anchor.x + RADIUS, anchor.y + 6, anchor.z + RADIUS);

		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, aoe,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			Vec3 toCenter = anchor.subtract(le.position()).normalize().scale(PULL_STRENGTH);
			le.setDeltaMovement(toCenter.x, 0.6, toCenter.z);
			le.hurtMarked = true;
			le.hurt(ModDamageTypes.thanosRealityTear(level, player), DAMAGE_PER_TARGET);
			le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1, true, true, true));
			le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, true, true, true));
			level.sendParticles(ParticleTypes.PORTAL,
					le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(),
					40, 0.4, 0.6, 0.4, 0.3);
		}

		level.sendParticles(ParticleTypes.PORTAL,
				anchor.x, anchor.y + 1.0, anchor.z, 200, RADIUS * 0.3, 1.5, RADIUS * 0.3, 0.6);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL,
				anchor.x, anchor.y + 1.0, anchor.z, 200, RADIUS * 0.3, 1.5, RADIUS * 0.3, 0.4);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				anchor.x, anchor.y + 0.5, anchor.z, 80, RADIUS * 0.4, 0.5, RADIUS * 0.4, 0.0);
		level.sendParticles(ParticleTypes.DRAGON_BREATH,
				anchor.x, anchor.y + 0.5, anchor.z, 60, RADIUS * 0.4, 0.5, RADIUS * 0.4, 0.05);
		level.sendParticles(com.example.superheroes.particle.ModParticles.PURPLE_FLAME,
				anchor.x, anchor.y + 1.0, anchor.z, 220, RADIUS * 0.4, 1.2, RADIUS * 0.4, 0.2);
		level.sendParticles(com.example.superheroes.particle.ModParticles.DARK_STAR,
				anchor.x, anchor.y + 1.0, anchor.z, 160, RADIUS * 0.4, 1.5, RADIUS * 0.4, 0.15);
		level.sendParticles(com.example.superheroes.particle.ModParticles.BLACK_FLAME,
				anchor.x, anchor.y + 1.0, anchor.z, 120, RADIUS * 0.3, 1.0, RADIUS * 0.3, 0.1);
		level.sendParticles(com.example.superheroes.particle.ModParticles.NIGHTFALL,
				anchor.x, anchor.y + 1.0, anchor.z, 180, RADIUS * 0.4, 1.4, RADIUS * 0.4, 0.18);
		level.sendParticles(com.example.superheroes.particle.ModParticles.CHAOS_ORB,
				anchor.x, anchor.y + 1.5, anchor.z, 80, RADIUS * 0.35, 1.6, RADIUS * 0.35, 0.16);
		level.sendParticles(com.example.superheroes.particle.ModParticles.SOUL_SPARK,
				anchor.x, anchor.y + 1.0, anchor.z, 120, RADIUS * 0.4, 1.2, RADIUS * 0.4, 0.12);

		level.playSound(null, anchor.x, anchor.y, anchor.z,
				SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.6f, 0.5f);
		level.playSound(null, anchor.x, anchor.y, anchor.z,
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.4f, 0.6f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
