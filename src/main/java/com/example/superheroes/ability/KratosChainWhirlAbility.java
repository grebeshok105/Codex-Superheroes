package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
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

public final class KratosChainWhirlAbility implements Ability {
	private static final double RADIUS = 4.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KRATOS_CHAIN_WHIRL;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 30f;
	}

	@Override
	public float costPerTick() {
		return 3.7f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.4f, 0.6f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 center = player.position().add(0, 1, 0);

		double angle = (player.tickCount % 30) / 30.0 * Math.PI * 2;
		for (int i = 0; i < 16; i++) {
			double a = angle + i * (Math.PI / 8);
			double rx = Math.cos(a) * RADIUS;
			double rz = Math.sin(a) * RADIUS;
			level.sendParticles(ModParticles.PURPLE_FLAME,
					center.x + rx, center.y + 0.4, center.z + rz,
					1, 0.05, 0.05, 0.05, 0.0);
			level.sendParticles(ModParticles.BLACK_FLAME,
					center.x + rx, center.y + 1.0, center.z + rz,
					1, 0.05, 0.08, 0.05, 0.0);
			if (i % 4 == 0) {
				level.sendParticles(ModParticles.DARK_STAR,
						center.x + rx, center.y + 0.7, center.z + rz,
						1, 0.1, 0.1, 0.1, 0.02);
			}
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
						center.x + rx * 0.7, center.y + 0.2, center.z + rz * 0.7,
						1, 0.04, 0.04, 0.04, 0.0);
			}
		}

		if (player.tickCount % 5 == 0) {
			AABB aoe = new AABB(
					center.x - RADIUS, center.y - 1.5, center.z - RADIUS,
					center.x + RADIUS, center.y + 1.5, center.z + RADIUS);
			for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, aoe,
					e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
				if (le.position().distanceTo(center) > RADIUS) continue;
				le.hurt(ModDamageTypes.kratosBlade(level, player), 9.0f);
				le.igniteForSeconds(2f);
				le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
				Vec3 push = le.position().subtract(center).normalize().scale(0.6);
				le.setDeltaMovement(le.getDeltaMovement().add(push.x, 0.1, push.z));
				le.hurtMarked = true;
				level.sendParticles(ModParticles.SPARKS, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(),
						6, 0.2, 0.2, 0.2, 0.05);
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.7f, 1.4f);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.4f, 1.6f);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
	}
}
