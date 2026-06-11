package com.example.superheroes.ability;

import com.example.superheroes.network.ThinkMarkS2CPayload;
import com.example.superheroes.physics.RushTerrainBreaker;
import com.example.superheroes.physics.ShockwaveUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * «Think, Mark!» — фирменный захват Омнимена.
 *
 * Фазы:
 *  LIFT  — хватает цель и взмывает с ней вертикально на ~50 блоков (2 секунды);
 *  HOVER — пара секунд зависания: жертва прижата вытянутыми руками, игрок целится;
 *  DASH  — рывок на бешеной скорости в направлении взгляда, цель в руках,
 *          всё на пути проламывается; финал — удар о землю с шоквейвом.
 *
 * Урон умышленно небольшой: это зрелище, а не экзекуция.
 */
public final class OmnimanThinkMarkAbility implements Ability {
	private static final int COOLDOWN_TICKS = 400;
	private static final float COST = 85f;
	private static final double GRAB_RANGE = 6.0;
	private static final int LIFT_TICKS = 40;
	private static final double LIFT_HEIGHT = 50.0;
	private static final int HOVER_TICKS = 50;
	private static final int DASH_MAX_TICKS = 60;
	private static final double DASH_SPEED = 4.2;
	private static final float DASH_TICK_DAMAGE = 1.5f;
	private static final float SLAM_DAMAGE = 6.0f;
	private static final WeakHashMap<UUID, State> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.OMNIMAN_THINK_MARK;
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
		LivingEntity target = findGrabTarget(player);
		if (target == null) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.omniman_think_mark.no_target"), true);
			return false;
		}

		ACTIVE.put(player.getUUID(), new State(target));
		setPose(player, true);

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.4f, 1.3f);
		level.sendParticles(ParticleTypes.EXPLOSION,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				1, 0.0, 0.0, 0.0, 0.0);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		State state = ACTIVE.get(player.getUUID());
		if (state == null) {
			return;
		}
		LivingEntity target = state.target;
		ServerLevel level = player.serverLevel();
		if (target == null || !target.isAlive() || target.isRemoved() || target.level() != level) {
			finish(player, null);
			return;
		}

		state.ticks++;
		player.fallDistance = 0f;
		target.fallDistance = 0f;

		switch (state.phase) {
			case LIFT -> tickLift(player, target, state, level);
			case HOVER -> tickHover(player, target, state, level);
			case DASH -> tickDash(player, target, state, level);
		}
	}

	private static void tickLift(ServerPlayer player, LivingEntity target, State state, ServerLevel level) {
		Vec3 up = new Vec3(0.0, LIFT_HEIGHT / LIFT_TICKS, 0.0);
		applyMotion(player, up);
		holdTarget(player, target);

		Vec3 c = player.position();
		level.sendParticles(ParticleTypes.CLOUD, c.x, c.y - 0.4, c.z, 6, 0.3, 0.2, 0.3, 0.06);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.0, c.z, 3, 0.4, 0.6, 0.4, 0.03);

		if (state.ticks >= LIFT_TICKS) {
			state.phase = Phase.HOVER;
			state.ticks = 0;
			level.playSound(null, c.x, c.y, c.z,
					SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0f, 0.8f);
		}
	}

	private static void tickHover(ServerPlayer player, LivingEntity target, State state, ServerLevel level) {
		applyMotion(player, Vec3.ZERO);
		holdTarget(player, target);

		Vec3 c = player.position();
		if (state.ticks % 10 == 0) {
			level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.2, c.z, 4, 0.8, 0.5, 0.8, 0.01);
		}
		if (state.ticks == HOVER_TICKS / 2) {
			// Драматическая пауза: «Think, Mark!»
			level.playSound(null, c.x, c.y, c.z,
					SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.1f, 1.6f);
		}

		if (state.ticks >= HOVER_TICKS) {
			state.phase = Phase.DASH;
			state.ticks = 0;
			level.playSound(null, c.x, c.y, c.z,
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.7f);
			level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1.0, c.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void tickDash(ServerPlayer player, LivingEntity target, State state, ServerLevel level) {
		Vec3 direction = player.getViewVector(1f).normalize();
		if (direction.lengthSqr() < 1.0e-4) {
			direction = new Vec3(0.0, -0.4, 1.0).normalize();
		}
		applyMotion(player, direction.scale(DASH_SPEED));
		holdTarget(player, target);

		// Небольшой периодический урон цели — телом о воздух и обломки.
		if (state.ticks % 10 == 0) {
			target.hurt(level.damageSources().playerAttack(player), DASH_TICK_DAMAGE);
		}

		Vec3 c = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
		Vec3 wake = c.subtract(direction.scale(1.1));
		level.sendParticles(ParticleTypes.CLOUD, wake.x, wake.y, wake.z, 14, 0.4, 0.35, 0.4, 0.16);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.06);
		level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 8, 0.4, 0.4, 0.4, 0.18);
		if (state.ticks % 8 == 0) {
			level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		boolean hitWall = false;
		if (player.horizontalCollision || player.verticalCollision || player.onGround()) {
			Vec3 contact = player.position().add(direction.scale(0.6)).add(0.0, player.getBbHeight() * 0.5, 0.0);
			int broken = RushTerrainBreaker.breakContact(level, player, contact, direction, 2.4, 120);
			hitWall = broken == 0;
		}

		if (hitWall || state.ticks >= DASH_MAX_TICKS || (state.ticks > 8 && player.onGround())) {
			finish(player, target);
		}
	}

	private static void finish(ServerPlayer player, LivingEntity target) {
		ACTIVE.remove(player.getUUID());
		setPose(player, false);
		ServerLevel level = player.serverLevel();
		Vec3 c = player.position();
		if (target != null && target.isAlive()) {
			target.hurt(level.damageSources().playerAttack(player), SLAM_DAMAGE);
			Vec3 toss = player.getViewVector(1f).normalize().scale(1.4).add(0.0, 0.45, 0.0);
			target.setDeltaMovement(toss);
			target.hurtMarked = true;
		}
		level.playSound(null, c.x, c.y, c.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.7f);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0.0, 0.0, 0.0, 0.0);
		ShockwaveUtil.detonate(player, c, 5.0, 4.0f, true);
	}

	public static void clear(ServerPlayer player) {
		if (ACTIVE.remove(player.getUUID()) != null) {
			setPose(player, false);
		}
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	/** Прижимаем цель к груди перед вытянутыми руками. */
	private static void holdTarget(ServerPlayer player, LivingEntity target) {
		Vec3 look = player.getViewVector(1f).normalize();
		Vec3 hold = player.getEyePosition()
				.add(look.scale(1.15))
				.subtract(0.0, target.getBbHeight() * 0.5, 0.0);
		target.teleportTo(hold.x, hold.y, hold.z);
		target.setDeltaMovement(Vec3.ZERO);
		target.hurtMarked = true;
	}

	private static void applyMotion(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));
	}

	private static void setPose(ServerPlayer player, boolean active) {
		ThinkMarkS2CPayload payload = new ThinkMarkS2CPayload(player.getUUID(), active);
		for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}

	/**
	 * Цель — ближайшее живое существо в конусе взгляда.
	 * Креативных игроков хватать МОЖНО: способность — про зрелище, урон им не пройдёт.
	 */
	private static LivingEntity findGrabTarget(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f).normalize();
		AABB box = player.getBoundingBox().inflate(GRAB_RANGE);
		List<LivingEntity> candidates = player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator());
		LivingEntity best = null;
		double bestScore = 0.55; // минимальный косинус угла (~57° конус)
		for (LivingEntity e : candidates) {
			Vec3 to = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(eye);
			double dist = to.length();
			if (dist < 1.0e-3 || dist > GRAB_RANGE) {
				continue;
			}
			double cos = to.normalize().dot(look);
			double score = cos - dist * 0.02;
			if (cos > 0.55 && score > bestScore) {
				bestScore = score;
				best = e;
			}
		}
		return best;
	}

	private enum Phase {
		LIFT,
		HOVER,
		DASH
	}

	private static final class State {
		final LivingEntity target;
		Phase phase = Phase.LIFT;
		int ticks;

		State(LivingEntity target) {
			this.target = target;
		}
	}
}
