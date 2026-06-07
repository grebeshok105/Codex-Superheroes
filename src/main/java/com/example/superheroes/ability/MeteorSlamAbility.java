package com.example.superheroes.ability;

import com.example.superheroes.physics.ShockwaveUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.WeakHashMap;

public final class MeteorSlamAbility implements Ability {
	private static final int COOLDOWN_TICKS = 220;
	private static final int MAX_AIR_TICKS = 70;
	private static final int MIN_AIR_TICKS_BEFORE_DETONATE = 4;
	private static final double RADIUS = 6.2;
	private static final float GROUNDED_DAMAGE = 20f;
	private static final float AIRBORNE_DAMAGE = 34f;
	private static final WeakHashMap<UUID, SlamState> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.METEOR_SLAM;
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
		return !AbilityCooldowns.isOnCooldown(player, getId()) && !ACTIVE.containsKey(player.getUUID());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		boolean airborne = !player.onGround();
		Vec3 motion = airborne ? new Vec3(0, -1.85, 0) : new Vec3(0, 1.25, 0);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));
		ACTIVE.put(player.getUUID(), new SlamState(0, airborne));
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.2f, 0.75f);
		level.sendParticles(ParticleTypes.CLOUD,
				player.getX(), player.getY() + 0.2, player.getZ(),
				24, 0.45, 0.25, 0.45, 0.12);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		SlamState state = ACTIVE.get(player.getUUID());
		if (state == null) return;

		if (state.ticks >= MIN_AIR_TICKS_BEFORE_DETONATE && (player.onGround() || player.verticalCollision)) {
			detonate(player, state.airborne);
			ACTIVE.remove(player.getUUID());
			return;
		}
		if (state.ticks >= MAX_AIR_TICKS) {
			ACTIVE.remove(player.getUUID());
			return;
		}
		if (state.airborne && !player.onGround()) {
			Vec3 current = player.getDeltaMovement();
			Vec3 motion = new Vec3(current.x * 0.35, -1.85, current.z * 0.35);
			player.setDeltaMovement(motion);
			player.hurtMarked = true;
			player.fallDistance = 0f;
			player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));
		}
		player.serverLevel().sendParticles(ParticleTypes.SMOKE,
				player.getX(), player.getY() + 0.7, player.getZ(),
				6, 0.25, 0.25, 0.25, 0.04);
		state.ticks++;
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	private static void detonate(ServerPlayer player, boolean airborne) {
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();
		float damage = airborne ? AIRBORNE_DAMAGE : GROUNDED_DAMAGE;
		double radius = airborne ? RADIUS + 1.2 : RADIUS;
		ShockwaveUtil.detonate(player, pos, radius, damage, false);
		level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 0.7, pos.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				pos.x, pos.y + 0.25, pos.z,
				48, radius * 0.45, 0.25, radius * 0.45, 0.18);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, airborne ? 1.3f : 0.8f, 0.9f);
	}

	private static final class SlamState {
		int ticks;
		final boolean airborne;

		SlamState(int ticks, boolean airborne) {
			this.ticks = ticks;
			this.airborne = airborne;
		}
	}
}
