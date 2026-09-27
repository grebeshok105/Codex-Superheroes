package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.hero.homelander.item.UraniumDaggerItem;
import io.github.grebeshok105.codex.hero.homelander.net.UraniumPressureS2CPayload;
import io.github.grebeshok105.codex.hero.homelander.net.UraniumThreatS2CPayload;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class UraniumDefenseController {
	private static final net.minecraft.resources.ResourceLocation HOMELANDER_ID = ModId.of("homelander");

	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final double THREAT_RADIUS = 64.0;
	private static int tickCounter = 0;
	// No lifecycle clearOn: both collections were rebuilt/trimmed by the 20-tick
	// scan only — wholesale replace and retainAll — never lifecycle-cleared.
	private static final OwnedSessionMap<UUID, Boolean> lastPressured =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));
	private static final OwnedSessionMap<UUID, Integer> lastSourceCount =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private UraniumDefenseController() {
	}


	public static boolean isUnderUraniumThreat(Player player) {
		return lastPressured.containsKey(player.getUUID());
	}

	public static boolean isHomelander(Player player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.hasHero() && HOMELANDER_ID.equals(data.heroId());
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
		List<UUID> ids = new ArrayList<>();
		for (Map.Entry<UUID, Boolean> e : lastPressured) {
			ids.add(e.getKey());
		}
		ServerPlayNetworking.send(player, new UraniumPressureS2CPayload(ids));
		if (isHomelander(player)) {
			Integer stored = lastSourceCount.get(player.getUUID());
			int count = stored == null ? 0 : stored;
			boolean self = count > 0 && !HomelanderEffects.isMadness(player);
			ServerPlayNetworking.send(player, new UraniumThreatS2CPayload(self, count));
		}
	}

	public static void serverTick(MinecraftServer server) {
			if (++tickCounter < SCAN_INTERVAL_TICKS) return;
			tickCounter = 0;

			Set<UUID> pressured = new HashSet<>();
			Map<UUID, Integer> sourceCounts = new HashMap<>();
			List<ServerPlayer> players = server.getPlayerList().getPlayers();
			for (ServerPlayer homelander : players) {
				if (!isHomelander(homelander)) continue;
				if (HomelanderEffects.isMadness(homelander)) {
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

			boolean pressureChanged = pressured.size() != lastPressured.size()
					|| pressured.stream().anyMatch(id -> !lastPressured.containsKey(id));
			if (pressureChanged) {
				lastPressured.clear();
				for (UUID id : pressured) {
					lastPressured.put(id, id, Boolean.TRUE);
				}
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
					boolean self = count > 0 && !HomelanderEffects.isMadness(homelander);
					ServerPlayNetworking.send(homelander, new UraniumThreatS2CPayload(self, count));
					if (self && (prev == null || prev == 0)) {
						ServerLevel l = homelander.serverLevel();
						l.playSound(null, homelander.getX(), homelander.getY(), homelander.getZ(),
								SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.0f);
					}
					lastSourceCount.put(homelander.getUUID(), homelander.getUUID(), count);
				}
			}

			for (Iterator<Map.Entry<UUID, Integer>> it = lastSourceCount.iterator(); it.hasNext();) {
				if (!sourceCounts.containsKey(it.next().getKey())) {
					it.remove();
				}
			}
			}

}
