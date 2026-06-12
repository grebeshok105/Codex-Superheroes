package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.ScorpionHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side brain for Scorpion: kunai-chain pulls, Hell Breath channel and
 * the hellish passive (fire immunity + searing aura).
 */
public final class ScorpionController {
	private static final int SPEAR_PULL_TICKS = 20;
	private static final double SPEAR_STOP_DISTANCE = 2.4;
	private static final double SPEAR_MAX_PULL_SPEED = 1.35;
	private static final double SPEAR_MAX_PULL_LIFT = 0.14;

	private static final int BREATH_DURATION_TICKS = 50;
	private static final double BREATH_RANGE = 9.0;
	private static final double BREATH_CONE_DOT = 0.72;
	private static final float BREATH_DAMAGE_PER_HIT = 2.0f;

	private static final int PASSIVE_REFRESH_INTERVAL = 20;

	private record SpearPull(UUID owner, long startedAt) {
	}

	private record Breath(UUID playerId, long endsAt) {
	}

	private static final Map<UUID, SpearPull> SPEAR_PULLS = new HashMap<>();
	private static final Map<UUID, Breath> BREATHS = new HashMap<>();

	private ScorpionController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickSpearPulls(server);
			tickBreaths(server);
			tickPassives(server);
		});
	}

	public static boolean isScorpion(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && ScorpionHero.ID.equals(data.heroId());
	}

	// ---------------------------------------------------------------- spear

	public static void startSpearPull(ServerPlayer player, LivingEntity target) {
		if (player == null || target == null || !target.isAlive()) {
			return;
		}
		SPEAR_PULLS.put(target.getUUID(), new SpearPull(
				player.getUUID(), player.serverLevel().getGameTime()));
		if (tickSpearPull(player, target)) {
			stopSpearPull(target);
			SPEAR_PULLS.remove(target.getUUID());
		}
	}

	private static void tickSpearPulls(MinecraftServer server) {
		if (SPEAR_PULLS.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, SpearPull>> it = SPEAR_PULLS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, SpearPull> entry = it.next();
			SpearPull pull = entry.getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(pull.owner());
			LivingEntity target = owner == null ? null : findLiving(owner.serverLevel(), entry.getKey());
			if (owner == null || target == null || !target.isAlive() || !isScorpion(owner)) {
				it.remove();
				continue;
			}
			long elapsed = owner.serverLevel().getGameTime() - pull.startedAt();
			spawnChainParticles(owner, target);
			if (tickSpearPull(owner, target) || elapsed >= SPEAR_PULL_TICKS) {
				stopSpearPull(target);
				it.remove();
			}
		}
	}

	private static boolean tickSpearPull(ServerPlayer player, LivingEntity target) {
		Vec3 playerPos = player.position();
		Vec3 targetPos = target.position();
		Vec3 horizontal = new Vec3(playerPos.x - targetPos.x, 0.0, playerPos.z - targetPos.z);
		double distance = horizontal.length();
		if (distance <= SPEAR_STOP_DISTANCE) {
			return true;
		}
		Vec3 direction = horizontal.scale(1.0 / distance);
		double speed = Math.min(SPEAR_MAX_PULL_SPEED, (distance - SPEAR_STOP_DISTANCE) * 0.45);
		double lift = Math.min(SPEAR_MAX_PULL_LIFT,
				Math.max(0.0, (playerPos.y - targetPos.y) * 0.08 + 0.04));
		setPullMotion(target, direction.scale(speed).add(0.0, lift, 0.0));
		return false;
	}

	private static void stopSpearPull(LivingEntity target) {
		Vec3 current = target.getDeltaMovement();
		setPullMotion(target, new Vec3(0.0, Math.min(current.y, SPEAR_MAX_PULL_LIFT), 0.0));
	}

	private static void setPullMotion(LivingEntity target, Vec3 motion) {
		target.setDeltaMovement(motion);
		target.hurtMarked = true;
		if (target instanceof ServerPlayer targetPlayer) {
			targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
		}
	}

	private static void spawnChainParticles(ServerPlayer owner, LivingEntity target) {
		ServerLevel level = owner.serverLevel();
		Vec3 from = owner.getEyePosition().add(0.0, -0.25, 0.0);
		Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
		int steps = 10;
		for (int i = 0; i <= steps; i++) {
			Vec3 point = from.lerp(to, i / (double) steps);
			level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.005);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.SMALL_FLAME, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.01);
			}
		}
	}

	private static LivingEntity findLiving(ServerLevel level, UUID id) {
		Entity entity = level.getEntity(id);
		return entity instanceof LivingEntity living ? living : null;
	}

	// --------------------------------------------------------------- breath

	public static void startBreath(ServerPlayer player) {
		BREATHS.put(player.getUUID(), new Breath(
				player.getUUID(), player.serverLevel().getGameTime() + BREATH_DURATION_TICKS));
	}

	public static boolean isBreathing(ServerPlayer player) {
		Breath breath = BREATHS.get(player.getUUID());
		return breath != null && player.serverLevel().getGameTime() < breath.endsAt();
	}

	private static void tickBreaths(MinecraftServer server) {
		if (BREATHS.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Breath>> it = BREATHS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Breath> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive() || !isScorpion(player)
					|| player.serverLevel().getGameTime() >= entry.getValue().endsAt()) {
				it.remove();
				continue;
			}
			tickBreath(player);
		}
	}

	private static void tickBreath(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition().add(0.0, -0.2, 0.0);
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}

		// Flame jet: particles streaming out of the mouth in a widening cone.
		for (int i = 0; i < 6; i++) {
			double t = 0.8 + level.random.nextDouble() * (BREATH_RANGE - 0.8);
			double spread = 0.10 + t * 0.085;
			Vec3 point = eye.add(forward.scale(t));
			level.sendParticles(ParticleTypes.FLAME,
					point.x, point.y, point.z, 2, spread, spread, spread, 0.02);
		}
		if (player.tickCount % 4 == 0) {
			Vec3 far = eye.add(forward.scale(BREATH_RANGE * 0.7));
			level.sendParticles(ParticleTypes.LAVA, far.x, far.y, far.z, 1, 0.5, 0.4, 0.5, 0.0);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, far.x, far.y, far.z, 2, 0.5, 0.4, 0.5, 0.01);
		}
		if (player.tickCount % 10 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 1.1f, 0.75f);
		}
		if (player.tickCount % 8 == 0) {
			ScorpionFx.breath(level, eye.add(forward.scale(0.8)), forward);
		}
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 0, true, false, false));

		if (player.tickCount % 4 != 0) {
			return;
		}
		AABB box = new AABB(eye, eye.add(forward.scale(BREATH_RANGE))).inflate(2.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
				t -> isValidTarget(player, t))) {
			Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toTarget = center.subtract(eye);
			double distance = toTarget.length();
			if (distance < 0.001 || distance > BREATH_RANGE) {
				continue;
			}
			if (toTarget.scale(1.0 / distance).dot(forward) < BREATH_CONE_DOT) {
				continue;
			}
			target.invulnerableTime = 0;
			target.hurt(level.damageSources().playerAttack(player), BREATH_DAMAGE_PER_HIT);
			target.igniteForSeconds(4f);
		}
	}

	// -------------------------------------------------------------- passive

	private static void tickPassives(MinecraftServer server) {
		long tick = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isScorpion(player)) {
				continue;
			}
			if (tick % PASSIVE_REFRESH_INTERVAL == 0) {
				player.addEffect(new MobEffectInstance(
						MobEffects.FIRE_RESISTANCE, 60, 0, true, false, false));
				if (player.isOnFire()) {
					player.clearFire();
				}
			}
			ServerLevel level = player.serverLevel();
			if (tick % 5 == 0) {
				level.sendParticles(ParticleTypes.SMALL_FLAME,
						player.getX(), player.getY() + 0.15, player.getZ(),
						2, 0.30, 0.05, 0.30, 0.01);
			}
		}
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}
}
