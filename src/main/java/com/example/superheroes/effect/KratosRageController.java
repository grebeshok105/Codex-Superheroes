package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.network.KratosRageS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KratosRageController {
	public static final float MAX_RAGE = 100f;
	private static final float TAKEN_PER_DMG = 0.4f;
	private static final float DEALT_PER_DMG = 1.0f;
	private static final int DURATION_TICKS = 240;
	private static final float DRAIN_PER_TICK = MAX_RAGE / (float) DURATION_TICKS;

	private static final Map<UUID, Float> RAGE = new HashMap<>();
	private static final Set<UUID> ACTIVE = new HashSet<>();

	private KratosRageController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer victim && isKratos(victim) && !ACTIVE.contains(victim.getUUID())) {
				addRage(victim, amount * TAKEN_PER_DMG);
			}
			Entity src = source.getEntity();
			if (src instanceof ServerPlayer attacker && entity != attacker
					&& isKratos(attacker) && !ACTIVE.contains(attacker.getUUID())) {
				addRage(attacker, amount * DEALT_PER_DMG);
			}
			return true;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (ACTIVE.isEmpty()) return;
			java.util.Iterator<UUID> it = ACTIVE.iterator();
			while (it.hasNext()) {
				UUID id = it.next();
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p == null) {
					it.remove();
					continue;
				}
				if (!isKratos(p)) {
					it.remove();
					RAGE.put(id, 0f);
					sync(p);
					continue;
				}
				float curr = RAGE.getOrDefault(id, 0f);
				float next = curr - DRAIN_PER_TICK;
				if (next <= 0f) {
					RAGE.put(id, 0f);
					it.remove();
					AbilityRouter.deactivate(p, AbilityIds.KRATOS_SPARTAN_RAGE);
					sync(p);
				} else {
					RAGE.put(id, next);
					if (server.getTickCount() % 4 == 0) sync(p);
				}
			}
		});

		ServerTickEvents.START_SERVER_TICK.register(server -> {
			RAGE.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
			ACTIVE.removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
		});
	}

	public static boolean tryActivate(ServerPlayer player) {
		UUID id = player.getUUID();
		if (RAGE.getOrDefault(id, 0f) < MAX_RAGE - 0.001f) return false;
		ACTIVE.add(id);
		sync(player);
		return true;
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.contains(player.getUUID());
	}

	public static void onAbilityDeactivated(ServerPlayer player) {
		UUID id = player.getUUID();
		ACTIVE.remove(id);
		RAGE.put(id, 0f);
		sync(player);
	}

	public static float getRage(ServerPlayer player) {
		return RAGE.getOrDefault(player.getUUID(), 0f);
	}

	private static void addRage(ServerPlayer player, float delta) {
		if (delta <= 0f) return;
		UUID id = player.getUUID();
		float curr = RAGE.getOrDefault(id, 0f);
		float next = Math.min(MAX_RAGE, curr + delta);
		if (next != curr) {
			RAGE.put(id, next);
			sync(player);
		}
	}

	private static boolean isKratos(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return KratosHero.ID.equals(data.heroId());
	}

	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, new KratosRageS2CPayload(getRage(player), isActive(player)));
	}
}
