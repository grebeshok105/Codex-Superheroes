package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DoomsdayFootstepsController {
	private static final int LOOP_INTERVAL_TICKS = 100;

	private static final Map<UUID, Long> NEXT_PLAY = new HashMap<>();

	private DoomsdayFootstepsController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});
	}

	private static void tickPlayer(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		UUID id = player.getUUID();
		if (!data.hasHero() || !DoomsdayHero.ID.equals(data.heroId())) {
			NEXT_PLAY.remove(id);
			return;
		}
		long now = player.level().getGameTime();
		Long next = NEXT_PLAY.get(id);
		if (next != null && now < next) {
			return;
		}
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.HOMELANDER_IRON_FISTS_CHARGE, SoundSource.PLAYERS, 0.85f, 0.7f);
		NEXT_PLAY.put(id, now + LOOP_INTERVAL_TICKS);
	}
}
