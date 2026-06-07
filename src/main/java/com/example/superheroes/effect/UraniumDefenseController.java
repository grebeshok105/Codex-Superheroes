package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.item.UraniumDaggerItem;
import com.example.superheroes.network.UraniumPressureS2CPayload;
import com.example.superheroes.network.UraniumThreatS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class UraniumDefenseController {
	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final double THREAT_RADIUS = 64.0;
	private static int tickCounter = 0;
	private static Set<UUID> lastPressured = Collections.emptySet();
	private static final Map<UUID, Integer> lastSourceCount = new HashMap<>();

	private UraniumDefenseController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++tickCounter < SCAN_INTERVAL_TICKS) return;
			tickCounter = 0;

			Set<UUID> pressured = new HashSet<>();
			Map<UUID, Integer> sourceCounts = new HashMap<>();
			List<ServerPlayer> players = server.getPlayerList().getPlayers();
			for (ServerPlayer homelander : players) {
				if (!isHomelander(homelander)) continue;
				if (ModEffects.isMadness(homelander)) {
					sourceCounts.put(homelander.getUUID(), 0);
					continue;
				}
				ServerLevel level = homelander.serverLevel();
				int count = 0;
				for (ServerPlayer other : players) {
					if (other == homelander) continue;
					if (other.serverLevel() != level) continue;
					if (homelander.distanceToSqr(other) > THREAT_RADIUS * THREAT_RADIUS) continue;
					if (hasUraniumDagger(other)) {
						count++;
					}
				}
				sourceCounts.put(homelander.getUUID(), count);
				if (count > 0) pressured.add(homelander.getUUID());
			}

			boolean pressureChanged = !pressured.equals(lastPressured);
			if (pressureChanged) {
				lastPressured = pressured;
				List<UUID> ids = new ArrayList<>(pressured);
				UraniumPressureS2CPayload payload = new UraniumPressureS2CPayload(ids);
				for (ServerPlayer p : players) {
					ServerPlayNetworking.send(p, payload);
				}
			}

			for (ServerPlayer homelander : players) {
				if (!isHomelander(homelander)) continue;
				int count = sourceCounts.getOrDefault(homelander.getUUID(), 0);
				Integer prev = lastSourceCount.get(homelander.getUUID());
				if (prev == null || prev != count) {
					boolean self = count > 0 && !ModEffects.isMadness(homelander);
					ServerPlayNetworking.send(homelander, new UraniumThreatS2CPayload(self, count));
					if (self && (prev == null || prev == 0)) {
						ServerLevel l = homelander.serverLevel();
						l.playSound(null, homelander.getX(), homelander.getY(), homelander.getZ(),
								SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.0f);
					}
					lastSourceCount.put(homelander.getUUID(), count);
				}
			}

			lastSourceCount.keySet().retainAll(sourceCounts.keySet());
		});
	}

	public static boolean isUnderUraniumThreat(Player player) {
		return lastPressured.contains(player.getUUID());
	}

	public static boolean isHomelander(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && HomelanderHero.ID.equals(data.heroId());
	}

	public static boolean hasUraniumDagger(Player player) {
		for (ItemStack stack : player.getInventory().items) {
			if (stack.getItem() instanceof UraniumDaggerItem) return true;
		}
		for (ItemStack stack : player.getInventory().offhand) {
			if (stack.getItem() instanceof UraniumDaggerItem) return true;
		}
		return false;
	}

	public static boolean isPlayerWithDagger(Player player) {
		return player instanceof Player && hasUraniumDagger(player);
	}

	public static float laserDamageMultiplier(Player target) {
		return hasUraniumDagger(target) ? 0.5f : 1.0f;
	}

	public static void sendCurrentTo(ServerPlayer player) {
		List<UUID> ids = new ArrayList<>(lastPressured);
		ServerPlayNetworking.send(player, new UraniumPressureS2CPayload(ids));
		if (isHomelander(player)) {
			int count = lastSourceCount.getOrDefault(player.getUUID(), 0);
			boolean self = count > 0 && !ModEffects.isMadness(player);
			ServerPlayNetworking.send(player, new UraniumThreatS2CPayload(self, count));
		}
	}
}
