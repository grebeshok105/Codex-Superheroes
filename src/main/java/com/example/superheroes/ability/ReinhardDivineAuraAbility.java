package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ReinhardDivineAuraAbility implements Ability {
	private static final double RADIUS = 7.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_DIVINE_AURA;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 4f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(),
				24, 0.4, 0.6, 0.4, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (player.tickCount % 20 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false, true));
		}
		if (player.tickCount % 10 == 0) {
			AABB box = player.getBoundingBox().inflate(RADIUS);
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
					e -> e.isAlive() && e != player && !e.isSpectator()
							&& e.distanceTo(player) <= RADIUS);
			for (LivingEntity target : targets) {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, false, false));
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false, false));
			}
		}
		if (player.tickCount % 6 == 0) {
			for (int i = 0; i < 6; i++) {
				double a = (player.tickCount * 0.15 + i * Math.PI / 3.0) % (Math.PI * 2);
				double rx = Math.cos(a) * RADIUS * 0.7;
				double rz = Math.sin(a) * RADIUS * 0.7;
				level.sendParticles(ParticleTypes.END_ROD,
						player.getX() + rx, player.getY() + 0.4, player.getZ() + rz,
						1, 0.05, 0.4, 0.05, 0.01);
			}
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.SMOKE,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.4, 0.4, 0.02);
	}
}
