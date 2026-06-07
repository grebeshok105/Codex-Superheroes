package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NarutoBijuudamaAbility implements Ability {
	private static final int COOLDOWN_TICKS = 600;
	private static final double RANGE = 32.0;
	private static final double EXPLOSION_RADIUS = 7.0;
	private static final float CENTER_DAMAGE = 40.0f;
	private static final float EDGE_DAMAGE = 16.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.NARUTO_BIJUUDAMA;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 200f;
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
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));

		BlockHitResult bh = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 impact = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;

		double traveled = eye.distanceTo(impact);
		int steps = (int) Math.max(12, traveled * 2);
		for (int i = 0; i < steps; i++) {
			double t = (double) i / steps;
			Vec3 p = eye.add(dir.scale(t * traveled));
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					p.x, p.y, p.z, 2, 0.25, 0.25, 0.25, 0.02);
			level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
					p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.02);
		}

		float centerDamage = CENTER_DAMAGE;
		float edgeDamage = EDGE_DAMAGE;

		AABB aoe = new AABB(
				impact.x - EXPLOSION_RADIUS, impact.y - EXPLOSION_RADIUS, impact.z - EXPLOSION_RADIUS,
				impact.x + EXPLOSION_RADIUS, impact.y + EXPLOSION_RADIUS, impact.z + EXPLOSION_RADIUS);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, aoe,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p2 && p2.getUUID().equals(player.getUUID())))) {
			double dist = le.position().distanceTo(impact);
			float falloff = (float) Math.max(0.0, 1.0 - dist / EXPLOSION_RADIUS);
			float damage = edgeDamage + (centerDamage - edgeDamage) * falloff;
			le.hurt(ModDamageTypes.narutoBijuudama(level, player), damage);
			le.igniteForSeconds(3f);
			Vec3 away = le.position().subtract(impact);
			double horiz = Math.max(0.01, Math.sqrt(away.x * away.x + away.z * away.z));
			le.setDeltaMovement(away.x / horiz * 1.4, 0.7, away.z / horiz * 1.4);
			le.hurtMarked = true;
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
				impact.x, impact.y, impact.z, 6, EXPLOSION_RADIUS * 0.3, EXPLOSION_RADIUS * 0.3, EXPLOSION_RADIUS * 0.3, 0.0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				impact.x, impact.y + 1, impact.z, 200, EXPLOSION_RADIUS * 0.6, EXPLOSION_RADIUS * 0.5, EXPLOSION_RADIUS * 0.6, 0.4);
		level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
				impact.x, impact.y + 1, impact.z, 160, EXPLOSION_RADIUS * 0.6, EXPLOSION_RADIUS * 0.5, EXPLOSION_RADIUS * 0.6, 0.4);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				impact.x, impact.y + 1, impact.z, 120, EXPLOSION_RADIUS * 0.7, EXPLOSION_RADIUS * 0.5, EXPLOSION_RADIUS * 0.7, 0.2);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.6f);
		level.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.4f, 0.5f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
