package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * §3 Полёт Хоумлендера: пока toggle FLIGHT активен, идёт счётчик 5 секунд;
 * по истечении сервер принудительно деактивирует FLIGHT и кладёт игрока на 1с
 * cooldown. На земле — выключение мгновенное. Во время MADNESS нерф снят.
 *
 * Применяется ТОЛЬКО к Хоумлендеру.
 */
public final class FlightController {
	private static final int AUTO_OFF_TICKS = 100; // 5 c
	private static final int COOLDOWN_TICKS = 20;  // 1 c

	private static final Map<UUID, Long> ACTIVE_SINCE = new HashMap<>();
	private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

	private FlightController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
			cleanup(server);
		});
	}

	private static void tickPlayer(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		boolean isHomelander = data.hasHero() && HomelanderHero.ID.equals(data.heroId());
		boolean active = data.isActive(AbilityIds.FLIGHT);
		UUID id = player.getUUID();

		if (!isHomelander) {
			ACTIVE_SINCE.remove(id);
			return;
		}
		if (ModEffects.isMadness(player)) {
			ACTIVE_SINCE.remove(id);
			return;
		}
		if (!UraniumDefenseController.isUnderUraniumThreat(player)) {
			ACTIVE_SINCE.remove(id);
			return;
		}
		if (!active) {
			ACTIVE_SINCE.remove(id);
			return;
		}

		// На земле → выключить мгновенно
		if (player.onGround() || player.isInWater()) {
			forceOff(player);
			return;
		}

		long now = player.level().getGameTime();
		Long since = ACTIVE_SINCE.get(id);
		if (since == null) {
			ACTIVE_SINCE.put(id, now);
			return;
		}
		long elapsed = now - since;
		if (elapsed >= AUTO_OFF_TICKS) {
			forceOff(player);
		}
	}

	private static void forceOff(ServerPlayer player) {
		UUID id = player.getUUID();
		ACTIVE_SINCE.remove(id);
		COOLDOWN_UNTIL.put(id, player.level().getGameTime() + COOLDOWN_TICKS);
		AbilityRouter.deactivate(player, AbilityIds.FLIGHT);
	}

	public static boolean isOnCooldown(ServerPlayer player) {
		Long until = COOLDOWN_UNTIL.get(player.getUUID());
		if (until == null) return false;
		if (player.level().getGameTime() >= until) {
			COOLDOWN_UNTIL.remove(player.getUUID());
			return false;
		}
		return true;
	}

	private static void cleanup(net.minecraft.server.MinecraftServer server) {
		Iterator<Map.Entry<UUID, Long>> it = COOLDOWN_UNTIL.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Long> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || p.level().getGameTime() >= e.getValue()) {
				it.remove();
			}
		}
	}

	public static void clear(UUID id) {
		ACTIVE_SINCE.remove(id);
		COOLDOWN_UNTIL.remove(id);
	}
}
