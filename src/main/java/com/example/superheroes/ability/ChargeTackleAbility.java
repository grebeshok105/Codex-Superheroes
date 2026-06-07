package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class ChargeTackleAbility implements Ability {
	private static final int COOLDOWN_TICKS = 240;
	private static final int DURATION_TICKS = 18;
	private static final float DAMAGE = 30f;
	private static final double DISTANCE = 20.0;
	private static final double KNOCKBACK = 5.0;

	private static final WeakHashMap<UUID, ActiveCharge> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.DOOMSDAY_CHARGE_TACKLE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 60f;
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
		Vec3 look = player.getLookAngle().normalize();
		Vec3 horiz = new Vec3(look.x, 0, look.z).normalize();

		double speed = DISTANCE / (double) DURATION_TICKS;
		Vec3 vel = horiz.scale(speed);
		Vec3 motion = new Vec3(vel.x, 0.05, vel.z);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		ACTIVE.put(player.getUUID(), new ActiveCharge(DURATION_TICKS, horiz, new HashSet<>()));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				com.example.superheroes.sound.ModSounds.DOOMSDAY_ROAR, SoundSource.PLAYERS, 1.6f, 0.95f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveCharge ac = ACTIVE.get(player.getUUID());
		if (ac == null) return;

		Vec3 v = ac.dir.scale(20.0 / (double) DURATION_TICKS);
		Vec3 motion = new Vec3(v.x, Math.max(player.getDeltaMovement().y, -0.05), v.z);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

		ServerLevel level = player.serverLevel();
		AABB box = player.getBoundingBox().inflate(1.5);
		List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !ac.hits.contains(e.getUUID()));
		for (LivingEntity e : hits) {
			ac.hits.add(e.getUUID());
			e.hurt(ModDamageTypes.doomsdayChargeTackle(level, player), DAMAGE);
			Vec3 kb = ac.dir.scale(KNOCKBACK).add(0, 0.6, 0);
			e.push(kb.x, kb.y, kb.z);
			e.hurtMarked = true;
		}

		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				player.getX(), player.getY() + 0.5, player.getZ(),
				6, 0.4, 0.3, 0.4, 0.05);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				player.getX(), player.getY() + 0.5, player.getZ(),
				3, 0.3, 0.3, 0.3, 0.02);

		ac.ticksLeft--;
		if (ac.ticksLeft <= 0) {
			ACTIVE.remove(player.getUUID());
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.7f);
		}
	}

	public static boolean isCharging(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	private static final class ActiveCharge {
		int ticksLeft;
		final Vec3 dir;
		final Set<UUID> hits;

		ActiveCharge(int ticksLeft, Vec3 dir, Set<UUID> hits) {
			this.ticksLeft = ticksLeft;
			this.dir = dir;
			this.hits = hits;
		}
	}
}
