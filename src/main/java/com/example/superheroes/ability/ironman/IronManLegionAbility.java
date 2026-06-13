package com.example.superheroes.ability.ironman;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.entity.IronLegionDroneEntity;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class IronManLegionAbility implements Ability {
	private static final int COOLDOWN_TICKS = 1200; // 60 seconds

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_LEGION;
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
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		int currentSuit = player.getAttachedOrCreate(ModAttachments.SUIT_VARIANT);
		List<IronManSuitVariant> variants = IronManSuitVariant.legionVariants(currentSuit);
		ThreadLocalRandom rng = ThreadLocalRandom.current();

		for (int i = 0; i < variants.size(); i++) {
			IronManSuitVariant variant = variants.get(i);
			IronLegionDroneEntity drone = ModEntities.IRON_LEGION_DRONE.create(level);
			if (drone == null) continue;

			double angle = ((double) i / variants.size()) * Math.PI * 2;
			double dist = 3.0 + rng.nextDouble() * 2.0;
			double spawnX = player.getX() + Math.cos(angle) * dist;
			double spawnZ = player.getZ() + Math.sin(angle) * dist;
			double spawnY = player.getY() + 6.0 + rng.nextDouble() * 3.0; // hover in view, descend to fight

			drone.moveTo(spawnX, spawnY, spawnZ, rng.nextFloat() * 360f - 180f, 0);
			drone.setSuitVariant(variant.index());
			drone.setOwnerUuid(player.getUUID());
			drone.setCustomNameVisible(false);
			drone.finalizeSpawn(level, level.getCurrentDifficultyAt(drone.blockPosition()),
					MobSpawnType.MOB_SUMMONED, null);
			level.addFreshEntity(drone);

			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					spawnX, player.getY() + 2.0, spawnZ, 15, 0.3, 0.5, 0.3, 0.08);
		}

		// Голос Джарвиса привязан к самому Железному Человеку (entity-bound:
		// сервер шлёт ClientboundSoundEntityPacket → звук летит вместе с игроком,
		// а не висит в точке запуска) и слышен всем игрокам вокруг.
		level.playSound(null, player, ModSounds.IRONMAN_JARVIS_LEGION_LAUNCH,
				SoundSource.PLAYERS, 2.0f, 1.0f);

		// [JARVIS] сообщение о запуске легиона в чат убрано по запросу.

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
