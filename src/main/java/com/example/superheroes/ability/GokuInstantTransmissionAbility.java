package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class GokuInstantTransmissionAbility implements Ability {
	private static final int COOLDOWN_TICKS = 160;
	private static final double SEARCH_RANGE = 30.0;
	private static final float STRIKE_DAMAGE = 8.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GOKU_INSTANT_TRANSMISSION;
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
		AABB box = player.getBoundingBox().inflate(SEARCH_RANGE);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator() && player.distanceTo(e) <= SEARCH_RANGE);
		candidates.sort(Comparator.comparingDouble(player::distanceTo));
		LivingEntity target = candidates.isEmpty() ? null : candidates.get(0);

		Vec3 origin = player.position();
		Vec3 dest;
		boolean strike = false;
		if (target != null) {
			Vec3 back = target.position().subtract(target.getLookAngle().scale(1.5));
			dest = new Vec3(back.x, target.getY(), back.z);
			strike = true;
		} else {
			Vec3 dir = player.getViewVector(1f);
			dest = origin.add(dir.scale(12.0));
		}

		spawnParticles(level, origin);
		player.connection.teleport(dest.x, dest.y, dest.z, player.getYRot(), player.getXRot(), Set.of());
		spawnParticles(level, dest);

		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.6f);
		level.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.6f);

		if (strike && target != null) {
			target.hurt(ModDamageTypes.gokuInstantStrike(level, player), STRIKE_DAMAGE);
			Vec3 push = player.getLookAngle().scale(0.3);
			target.push(push.x, 0.2, push.z);
			level.playSound(null, dest.x, dest.y, dest.z,
					SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 1.4f);
		}

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void spawnParticles(ServerLevel level, Vec3 pos) {
		level.sendParticles(ParticleTypes.PORTAL,
				pos.x, pos.y + 1.0, pos.z, 40, 0.4, 0.8, 0.4, 0.5);
		level.sendParticles(ModParticles.GOKU_KI_AURA,
				pos.x, pos.y + 1.0, pos.z, 14, 0.4, 0.6, 0.4, 0.05);
		level.sendParticles(ParticleTypes.END_ROD,
				pos.x, pos.y + 1.0, pos.z, 10, 0.3, 0.6, 0.3, 0.05);
	}
}
