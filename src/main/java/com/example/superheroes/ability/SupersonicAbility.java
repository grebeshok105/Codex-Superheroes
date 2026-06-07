package com.example.superheroes.ability;

import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class SupersonicAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.SUPERSONIC;
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
		return 6.0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		Abilities a = player.getAbilities();
		a.mayfly = true;
		a.flying = true;
		player.onUpdateAbilities();
		player.startFallFlying();
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 1.6f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		if (!player.isFallFlying()) {
			player.startFallFlying();
		}
		Abilities a = player.getAbilities();
		if (!a.flying) {
			a.flying = true;
			player.onUpdateAbilities();
		}
		player.fallDistance = 0f;

		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();
		Vec3 dir = player.getLookAngle();
		Vec3 back = pos.add(dir.reverse().scale(0.5));
		level.sendParticles(ParticleTypes.FLAME,
				back.x, back.y + 0.3, back.z, 6, 0.15, 0.15, 0.15, 0.02);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				back.x, back.y + 0.3, back.z, 2, 0.2, 0.2, 0.2, 0.01);
		level.sendParticles(ModParticles.LASER_SPARK,
				back.x, back.y + 0.3, back.z, 3, 0.1, 0.1, 0.1, 0.0);

		if (player.tickCount % 6 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.2f, 1.6f);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		Abilities a = player.getAbilities();
		a.flying = false;
		if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
			a.mayfly = false;
		}
		player.onUpdateAbilities();
		player.stopFallFlying();
	}
}
