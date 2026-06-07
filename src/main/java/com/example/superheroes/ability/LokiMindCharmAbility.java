package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LokiMindCharmAbility implements Ability {
	private static final int COOLDOWN_TICKS = 200;
	private static final double RANGE = 24.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.LOKI_MIND_CHARM;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 80f;
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

		LivingEntity target = null;
		double closest = Double.MAX_VALUE;
		AABB scan = new AABB(eye, end).inflate(2.0);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			Vec3 toEntity = le.position().add(0, le.getBbHeight() / 2, 0).subtract(eye);
			double len = toEntity.length();
			if (len < 0.001) continue;
			double dot = toEntity.scale(1.0 / len).dot(dir);
			if (dot < 0.55) continue;
			if (len > RANGE) continue;
			if (len < closest) {
				closest = len;
				target = le;
			}
		}

		int steps = 16;
		for (int i = 0; i < steps; i++) {
			double t = (double) i / steps;
			Vec3 p = eye.add(dir.scale(t * RANGE));
			level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
		}

		if (target instanceof Mob mob) {
			mob.setTarget(null);
			mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 2, true, true, true));
			mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2, true, true, true));
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, true, true, true));
			mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1, true, true, true));
		} else if (target instanceof Player p) {
			p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, true, true));
		}

		if (target != null) {
			level.sendParticles(ParticleTypes.WITCH,
					target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 40, 0.5, 0.5, 0.5, 0.05);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2f, 1.4f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
