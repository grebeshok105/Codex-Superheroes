package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RepulsorAbility implements Ability {
	private static final double RANGE = 40.0;
	private static final float DAMAGE = 8.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REPULSOR;
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
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult bh = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 actualEnd = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(0.6);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, actualEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		if (hit != null) {
			LivingEntity target = (LivingEntity) hit.getEntity();
			target.hurt(ModDamageTypes.repulsor(level, player), DAMAGE);
			actualEnd = hit.getLocation();
			Vec3 push = dir.scale(0.6);
			target.push(push.x, 0.2, push.z);
			target.hurtMarked = true;
		}
		Vec3 hand = eye.add(dir.scale(0.5));
		ModNetworking.broadcastRepulsor(player, hand, actualEnd);

		level.sendParticles(ModParticles.REPULSOR_SPARK,
				hand.x, hand.y, hand.z, 18, 0.25, 0.25, 0.25, 0.05);
		level.sendParticles(ParticleTypes.SMOKE,
				hand.x, hand.y, hand.z, 8, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(ModParticles.REPULSOR_SPARK,
				actualEnd.x, actualEnd.y, actualEnd.z, 14, 0.3, 0.3, 0.3, 0.08);
		level.sendParticles(ParticleTypes.END_ROD,
				actualEnd.x, actualEnd.y, actualEnd.z, 4, 0.2, 0.2, 0.2, 0.05);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 1.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.5f, 1.4f);
		return true;
	}
}
