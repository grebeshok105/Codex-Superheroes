package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.AbilityScopedModifiers;
import io.github.grebeshok105.codex.network.ReinhardCeremonyS2CPayload;
import io.github.grebeshok105.codex.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import io.github.grebeshok105.codex.core.transform.HeroData;

/**
 * Ceremonial sword draw for Reinhard. Lasts 10 seconds (200 ticks):
 * <ul>
 *   <li>All living entities within 50 blocks of Reinhard are frozen
 *       (mobs: <code>noAi</code>; players: Slowness 6 + Weakness 4 + Mining Fatigue 4).</li>
 *   <li>Reinhard himself is also frozen during the buildup.</li>
 *   <li>VFX rings + sounds escalate around Reinhard each tick.</li>
 *   <li>All players in radius receive a ramping screen-brighten overlay via
 *       {@link ReinhardCeremonyS2CPayload}.</li>
 *   <li>At t=200 ticks the actual sword is given and the toggle is marked active.</li>
 * </ul>
 *
 * Cancellation paths:
 * <ul>
 *   <li>{@link ReinhardController#clearAdaptations} (suit removed) calls {@link #cancelCeremony}.</li>
 *   <li>Reinhard logging out / changing dimension — cleaned up via player iteration each tick.</li>
 * </ul>
 */
public final class ReinhardSwordDrawCeremonyController {
	public static final int CEREMONY_DURATION_TICKS = 100;
	public static final double CEREMONY_RADIUS = 50.0;
	private static final int FREEZE_EFFECT_REFRESH = 18; // re-apply slightly under 1s

	// Empty ClearOn: leave/death cancellation must still see these entries — cancelCeremony
	// thaws the mobs recorded in FROZEN_MOBS, so the drops stay inside that explicit hook.
	private static final OwnedSessionMap<UUID, CeremonyState> CEREMONIES =
			OwnedSessionMap.create(LifecycleRegistrar.global(), java.util.Set.of());
	private static final OwnedSessionMap<UUID, Set<UUID>> FROZEN_MOBS =
			OwnedSessionMap.create(LifecycleRegistrar.global(), java.util.Set.of());

	private record CeremonyState(long startTick, long endTick) {}

	private ReinhardSwordDrawCeremonyController() {}

	public static void register(HeroModuleContext ctx) {
		ctx.lifecycle().onLeave(ReinhardSwordDrawCeremonyController::cancelCeremony);
		ctx.lifecycle().onDeath(ReinhardSwordDrawCeremonyController::cancelCeremony);
	}

	public static boolean isInCeremony(ServerPlayer player) {
		return CEREMONIES.containsKey(player.getUUID());
	}

	public static boolean startCeremony(ServerPlayer player) {
		if (CEREMONIES.containsKey(player.getUUID())) return false;
		ReinhardState rstate = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (rstate.swordDrawn()) return false;
		long now = player.serverLevel().getGameTime();
		CeremonyState st = new CeremonyState(now, now + CEREMONY_DURATION_TICKS);
		CEREMONIES.put(player.getUUID(), player.getUUID(), st);
		FROZEN_MOBS.put(player.getUUID(), player.getUUID(), new HashSet<>());

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.REINHARD_SWORD_DRAW_CEREMONY, SoundSource.PLAYERS, 2.0f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6f, 0.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.4f);

