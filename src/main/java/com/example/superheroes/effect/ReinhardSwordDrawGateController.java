package com.example.superheroes.effect;

import com.example.superheroes.network.ReinhardSwordGateS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gate that unlocks Reinhard's sword-draw ceremony once a single attacker has dealt
 * {@link #DAMAGE_THRESHOLD} damage within {@link #WINDOW_TICKS}. Per-attacker accumulators
 * decay (entry expires) when no hit lands inside the window. Once the gate is "ready",
 * it stays ready until {@link #consumeReady} is called (i.e. the ceremony actually starts).
 */
public final class ReinhardSwordDrawGateController {
	public static final float DAMAGE_THRESHOLD = 30.0f;
	public static final int WINDOW_TICKS = 600; // 30 seconds — must accumulate within this window per source

	private record Accum(float total, long lastHitTick) {}

	private static final Map<UUID, Map<UUID, Accum>> PER_PLAYER = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> READY = new ConcurrentHashMap<>();

	private ReinhardSwordDrawGateController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			long now = server.overworld().getGameTime();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Map<UUID, Accum> per = PER_PLAYER.get(player.getUUID());
				if (per == null) continue;
				if (per.isEmpty()) continue;
				boolean changed = false;
				Iterator<Map.Entry<UUID, Accum>> it = per.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<UUID, Accum> e = it.next();
					if (now - e.getValue().lastHitTick > WINDOW_TICKS) {
						it.remove();
						changed = true;
					}
				}
				if (changed) {
					broadcastProgress(player);
				}
			}
			PER_PLAYER.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
			READY.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
		});
	}

	public static void recordHit(ServerPlayer player, Entity attacker, float amount) {
		if (attacker == null || amount <= 0f) return;
		if (Boolean.TRUE.equals(READY.get(player.getUUID()))) return;
		long now = player.serverLevel().getGameTime();
		Map<UUID, Accum> per = PER_PLAYER.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
		UUID attackerId = attacker.getUUID();
		Accum existing = per.get(attackerId);
		float total;
		if (existing == null || now - existing.lastHitTick > WINDOW_TICKS) {
			total = amount;
		} else {
			total = existing.total + amount;
		}
		per.put(attackerId, new Accum(total, now));
		if (total >= DAMAGE_THRESHOLD) {
			READY.put(player.getUUID(), true);
			ServerPlayNetworking.send(player, new ReinhardSwordGateS2CPayload(true, 1f));
			player.displayClientMessage(
					net.minecraft.network.chat.Component.translatable(
							"ability.superheroes.reinhard_sword_draw.ready")
							.withStyle(net.minecraft.ChatFormatting.GOLD,
									net.minecraft.ChatFormatting.BOLD),
					true);
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
					net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
					net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 1.6f);
			return;
		}
		broadcastProgress(player);
	}

	public static boolean isReady(ServerPlayer player) {
		return Boolean.TRUE.equals(READY.get(player.getUUID()));
	}

	public static void consumeReady(ServerPlayer player) {
		READY.remove(player.getUUID());
		PER_PLAYER.remove(player.getUUID());
		ServerPlayNetworking.send(player, new ReinhardSwordGateS2CPayload(false, 0f));
	}

	public static void clearAll(ServerPlayer player) {
		consumeReady(player);
	}

	private static void broadcastProgress(ServerPlayer player) {
		if (Boolean.TRUE.equals(READY.get(player.getUUID()))) {
			ServerPlayNetworking.send(player, new ReinhardSwordGateS2CPayload(true, 1f));
			return;
		}
		Map<UUID, Accum> per = PER_PLAYER.get(player.getUUID());
		float best = 0f;
		if (per != null) {
			for (Accum a : per.values()) {
				if (a.total > best) best = a.total;
			}
		}
		float progress = Math.min(1f, best / DAMAGE_THRESHOLD);
		ServerPlayNetworking.send(player, new ReinhardSwordGateS2CPayload(false, progress));
	}
}
