package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.KratosHero;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.network.KratosRageS2CPayload;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class KratosRageController {
	public static final float MAX_RAGE = 100f;
	private static final float TAKEN_PER_DMG = 0.3f;
	private static final float DEALT_PER_DMG = 0.75f;
	private static final int DURATION_TICKS = 240;
	private static final float DRAIN_PER_TICK = MAX_RAGE / (float) DURATION_TICKS;

	// Rage is session-scoped: leave/death drop the tracking entries instead of waiting
	// for the offline sweep (a drop reads the same as the old 0f sentinel via getRage).
	private static final OwnedSessionMap<UUID, Float> RAGE =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));
	private static final OwnedSessionMap<UUID, Boolean> ACTIVE =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));

	private KratosRageController() {
	}

	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer victim && isKratos(victim) && !ACTIVE.containsKey(victim.getUUID())) {
				addRage(victim, damageTaken * TAKEN_PER_DMG);
			}
			Entity src = source.getEntity();
			if (src instanceof ServerPlayer attacker && entity != attacker
					&& isKratos(attacker) && !ACTIVE.containsKey(attacker.getUUID())) {
				addRage(attacker, damageTaken * DEALT_PER_DMG);
			}
		});


		ctx.ticks().start(server -> {
			dropGonePlayers(server, RAGE);
			dropGonePlayers(server, ACTIVE);
		});
	}

	private static void dropGonePlayers(MinecraftServer server, OwnedSessionMap<UUID, ?> map) {
		for (Iterator<? extends Map.Entry<UUID, ?>> it = map.iterator(); it.hasNext();) {
			if (server.getPlayerList().getPlayer(it.next().getKey()) == null) {
				it.remove();
			}
		}
	}

	public static boolean tryActivate(ServerPlayer player) {
		UUID id = player.getUUID();
		if (getRage(id) < MAX_RAGE - 0.001f) return false;
		ACTIVE.put(id, id, Boolean.TRUE);
		sync(player);
		return true;
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static void onAbilityDeactivated(ServerPlayer player) {
		UUID id = player.getUUID();
		ACTIVE.remove(id);
		RAGE.put(id, id, 0f);
		sync(player);
	}

	public static float getRage(ServerPlayer player) {
		return getRage(player.getUUID());
	}

	private static float getRage(UUID id) {
		Float rage = RAGE.get(id);
		return rage == null ? 0f : rage;
	}

	private static void addRage(ServerPlayer player, float delta) {
		if (delta <= 0f) return;
		UUID id = player.getUUID();
		float curr = getRage(id);
		float next = Math.min(MAX_RAGE, curr + delta);
		if (next != curr) {
			RAGE.put(id, id, next);
			sync(player);
		}
	}

	private static boolean isKratos(Player player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return KratosHero.ID.equals(data.heroId());
	}

	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, new KratosRageS2CPayload(getRage(player), isActive(player)));
	}

	public static void serverTick(MinecraftServer server) {
			if (ACTIVE.size() == 0) return;
			Iterator<Map.Entry<UUID, Boolean>> it = ACTIVE.iterator();
			while (it.hasNext()) {
				UUID id = it.next().getKey();
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p == null) {
					it.remove();
					continue;
				}
				if (!isKratos(p)) {
					it.remove();
					RAGE.put(id, id, 0f);
					sync(p);
					continue;
				}
				float curr = getRage(id);
				float next = curr - DRAIN_PER_TICK;
				if (next <= 0f) {
					RAGE.put(id, id, 0f);
					it.remove();
					AbilityRouter.deactivate(p, AbilityIds.KRATOS_SPARTAN_RAGE);
					sync(p);
				} else {
					RAGE.put(id, id, next);
					if (server.getTickCount() % 4 == 0) sync(p);
				}
			}
			}

}
