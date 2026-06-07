package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.network.ReinhardCeremonyS2CPayload;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

	private static final ConcurrentHashMap<UUID, CeremonyState> CEREMONIES = new ConcurrentHashMap<>();
	private static final Map<UUID, Set<UUID>> FROZEN_MOBS = new ConcurrentHashMap<>();

	private record CeremonyState(long startTick, long endTick) {}

	private ReinhardSwordDrawCeremonyController() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				CeremonyState st = CEREMONIES.get(player.getUUID());
				if (st == null) continue;
				tickCeremony(player, st);
			}
		});
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
		CEREMONIES.put(player.getUUID(), st);
		FROZEN_MOBS.put(player.getUUID(), new HashSet<>());

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
		HeroAttributes.REINHARD_DRAW.apply(player);
		giveSword(player);
		ReinhardTimeSlowController.armForFirstStrike(player);

		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.isActive(AbilityIds.REINHARD_SWORD_DRAW)) {
			data = data.withActive(AbilityIds.REINHARD_SWORD_DRAW, true);
			player.setAttached(ModAttachments.HERO_DATA, data);
			ModNetworking.syncHeroData(player, data);
		}

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
			applyFreeze(living, player.getUUID());
		}
	}

	private static void applyFreeze(LivingEntity entity, UUID reinhardId) {
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
			Set<UUID> set = FROZEN_MOBS.get(reinhardId);
			if (set != null) {
				if (set.add(mob.getUUID()) && !mob.isNoAi()) {
					mob.setNoAi(true);
				}
			}
		}
	}

	private static void thawNearby(ServerPlayer player) {
		Set<UUID> mobIds = FROZEN_MOBS.remove(player.getUUID());
		if (mobIds == null || mobIds.isEmpty()) return;
		ServerLevel level = player.serverLevel();
		AABB box = new AABB(player.position(), player.position()).inflate(CEREMONY_RADIUS + 8.0);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> mobIds.contains(m.getUUID()))) {
			mob.setNoAi(false);
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

	private static void giveSword(ServerPlayer player) {
		if (player.getMainHandItem().is(ModItems.ROYAL_ICICLE)) return;
		if (player.getOffhandItem().is(ModItems.ROYAL_ICICLE)) return;
		ItemStack stack = new ItemStack(ModItems.ROYAL_ICICLE);
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		} else if (player.getOffhandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.OFF_HAND, stack);
		} else {
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
	}
}
