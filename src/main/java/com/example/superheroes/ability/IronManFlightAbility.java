package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.transform.HeroData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class IronManFlightAbility implements Ability {
	public static final float ENERGY_FLOOR = 100f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_FLIGHT;
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
		return 0f;
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
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.6f, 1.4f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		if (!player.isFallFlying()) {
			player.startFallFlying();
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.energy() < ENERGY_FLOOR) {
			HeroData updated = data.withResources(ENERGY_FLOOR, data.mana());
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncResources(player, updated);
		}
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();
		Vec3 look = player.getLookAngle();
		Vec3 trail = pos.add(look.reverse().scale(0.4)).add(0, 0.2, 0);
		if (player.tickCount % 2 == 0) {
			level.sendParticles(ModParticles.TRANSFORM_SPARK,
					trail.x, trail.y, trail.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		level.sendParticles(ParticleTypes.FLAME,
				pos.x, pos.y - 0.2, pos.z, 1, 0.1, 0.0, 0.1, 0.005);
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
