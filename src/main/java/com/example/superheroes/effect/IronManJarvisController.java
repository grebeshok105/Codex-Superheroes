package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.network.JarvisDetectionS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Джарвис: пока игрок в костюме Железного Человека, сервер ищет других
 * игроков-героев. Новый герой замечен -> через 5 секунд Железному Человеку
 * уходит {@link JarvisDetectionS2CPayload} (клиент проигрывает реплику и
 * после неё показывает инфопанель). Каждая пара (цель, герой) объявляется
 * один раз; если цель сменила героя — объявляется заново.
 */
public final class IronManJarvisController {
	private static final int DETECT_DELAY_TICKS = 100; // 5 секунд
	private static final int SCAN_INTERVAL_TICKS = 20;

	/** ironman -> (target -> heroId, о котором уже объявлено). */
	private static final Map<UUID, Map<UUID, ResourceLocation>> ANNOUNCED = new HashMap<>();
	/** ironman -> (target -> тиков до объявления). */
	private static final Map<UUID, Map<UUID, Integer>> PENDING = new HashMap<>();

	private IronManJarvisController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SCAN_INTERVAL_TICKS == 0) {
				scan(server);
			}
			tickPending(server);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			ANNOUNCED.remove(id);
			PENDING.remove(id);
			for (Map<UUID, ResourceLocation> m : ANNOUNCED.values()) {
				m.remove(id);
			}
			for (Map<UUID, Integer> m : PENDING.values()) {
				m.remove(id);
			}
		});
	}

	private static void scan(MinecraftServer server) {
		for (ServerPlayer ironman : server.getPlayerList().getPlayers()) {
			UUID imId = ironman.getUUID();
			if (!IronManHero.ID.equals(heroIdOf(ironman))) {
				// снял костюм — забываем, чтобы после повторной трансформации Джарвис доложил заново
				ANNOUNCED.remove(imId);
				PENDING.remove(imId);
				continue;
			}
			for (ServerPlayer target : server.getPlayerList().getPlayers()) {
				if (target == ironman) {
					continue;
				}
				ResourceLocation heroId = heroIdOf(target);
				if (heroId == null) {
					continue;
				}
				ResourceLocation announced = ANNOUNCED
						.getOrDefault(imId, Map.of())
						.get(target.getUUID());
				if (heroId.equals(announced)) {
					continue;
				}
				Map<UUID, Integer> pending = PENDING.computeIfAbsent(imId, id -> new HashMap<>());
				pending.putIfAbsent(target.getUUID(), DETECT_DELAY_TICKS);
			}
		}
	}

	private static void tickPending(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Map<UUID, Integer>>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Map<UUID, Integer>> entry = it.next();
			ServerPlayer ironman = server.getPlayerList().getPlayer(entry.getKey());
			if (ironman == null || !IronManHero.ID.equals(heroIdOf(ironman))) {
				it.remove();
				continue;
			}
			for (Iterator<Map.Entry<UUID, Integer>> ti = entry.getValue().entrySet().iterator(); ti.hasNext(); ) {
				Map.Entry<UUID, Integer> pe = ti.next();
				ServerPlayer target = server.getPlayerList().getPlayer(pe.getKey());
				ResourceLocation heroId = target == null ? null : heroIdOf(target);
				if (heroId == null) {
					ti.remove();
					continue;
				}
				int left = pe.getValue() - 1;
				if (left > 0) {
					pe.setValue(left);
					continue;
				}
				ti.remove();
				ANNOUNCED.computeIfAbsent(entry.getKey(), id -> new HashMap<>())
						.put(pe.getKey(), heroId);
				int distance = (int) Math.round(ironman.position().distanceTo(target.position()));
				ServerPlayNetworking.send(ironman, new JarvisDetectionS2CPayload(
						target.getGameProfile().getName(), heroId, distance));
			}
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}
	}

	private static ResourceLocation heroIdOf(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.heroId();
	}
}
