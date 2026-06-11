package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.FlightController;
import com.example.superheroes.physics.RushTerrainBreaker;
import com.example.superheroes.transform.HeroData;
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

public final class ViltrumiteChargeAbility implements Ability {
	private static final int COOLDOWN_TICKS = 160;
	private static final int DURATION_TICKS = 4;
	private static final float DAMAGE = 28f;
	private static final double DISTANCE = 22.0;
	private static final double FLIGHT_DISTANCE_MULTIPLIER = 2.5;
	private static final double KNOCKBACK = 4.8;
	private static final WeakHashMap<UUID, ActiveCharge> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.VILTRUMITE_CHARGE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 70f;
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
		Vec3 look = player.getLookAngle().normalize();
		if (look.lengthSqr() < 1.0e-4) {
			look = new Vec3(0, 0, 1);
		}
		double distance = effectiveDistance(player);
		double speed = distance / DURATION_TICKS;
		Vec3 motion = look.scale(speed);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		ACTIVE.put(player.getUUID(), new ActiveCharge(DURATION_TICKS, look, distance, new HashSet<>()));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, distance > DISTANCE ? 1.85f : 1.5f, distance > DISTANCE ? 0.52f : 0.65f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.45f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveCharge charge = ACTIVE.get(player.getUUID());
		if (charge == null) return;

		Vec3 motion = charge.direction.scale(charge.distance / DURATION_TICKS);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(1.4);
		List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()));
		for (LivingEntity hit : hits) {
			if (!charge.hits.add(hit.getUUID())) continue;
			hit.hurt(player.damageSources().playerAttack(player), DAMAGE);
			Vec3 push = charge.direction.scale(KNOCKBACK).add(0, 0.45, 0);
			hit.push(push.x, push.y, push.z);
			hit.hurtMarked = true;
			level.sendParticles(ParticleTypes.CRIT,
					hit.getX(), hit.getY() + hit.getBbHeight() * 0.5, hit.getZ(),
					18, 0.35, 0.35, 0.35, 0.18);
			level.playSound(null, hit.getX(), hit.getY(), hit.getZ(),
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
		}

		level.sendParticles(ParticleTypes.CLOUD,
				player.getX(), player.getY() + 0.8, player.getZ(),
				charge.distance > DISTANCE ? 9 : 5, 0.25, 0.25, 0.25, charge.distance > DISTANCE ? 0.08 : 0.04);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 0.9, player.getZ(),
				charge.distance > DISTANCE ? 4 : 2, 0.18, 0.18, 0.18, 0.03);

		charge.ticksLeft--;
		if (player.horizontalCollision || player.verticalCollision || player.onGround()) {
			Vec3 contact = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
			int broken = RushTerrainBreaker.breakContact(level, player, contact, charge.direction, 2.2, 90);
			if (broken == 0 && player.horizontalCollision) {
				// Непробиваемая стена — рывок гасится.
				ACTIVE.remove(player.getUUID());
				return;
			}
		}
		if (charge.ticksLeft <= 0) {
			ACTIVE.remove(player.getUUID());
		}
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	private static double effectiveDistance(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		boolean flying = FlightController.isFlightActive(data)
				&& (!player.onGround() || player.getAbilities().flying || player.isFallFlying());
		return flying ? DISTANCE * FLIGHT_DISTANCE_MULTIPLIER : DISTANCE;
	}

	private static final class ActiveCharge {
		int ticksLeft;
		final Vec3 direction;
		final double distance;
		final Set<UUID> hits;

		ActiveCharge(int ticksLeft, Vec3 direction, double distance, Set<UUID> hits) {
			this.ticksLeft = ticksLeft;
			this.direction = direction;
			this.distance = distance;
			this.hits = hits;
		}
	}
}
