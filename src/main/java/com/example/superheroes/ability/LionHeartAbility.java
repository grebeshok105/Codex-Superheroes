package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class LionHeartAbility implements Ability {
	private static final double PUSH_RADIUS = 4.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.LION_HEART;
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
		return 10f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 4, true, false, true));

		Vec3 origin = player.position();
		AABB area = new AABB(origin, origin).inflate(PUSH_RADIUS);
		List<Entity> nearby = level.getEntities(player, area, e -> e != player && e.isAlive() && !e.isSpectator());
		for (Entity entity : nearby) {
			double dx = entity.getX() - player.getX();
			double dz = entity.getZ() - player.getZ();
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len < 0.001) {
				len = 0.001;
			}
			entity.push(dx / len * 1.5, 0.6, dz / len * 1.5);
			entity.hurtMarked = true;
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 0.9f);
		level.sendParticles(ParticleTypes.FLASH,
				player.getX(), player.getY() + 1.0, player.getZ(),
				1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 1.5, 0.5, 1.5, 0.05);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		if (player.tickCount % 20 == 0 && !player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 4, true, false, true));
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
	}
}
