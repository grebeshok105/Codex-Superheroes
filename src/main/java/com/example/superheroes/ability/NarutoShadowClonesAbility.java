package com.example.superheroes.ability;

import com.example.superheroes.entity.KageBunshinEntity;
import com.example.superheroes.entity.ModEntities;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class NarutoShadowClonesAbility implements Ability {
	private static final int COOLDOWN_TICKS = 240;
	private static final int CLONE_COUNT = 15;
	private static final double SPAWN_RADIUS = 10.0;
	private static final int CLONE_LIFETIME = 500; // 25 seconds
	private static final int DARKNESS_DURATION = 100; // 5 seconds
	private static final double DARKNESS_RANGE = 15.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.NARUTO_SHADOW_CLONES;
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
		Vec3 pos = player.position();
		ThreadLocalRandom rng = ThreadLocalRandom.current();

		for (int i = 0; i < CLONE_COUNT; i++) {
			double angle = rng.nextDouble() * Math.PI * 2;
			double dist = 1.5 + rng.nextDouble() * (SPAWN_RADIUS - 1.5);
			double spawnX = pos.x + Math.cos(angle) * dist;
			double spawnZ = pos.z + Math.sin(angle) * dist;
			double spawnY = pos.y;

			KageBunshinEntity clone = ModEntities.KAGE_BUNSHIN.create(level);
			if (clone == null) continue;

			float randomYaw = rng.nextFloat() * 360f - 180f;
			clone.moveTo(spawnX, spawnY, spawnZ, randomYaw, 0);
			clone.setOwnerUuid(player.getUUID());
			clone.setLifetimeTicks(CLONE_LIFETIME);
			clone.setCustomNameVisible(false);
			clone.finalizeSpawn(level, level.getCurrentDifficultyAt(clone.blockPosition()),
					MobSpawnType.MOB_SUMMONED, null);
			level.addFreshEntity(clone);

			level.sendParticles(ModParticles.NARUTO_CLONE_POOF,
					spawnX, spawnY + 1.0, spawnZ, 20, 0.4, 0.6, 0.4, 0.05);
			level.sendParticles(ParticleTypes.CLOUD,
					spawnX, spawnY + 1.0, spawnZ, 24, 0.4, 0.6, 0.4, 0.05);
		}

		applyDarknessToEnemies(level, player);
		retargetNearbyHostiles(level, player);

		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.ALLAY_HURT, SoundSource.PLAYERS, 1.2f, 0.9f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0f, 1.4f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void applyDarknessToEnemies(ServerLevel level, ServerPlayer owner) {
		AABB area = owner.getBoundingBox().inflate(DARKNESS_RANGE);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (entity == owner) continue;
			if (entity instanceof Player p && p.getUUID().equals(owner.getUUID())) continue;
			if (entity instanceof KageBunshinEntity clone && owner.getUUID().equals(clone.getOwnerUuid())) continue;
			entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_DURATION, 0, false, false, true));
		}
	}

	private static void retargetNearbyHostiles(ServerLevel level, ServerPlayer owner) {
		AABB area = owner.getBoundingBox().inflate(20.0);
		var clones = level.getEntitiesOfClass(KageBunshinEntity.class, area,
				c -> owner.getUUID().equals(c.getOwnerUuid()));
		if (clones.isEmpty()) return;
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
				m -> m.getTarget() == owner)) {
			mob.setTarget(clones.get(rng.nextInt(clones.size())));
		}
	}
}
