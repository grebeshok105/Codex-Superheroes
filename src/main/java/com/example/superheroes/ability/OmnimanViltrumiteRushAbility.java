package com.example.superheroes.ability;

import com.example.superheroes.effect.OmnimanMomentumController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class OmnimanViltrumiteRushAbility implements Ability {
	private static final int COOLDOWN_TICKS = 140;
	private static final int DURATION_TICKS = 12;
	private static final float COST = 72f;
	private static final float MOMENTUM_COST = 25f;
	private static final float BASE_DAMAGE = 26f;
	private static final float BOOSTED_DAMAGE = 34f;
	private static final double BASE_DISTANCE = 18.0;
	private static final double BOOSTED_DISTANCE = 24.0;
	private static final double BASE_KNOCKBACK = 5.2;
	private static final double BOOSTED_KNOCKBACK = 6.6;
	private static final double HIT_SCAN_INFLATE = 1.55;
	private static final WeakHashMap<UUID, ActiveRush> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.OMNIMAN_VILTRUMITE_RUSH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return COST;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId()) && !ACTIVE.containsKey(player.getUUID());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		boolean boosted = consumeMomentum(player);
		Vec3 direction = viewDirection(player);
		double distance = boosted ? BOOSTED_DISTANCE : BASE_DISTANCE;
		Vec3 motion = direction.scale(distance / DURATION_TICKS);

		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));
		ACTIVE.put(player.getUUID(), new ActiveRush(DURATION_TICKS, distance, boosted, new HashSet<>()));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, boosted ? 1.8f : 1.45f, boosted ? 0.55f : 0.68f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, boosted ? 0.9f : 0.65f, boosted ? 1.25f : 1.45f);
		level.sendParticles(ParticleTypes.SONIC_BOOM,
				player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(),
				1, 0.0, 0.0, 0.0, 0.0);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveRush rush = ACTIVE.get(player.getUUID());
		if (rush == null) return;

		ServerLevel level = player.serverLevel();
		Vec3 direction = viewDirection(player);
		Vec3 motion = direction.scale(rush.distance / DURATION_TICKS);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		hitTargets(player, rush, direction);
		sendTrail(level, player, direction, rush.boosted);

		rush.ticksLeft--;
		if (rush.ticksLeft <= 0 || player.horizontalCollision) {
			ACTIVE.remove(player.getUUID());
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, rush.boosted ? 1.35f : 1.05f, 0.75f);
		}
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	private static void hitTargets(ServerPlayer player, ActiveRush rush, Vec3 direction) {
		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(HIT_SCAN_INFLATE);
		List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box, target -> validTarget(player, target));
		for (LivingEntity target : hits) {
			if (!rush.hits.add(target.getUUID())) continue;

			float damage = rush.boosted ? BOOSTED_DAMAGE : BASE_DAMAGE;
			double knockback = rush.boosted ? BOOSTED_KNOCKBACK : BASE_KNOCKBACK;
			target.hurt(level.damageSources().playerAttack(player), damage);
			Vec3 push = direction.scale(knockback).add(0.0, rush.boosted ? 0.75 : 0.6, 0.0);
			target.setDeltaMovement(push);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}

			Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
			level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
					rush.boosted ? 3 : 2, 0.18, 0.18, 0.18, 0.0);
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y, center.z,
					rush.boosted ? 24 : 18, 0.35, 0.35, 0.35, 0.0);
			level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
					rush.boosted ? 28 : 20, 0.35, 0.35, 0.35, 0.22);
			level.playSound(null, center.x, center.y, center.z,
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, rush.boosted ? 1.15f : 0.9f, 0.85f);
			level.playSound(null, center.x, center.y, center.z,
					SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, rush.boosted ? 1.35f : 1.1f, 0.55f);
		}
	}

	private static boolean validTarget(ServerPlayer player, LivingEntity target) {
		return target != player
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player targetPlayer && targetPlayer.isCreative());
	}

	private static void sendTrail(ServerLevel level, ServerPlayer player, Vec3 direction, boolean boosted) {
		Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
		Vec3 wake = center.subtract(direction.scale(0.85));
		level.sendParticles(ParticleTypes.CLOUD, wake.x, wake.y, wake.z,
				boosted ? 10 : 7, 0.32, 0.25, 0.32, boosted ? 0.12 : 0.08);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
				boosted ? 5 : 3, 0.24, 0.22, 0.24, 0.04);
		if (boosted) {
			level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
					1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static Vec3 viewDirection(ServerPlayer player) {
		Vec3 direction = player.getViewVector(1f).normalize();
		if (direction.lengthSqr() < 1.0e-4) {
			return new Vec3(0.0, 0.0, 1.0);
		}
		return direction;
	}

	private static boolean consumeMomentum(ServerPlayer player) {
		return OmnimanMomentumController.consume(player, MOMENTUM_COST);
	}

	private static final class ActiveRush {
		int ticksLeft;
		final double distance;
		final boolean boosted;
		final Set<UUID> hits;

		ActiveRush(int ticksLeft, double distance, boolean boosted, Set<UUID> hits) {
			this.ticksLeft = ticksLeft;
			this.distance = distance;
			this.boosted = boosted;
			this.hits = hits;
		}
	}
}
