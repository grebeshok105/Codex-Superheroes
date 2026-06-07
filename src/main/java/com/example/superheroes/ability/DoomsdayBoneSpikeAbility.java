package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class DoomsdayBoneSpikeAbility implements Ability {
	private static final int COOLDOWN_TICKS = 30;
	private static final double RANGE = 30.0;
	private static final float DAMAGE = 18.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.DOOMSDAY_BONE_SPIKE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 40f;
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
		Vec3 origin = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f).normalize();
		Vec3 end = origin.add(dir.scale(RANGE));

		AABB sweep = new AABB(origin, end).inflate(0.6);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, origin, end, sweep,
				e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity);

		Vec3 impactPos = hit != null ? hit.getLocation() : end;

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.4f, 0.5f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.0f, 0.6f);

		int steps = (int) Math.max(8.0, impactPos.subtract(origin).length() * 4.0);
		Vec3 step = impactPos.subtract(origin).scale(1.0 / Math.max(1, steps));
		Vec3 cur = origin;
		for (int i = 0; i < steps; i++) {
			cur = cur.add(step);
			level.sendParticles(ParticleTypes.WHITE_ASH, cur.x, cur.y, cur.z, 2, 0.05, 0.05, 0.05, 0.0);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.CRIT, cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
		}

		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			living.hurt(ModDamageTypes.doomsdayBoneSpike(level, player), DAMAGE);
			living.knockback(0.4, -dir.x, -dir.z);
			living.hurtMarked = true;
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, impactPos.x, impactPos.y, impactPos.z, 8, 0.2, 0.2, 0.2, 0.0);
			level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
		}

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
