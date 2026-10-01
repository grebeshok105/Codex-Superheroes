package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lion's Heart — the reworked "void" absolute defense (rework Task 4).
 *
 * <p>The shield exists only from the authored fire tick (cast {@code fire=14},
 * {@code castUntil=32} — the {@code lion_heart_activation} clip's trigger frame):
 * {@link #activate} runs as the cast's {@code onFire} and arms the free window at
 * {@code now + RegulusHearts.heartWindowTicks(player)}. While a window entry exists
 * the player is blocking:
 *
 * <ul>
 *   <li>the single {@code ALLOW_DAMAGE} arbiter voids every damage source that is not
 *       an internal true-cost type ({@link RegulusDamageTypes#isInternal}) — no
 *       Resistance effect is involved;</li>
 *   <li>food exhaustion is cancelled ({@code Player#causeFoodExhaustion} mixin →
 *       {@code Hero.blocksExhaustionWhile} → here);</li>
 *   <li>every {@link Projectile} inside 4 blocks is frozen each tick — velocity zeroed
 *       and a {@code NO_GRAVITY} control lock held by the owner; leaving the radius
 *       releases that owner's ref only (never {@code releaseOwnedBy}, which would
 *       strip the owner's unrelated locks on the same entity).</li>
 * </ul>
 *
 * <p>Past the window the overheat ramp burns the owner: {@code 0.75f} every 10 ticks
 * plus {@code 0.25f} per whole 40 ticks of overheat, synced to the client through
 * {@link RegulusHearts#setOverheatTicks}. At hp ≤ 4 the shield is forced off and the
 * 600-tick cooldown arms.
 *
 * <p>Both session maps use {@code ClearOn}-empty manual lifecycle: the frozen set must
 * release its per-victim locks inside {@link #shutdown} BEFORE the set goes, and map
 * ClearOn hooks could clear it first. {@link #deactivate} (the ability-driven path)
 * additionally runs the shockwave push; bare {@link #shutdown} (leave/death/
 * hero-clear) only releases state.
 */
public final class LionHeartController {
	private static final ResourceLocation REGULUS_ID = ModId.of("regulus");
	private static final ResourceLocation LION_HEART_ID = ModId.of("lion_heart");

	private static final double FREEZE_RADIUS = 4.0;
	private static final double PUSH_RADIUS = 4.0;
	private static final float OVERHEAT_BASE_DAMAGE = 0.75f;
	private static final int OVERHEAT_PERIOD_TICKS = 10;
	private static final float OVERHEAT_RAMP_STEP = 0.25f;
	private static final int OVERHEAT_RAMP_PERIOD_TICKS = 40;
	private static final float FORCE_OFF_HEALTH = 4.0f;
	private static final int FORCE_OFF_COOLDOWN_TICKS = 600;

	/** owner → free-window deadline (level gameTime); an existing entry means the shield is up. */
	private static final OwnedSessionMap<UUID, Long> WINDOW_DEADLINES = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));
	/** owner → frozen projectile ids; manual clear so locks release inside {@link #shutdown}. */
	private static final OwnedSessionMap<UUID, Set<UUID>> FROZEN_PROJECTILES = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private LionHeartController() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.ticks().hero(REGULUS_ID, LionHeartController::tickPlayer);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !isBlocking(player)) {
				return true;
			}
			// The void blocks everything except internal true-cost damage — matched by
			// ResourceKey, not instanceof, so types Task 8 has not registered yet pass too.
			return RegulusDamageTypes.isInternal(source);
		});

		ctx.lifecycle().onLeave(LionHeartController::shutdown);
		ctx.lifecycle().onDeath(LionHeartController::shutdown);
		ctx.lifecycle().onHeroClear(LionHeartController::shutdown);
		ctx.lifecycle().onServerStopped(server -> resetAll());
	}

	/** The shield is up — the cast's authored fire tick already happened. */
	public static boolean isBlocking(ServerPlayer player) {
		return WINDOW_DEADLINES.containsKey(player.getUUID());
	}

	/** "Lion heart active" for HUD-style readers: windup or shield-up alike. */
	public static boolean isActive(ServerPlayer player) {
		return RegulusCastState.isCasting(player, LION_HEART_ID) || isBlocking(player);
	}

	/** Cast {@code onFire}: the shield rises and the free window arms from the live heart count. */
	public static void activate(ServerPlayer player) {
		long deadline = player.level().getGameTime() + RegulusHearts.heartWindowTicks(player);
		WINDOW_DEADLINES.put(player.getUUID(), player.getUUID(), deadline);
		RegulusHearts.setOverheatTicks(player, 0);
		cleanseNegativeEffects(player);

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
		level.sendParticles(ParticleTypes.FLASH,
				player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(), 40, 1.5, 0.5, 1.5, 0.05);
	}

	/**
	 * The per-tick drain, run from the ability's {@code onTickActive} because
	 * {@code costPerTick()} is parameterless and cannot read the shield state:
	 * nothing is charged until the authored fire tick raised the shield.
	 */
	public static void drainActive(ServerPlayer player, float amount) {
		if (!isBlocking(player)) {
			return;
		}
		if (ResourceController.charge(player, LION_HEART_ID, amount) == null) {
			AbilityRouter.deactivate(player, LION_HEART_ID);
		}
	}

	/** Toggle-off / forced-off path: the shockwave push, then full teardown. */
	public static void deactivate(ServerPlayer player) {
		if (WINDOW_DEADLINES.containsKey(player.getUUID())) {
			pushNearby(player, FROZEN_PROJECTILES.get(player.getUUID()));
		}
		shutdown(player);
	}

	/** Every teardown — window, frozen set, overheat counter. Safe to repeat. */
	public static void shutdown(ServerPlayer player) {
		WINDOW_DEADLINES.remove(player.getUUID());
		releaseFrozen(player);
		// Hearts' own drop hook already cleared SYNC on leave/death/clear — reset only
		// a live view instead of recreating a stale entry for a departing player.
		if (RegulusHearts.syncedOverheatTicks(player) >= 0) {
			RegulusHearts.setOverheatTicks(player, 0);
		}
	}

	private static void resetAll() {
		WINDOW_DEADLINES.clear();
		FROZEN_PROJECTILES.clear();
	}

	private static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		Long deadline = WINDOW_DEADLINES.get(player.getUUID());
		if (deadline == null) {
			return;
		}
		freezeProjectiles(player);
		if (player.getHealth() <= FORCE_OFF_HEALTH) {
			forceOff(player);
			return;
		}
		long over = player.level().getGameTime() - deadline;
		if (over <= 0) {
			return;
		}
		RegulusHearts.setOverheatTicks(player, over > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) over);
		if (over % OVERHEAT_PERIOD_TICKS == 0L) {
			float amount = OVERHEAT_BASE_DAMAGE
					+ OVERHEAT_RAMP_STEP * (float) (over / OVERHEAT_RAMP_PERIOD_TICKS);
			player.hurt(RegulusDamageTypes.lionHeartOverheat(player.serverLevel()), amount);
			if (player.getHealth() <= FORCE_OFF_HEALTH) {
				forceOff(player);
			}
		}
	}

	/** hp≤4 kills the shield: deactivate (push + teardown) and arm the 600t cooldown. */
	private static void forceOff(ServerPlayer player) {
		AbilityRouter.deactivate(player, LION_HEART_ID);
		AbilityCooldowns.setCooldownTicks(player, LION_HEART_ID, FORCE_OFF_COOLDOWN_TICKS);
	}

	/**
	 * Freeze-or-release scan: projectiles inside the radius get a zeroed velocity and a
	 * NO_GRAVITY ref owned by this player; entries that left the radius, despawned or
	 * died release just this owner's ref.
	 */
	private static void freezeProjectiles(ServerPlayer player) {
		UUID ownerId = player.getUUID();
		ServerLevel level = player.serverLevel();
		AABB area = player.getBoundingBox().inflate(FREEZE_RADIUS);
		Set<UUID> frozen = FROZEN_PROJECTILES.get(ownerId);
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
			if (frozen == null) {
				frozen = new HashSet<>();
				FROZEN_PROJECTILES.put(ownerId, ownerId, frozen);
			}
			if (frozen.add(projectile.getUUID())) {
				EntityControlLock.acquire(projectile, ControlLockKind.NO_GRAVITY, player);
			}
			projectile.setDeltaMovement(Vec3.ZERO);
			projectile.hurtMarked = true;
		}
		if (frozen == null) {
			return;
		}
		frozen.removeIf(projectileId -> {
			Entity entity = level.getEntity(projectileId);
			if (entity instanceof Projectile projectile && projectile.isAlive()
					&& projectile.getBoundingBox().intersects(area)) {
				return false;
			}
			if (entity != null) {
				EntityControlLock.release(entity, ControlLockKind.NO_GRAVITY, ownerId);
			}
			return true;
		});
	}

	/**
	 * Deactivation shockwave: an impulse on every live non-spectator entity in radius —
	 * except the still-frozen projectiles, which the release drops rather than re-arms.
	 */
	private static void pushNearby(ServerPlayer player, Set<UUID> frozen) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		AABB area = new AABB(origin, origin).inflate(PUSH_RADIUS);
		List<Entity> nearby = level.getEntities(player, area,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& (frozen == null || !frozen.contains(e.getUUID())));
		for (Entity entity : nearby) {
			double dx = entity.getX() - player.getX();
			double dz = entity.getZ() - player.getZ();
			double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
			entity.push(dx / len * 1.5, 0.6, dz / len * 1.5);
			entity.hurtMarked = true;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 0.9f);
		level.sendParticles(ParticleTypes.FLASH,
				player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(), 40, 1.5, 0.5, 1.5, 0.05);
	}

	/** Shield-up cleanse — negative effects only; hero passives and buffs survive. */
	private static void cleanseNegativeEffects(ServerPlayer player) {
		List<Holder<MobEffect>> harmful = new ArrayList<>();
		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				harmful.add(instance.getEffect());
			}
		}
		harmful.forEach(player::removeEffect);
	}

	/** Releases this owner's NO_GRAVITY ref on every projectile it still holds frozen. */
	private static void releaseFrozen(ServerPlayer player) {
		Set<UUID> frozen = FROZEN_PROJECTILES.remove(player.getUUID());
		if (frozen == null || frozen.isEmpty()) {
			return;
		}
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (UUID projectileId : frozen) {
				Entity entity = level.getEntity(projectileId);
				if (entity != null) {
					EntityControlLock.release(entity, ControlLockKind.NO_GRAVITY, player.getUUID());
				}
			}
		}
	}
}
