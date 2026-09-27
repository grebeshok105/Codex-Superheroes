package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.sound.ModSounds;
import io.github.grebeshok105.codex.core.transform.HeroData;
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


	public static void tickPlayer(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
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
