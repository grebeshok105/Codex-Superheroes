package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.effect.ReinhardWorthyOpponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ReinhardSwordWaveAbility implements Ability {
	private static final double RANGE = 25.0;
	private static final double WIDTH = 2.5;
	private static final float DAMAGE_MOB = 9.0f;
	private static final float DAMAGE_WORTHY = 18.0f;
	private static final int COOLDOWN_TICKS = 60;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_SWORD_WAVE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 250f;
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
		Vec3 origin = player.position().add(0, 1.0, 0);
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = origin.add(dir.scale(RANGE));

		AABB box = new AABB(origin, end).inflate(WIDTH);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e != player && !e.isSpectator());

		DamageSource src = level.damageSources().playerAttack(player);
		for (LivingEntity target : targets) {
			boolean worthy = ReinhardWorthyOpponent.isWorthy(target);
			float dmg = worthy ? DAMAGE_WORTHY : DAMAGE_MOB;
			target.hurt(src, dmg);
			Vec3 push = dir.scale(1.5);
			target.push(push.x, 0.4, push.z);
			target.hurtMarked = true;
		}

		int steps = 30;
		for (int i = 0; i < steps; i++) {
			double t = i / (double) steps;
			Vec3 p = origin.add(dir.scale(t * RANGE));
			double offset = Math.sin(t * Math.PI) * WIDTH;
			Vec3 left = dir.cross(new Vec3(0, 1, 0)).normalize().scale(offset);
			level.sendParticles(ParticleTypes.END_ROD,
					p.x + left.x, p.y + left.y, p.z + left.z, 1, 0.05, 0.05, 0.05, 0.0);
			level.sendParticles(ParticleTypes.END_ROD,
					p.x - left.x, p.y - left.y, p.z - left.z, 1, 0.05, 0.05, 0.05, 0.0);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
		}
		level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6f, 0.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.4f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
