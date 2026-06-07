package com.example.superheroes.ability;

import com.example.superheroes.effect.GreedCageController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GreedsEmbraceAbility implements Ability {
	private static final int COOLDOWN_TICKS = 1200;
	private static final double AIM_RANGE = 40.0;
	private static final double GATHER_RADIUS = 20.0;
	private static final float DAMAGE = 35.0f;
	private static final double LAUNCH_VELOCITY_Y = 4.0;
	private static final int CAGE_TICKS = 120;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GREEDS_EMBRACE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 700f;
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
		Vec3 farEnd = eye.add(dir.scale(AIM_RANGE));

		BlockHitResult hit = level.clip(new ClipContext(
				eye, farEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 anchor = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : farEnd;
		anchor = new Vec3(anchor.x, anchor.y + 0.1, anchor.z);

		AABB gather = new AABB(
				anchor.x - GATHER_RADIUS, anchor.y - GATHER_RADIUS, anchor.z - GATHER_RADIUS,
				anchor.x + GATHER_RADIUS, anchor.y + GATHER_RADIUS, anchor.z + GATHER_RADIUS);
		final Vec3 anchorFinal = anchor;
		var targets = level.getEntitiesOfClass(LivingEntity.class, gather, e -> {
			if (!e.isAlive()) return false;
			if (e == player) return false;
			if (e instanceof Player p && p.getUUID().equals(player.getUUID())) return false;
			return e.position().distanceToSqr(anchorFinal) <= GATHER_RADIUS * GATHER_RADIUS;
		});

		DamageSource src = level.damageSources().playerAttack(player);
		for (LivingEntity le : targets) {
			le.hurt(src, DAMAGE);
			le.fallDistance = 0;
			le.setDeltaMovement(0, LAUNCH_VELOCITY_Y, 0);
			le.hurtMarked = true;
			le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CAGE_TICKS + 40, 1, false, true, true));
		}

		level.sendParticles(ParticleTypes.PORTAL, anchor.x, anchor.y + 1.0, anchor.z, 200, 2.5, 2.0, 2.5, 1.2);
		level.sendParticles(ParticleTypes.FLASH, anchor.x, anchor.y + 1.0, anchor.z, 4, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SONIC_BOOM, anchor.x, anchor.y + 1.0, anchor.z, 1, 0, 0, 0, 0);

		level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 1.4f);

		GreedCageController.create(level, anchor, targets, CAGE_TICKS);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