		freezeNearby(player);
		broadcastProgress(player, true, 0f);
		return true;
	}

	public static void cancelCeremony(ServerPlayer player) {
		CeremonyState st = CEREMONIES.remove(player.getUUID());
		if (st == null) return;
		thawNearby(player);
		broadcastProgress(player, false, 0f);
	}

	private static void tickCeremony(ServerPlayer player, CeremonyState st) {
		long now = player.serverLevel().getGameTime();
		long elapsed = now - st.startTick();
		if (elapsed < 0) elapsed = 0;
		float progress = Math.min(1f, (float) elapsed / (float) CEREMONY_DURATION_TICKS);

		ServerLevel level = player.serverLevel();

		// Re-apply freeze every ~1s in case effects expire or new mobs wandered in
		if (elapsed % FREEZE_EFFECT_REFRESH == 0) {
			freezeNearby(player);
		}

		// VFX around Reinhard — ramping intensity
		spawnAuraVfx(level, player, progress);

		// Sound build-up
		if (elapsed == 40) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.4f, 0.7f);
		} else if (elapsed == 100) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.9f);
		} else if (elapsed == 160) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.2f);
		}

		// Sync overlay to nearby players each tick
		broadcastProgress(player, true, progress);

		if (elapsed >= CEREMONY_DURATION_TICKS) {
			completeCeremony(player);
		}
	}

	private static void completeCeremony(ServerPlayer player) {
		CEREMONIES.remove(player.getUUID());
		thawNearby(player);

		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		player.setAttached(ModAttachments.REINHARD_STATE, state.withSwordDrawn(true));
		AbilityScopedModifiers.REINHARD_DRAW.apply(player);
		if (!io.github.grebeshok105.codex.ability.ReinhardSwordDrawAbility.giveSword(player)) {
			player.displayClientMessage(Component.translatable("ability.superheroes.bound_weapon.no_room"), true);
		}
		ReinhardTimeSlowController.armForFirstStrike(player);

		HeroDataStore.update(player, d -> d.withActive(AbilityIds.REINHARD_SWORD_DRAW, true));

		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(),
				160, 1.2, 1.6, 1.2, 0.25);
		level.sendParticles(ParticleTypes.FLASH,
				player.getX(), player.getY() + 1.0, player.getZ(),
				1, 0, 0, 0, 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.6f, 1.4f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.2f, 1.0f);

		broadcastProgress(player, false, 1f);
	}

	private static void freezeNearby(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		AABB box = new AABB(player.position(), player.position()).inflate(CEREMONY_RADIUS);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> true)) {
			applyFreeze(living, player);
		}
	}

	private static void applyFreeze(LivingEntity entity, ServerPlayer reinhard) {
		// Slowness 6 caps movement at 0; Weakness 4 + Mining Fatigue 4 prevent meaningful counter-attack
		entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				CEREMONY_DURATION_TICKS + 5, 6, true, false, false));
		entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
				CEREMONY_DURATION_TICKS + 5, 4, true, false, false));
		entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
				CEREMONY_DURATION_TICKS + 5, 4, true, false, false));
		entity.setDeltaMovement(Vec3.ZERO);
		entity.hurtMarked = true;
		if (entity instanceof Mob mob) {
			Set<UUID> set = FROZEN_MOBS.get(reinhard.getUUID());
			if (set != null) {
				set.add(mob.getUUID());
				EntityControlLock.acquire(mob, ControlLockKind.NO_AI, reinhard);
			}
		}
	}

	private static void thawNearby(ServerPlayer player) {
		Set<UUID> mobIds = FROZEN_MOBS.remove(player.getUUID());
		if (mobIds == null || mobIds.isEmpty()) return;
		ServerLevel level = player.serverLevel();
		AABB box = new AABB(player.position(), player.position()).inflate(CEREMONY_RADIUS + 8.0);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> mobIds.contains(m.getUUID()))) {
			EntityControlLock.release(mob, ControlLockKind.NO_AI, player.getUUID());
			mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			mob.removeEffect(MobEffects.WEAKNESS);
			mob.removeEffect(MobEffects.DIG_SLOWDOWN);
		}
		// Players: just drop the slowness early
		for (Player p : level.getEntitiesOfClass(Player.class, box, p -> true)) {
			p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			p.removeEffect(MobEffects.WEAKNESS);
			p.removeEffect(MobEffects.DIG_SLOWDOWN);
		}
	}

	private static void spawnAuraVfx(ServerLevel level, ServerPlayer player, float progress) {
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();
		double ringR = 1.4 + 5.0 * progress;
		int ringPts = 24 + (int) (40 * progress);
		long t = level.getGameTime();
		for (int i = 0; i < ringPts; i++) {
			double ang = (Math.PI * 2 * i) / ringPts;
			double rx = cx + Math.cos(ang) * ringR;
			double rz = cz + Math.sin(ang) * ringR;
			level.sendParticles(ParticleTypes.END_ROD, rx, cy + 0.05, rz, 1, 0, 0.05, 0, 0.0);
		}
		// Vertical column rising
		for (int i = 0; i < 3 + (int) (6 * progress); i++) {
			double yy = cy + (i * 0.4) + ((t % 20) * 0.05);
			level.sendParticles(ParticleTypes.END_ROD, cx, yy, cz, 1, 0.05, 0.0, 0.05, 0.0);
		}
		// Soul fire wisps (ominous)
		if (progress > 0.4f) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					cx, cy + 1.0, cz, 4, 0.6, 0.8, 0.6, 0.02);
		}
		// Glow dust at high progress
		if (progress > 0.7f) {
			level.sendParticles(ParticleTypes.GLOW,
					cx, cy + 1.2, cz, 6, 1.0, 1.0, 1.0, 0.05);
		}
	}

	private static void broadcastProgress(ServerPlayer reinhard, boolean active, float progress) {
		ServerLevel level = reinhard.serverLevel();
		AABB box = new AABB(reinhard.position(), reinhard.position()).inflate(CEREMONY_RADIUS + 4.0);
		ReinhardCeremonyS2CPayload payload = new ReinhardCeremonyS2CPayload(active, progress);
		for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, box, p -> true)) {
			ServerPlayNetworking.send(target, payload);
		}
	}


	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		CeremonyState st = CEREMONIES.get(player.getUUID());
		if (st == null) return;
		tickCeremony(player, st);
	}

}
