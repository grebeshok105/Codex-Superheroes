package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
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

import java.util.UUID;
import java.util.WeakHashMap;

public final class CapShieldSlamAbility implements Ability {
	private static final int COOLDOWN_TICKS = 200;
	private static final double RADIUS = 5.0;
	private static final float DAMAGE = 5.0f;
	private static final int MAX_AIR_TICKS = 100;
	private static final int MIN_AIR_TICKS_BEFORE_DETONATE = 4;

	private static final WeakHashMap<UUID, Integer> JUMPING = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.CAP_SHIELD_SLAM;
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
		return !AbilityCooldowns.isOnCooldown(player, getId()) && !JUMPING.containsKey(player.getUUID());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 motion = new Vec3(0, 1.4, 0);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		JUMPING.put(player.getUUID(), 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 1.2f, 1.4f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		Integer airTicks = JUMPING.get(player.getUUID());
		if (airTicks == null) return;
		if (airTicks >= MIN_AIR_TICKS_BEFORE_DETONATE && player.onGround()) {
			detonate(player);
			JUMPING.remove(player.getUUID());
			return;
		}
		if (airTicks >= MAX_AIR_TICKS) {
			JUMPING.remove(player.getUUID());
			return;
		}
		JUMPING.put(player.getUUID(), airTicks + 1);
	}

	private static void detonate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();

		AABB box = new AABB(
				pos.x - RADIUS, pos.y - 1, pos.z - RADIUS,
				pos.x + RADIUS, pos.y + 2, pos.z + RADIUS);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator())) {
			le.hurt(ModDamageTypes.capShieldSlam(level, player), DAMAGE);
			Vec3 away = le.position().subtract(pos);
			double horizDist = Math.sqrt(away.x * away.x + away.z * away.z);
			if (horizDist < 0.01) horizDist = 0.01;
			Vec3 push = new Vec3(away.x / horizDist * 1.6, 0.8, away.z / horizDist * 1.6);
			le.setDeltaMovement(push);
			le.hurtMarked = true;
		}

		level.sendParticles(ModParticles.CAP_SHIELD_SLAM_BURST,
				pos.x, pos.y + 0.1, pos.z, 80, RADIUS * 0.6, 0.4, RADIUS * 0.6, 0.3);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				pos.x, pos.y + 0.1, pos.z, 50, RADIUS * 0.5, 0.3, RADIUS * 0.5, 0.1);
		level.sendParticles(ParticleTypes.EXPLOSION,
				pos.x, pos.y + 0.5, pos.z, 4, RADIUS * 0.3, 0.1, RADIUS * 0.3, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				pos.x, pos.y + 0.4, pos.z, 6, RADIUS * 0.4, 0.1, RADIUS * 0.4, 0.0);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);
	}
}
