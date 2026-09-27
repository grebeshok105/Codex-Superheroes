package io.github.grebeshok105.codex.hero.ironman.runtime;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.hero.ironman.net.JarvisDetectionS2CPayload;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
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
	private static final ResourceLocation IRON_MAN_ID = ModId.of("iron_man");
	private static final int DETECT_DELAY_TICKS = 100; // 5 секунд
	private static final int SCAN_INTERVAL_TICKS = 20;

	/** ironman -> (target -> heroId, о котором уже объявлено). */
	private static final OwnedSessionMap<UUID, Map<UUID, ResourceLocation>> ANNOUNCED =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));
	/** ironman -> (target -> тиков до объявления). */
	private static final OwnedSessionMap<UUID, Map<UUID, Integer>> PENDING =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	private IronManJarvisController() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.lifecycle().onLeave(player -> {
			// Outer rows drop via ClearOn.LEAVE; inner maps are plain HashMaps and
			// still need the leaver scrubbed out of every other ironman's view.
			UUID id = player.getUUID();
			for (Map.Entry<UUID, Map<UUID, ResourceLocation>> e : ANNOUNCED) {
				e.getValue().remove(id);
			}
			for (Map.Entry<UUID, Map<UUID, Integer>> e : PENDING) {
				e.getValue().remove(id);
			}
		});
	}

	private static void scan(MinecraftServer server) {
		for (ServerPlayer ironman : server.getPlayerList().getPlayers()) {
			UUID imId = ironman.getUUID();
			if (!IRON_MAN_ID.equals(heroIdOf(ironman))) {
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
				Map<UUID, ResourceLocation> announcedMap = ANNOUNCED.get(imId);
				ResourceLocation announced = announcedMap == null ? null : announcedMap.get(target.getUUID());
				if (heroId.equals(announced)) {
					continue;
				}
				Map<UUID, Integer> pending = PENDING.get(imId);
				if (pending == null) {
					pending = new HashMap<>();
					PENDING.put(imId, imId, pending);
				}
				pending.putIfAbsent(target.getUUID(), DETECT_DELAY_TICKS);
			}
		}
	}

	private static void tickPending(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Map<UUID, Integer>>> it = PENDING.iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Map<UUID, Integer>> entry = it.next();
			ServerPlayer ironman = server.getPlayerList().getPlayer(entry.getKey());
			if (ironman == null || !IRON_MAN_ID.equals(heroIdOf(ironman))) {
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
				Map<UUID, ResourceLocation> announcedMap = ANNOUNCED.get(entry.getKey());
				if (announcedMap == null) {
					announcedMap = new HashMap<>();
					ANNOUNCED.put(entry.getKey(), entry.getKey(), announcedMap);
				}
				announcedMap.put(pe.getKey(), heroId);
				int distance = (int) Math.round(ironman.position().distanceTo(target.position()));
				JarvisThreatClass threat = JarvisThreatClass.forHero(heroId);
				String quote = JarvisQuotes.randomDetect(threat);
				ServerPlayNetworking.send(ironman, new JarvisDetectionS2CPayload(
						target.getGameProfile().getName(), heroId, distance,
						threat.label(), quote));
			}
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}
	}

	private static ResourceLocation heroIdOf(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.heroId();
	}

	public static void serverTick(MinecraftServer server) {
		if (server.getTickCount() % SCAN_INTERVAL_TICKS == 0) {
			scan(server);
		}
		tickPending(server);
	}

}
