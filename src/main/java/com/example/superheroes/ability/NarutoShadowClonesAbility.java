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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NarutoShadowClonesAbility implements Ability {
	private static final int COOLDOWN_TICKS = 240;
	private static final int CLONE_LIFETIME = 120;

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

		for (int i = 0; i < 2; i++) {
			double angle = (i == 0 ? -Math.PI / 4 : Math.PI / 4);
			Vec3 rot = rotate(player.getLookAngle(), angle);
			Vec3 spawn = pos.add(rot.x * 1.5, 0, rot.z * 1.5);
			KageBunshinEntity clone = ModEntities.KAGE_BUNSHIN.create(level);
			if (clone == null) continue;
			clone.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + (i == 0 ? -30f : 30f), 0);
			clone.setOwnerUuid(player.getUUID());
			clone.setLifetimeTicks(CLONE_LIFETIME);
			clone.finalizeSpawn(level, level.getCurrentDifficultyAt(clone.blockPosition()),
					MobSpawnType.MOB_SUMMONED, null);
			level.addFreshEntity(clone);

			level.sendParticles(ModParticles.NARUTO_CLONE_POOF,
					spawn.x, spawn.y + 1.0, spawn.z, 20, 0.4, 0.6, 0.4, 0.05);
			level.sendParticles(ParticleTypes.CLOUD,
					spawn.x, spawn.y + 1.0, spawn.z, 24, 0.4, 0.6, 0.4, 0.05);
			level.sendParticles(ParticleTypes.WHITE_ASH,
					spawn.x, spawn.y + 0.5, spawn.z, 18, 0.5, 0.5, 0.5, 0.05);

			retargetNearbyHostiles(level, player, clone);
		}

		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.ALLAY_HURT, SoundSource.PLAYERS, 1.2f, 0.9f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0f, 1.4f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static Vec3 rotate(Vec3 v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
	}

	private static void retargetNearbyHostiles(ServerLevel level, ServerPlayer owner, KageBunshinEntity clone) {
		AABB area = owner.getBoundingBox().inflate(16.0);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
				m -> m.getTarget() == owner)) {
			mob.setTarget(clone);
		}
	}
}
