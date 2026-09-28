package io.github.grebeshok105.codex.hero.doomsday.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.Set;
import java.util.UUID;

public final class DoomsdayFootstepsController {
	private static final int LOOP_INTERVAL_TICKS = 100;

	private static final ResourceLocation DOOMSDAY_ID = ModId.of("doomsday");

	// Next-play deadline per player; self-healing timer state, safe to drop on leave/hero-clear
	// (re-seeded by the next tickPlayer). Old code left stale entries behind on relog.
	private static final OwnedSessionMap<UUID, Long> NEXT_PLAY =
			OwnedSessionMap.create(LifecycleRegistrar.global(), Set.of(ClearOn.LEAVE, ClearOn.HERO_CLEAR));

	private DoomsdayFootstepsController() {
	}


	public static void tickPlayer(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		UUID id = player.getUUID();
		if (!data.hasHero() || !DOOMSDAY_ID.equals(data.heroId())) {
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
				HomelanderSounds.IRON_FISTS_CHARGE, SoundSource.PLAYERS, 0.85f, 0.7f);
		NEXT_PLAY.put(id, id, now + LOOP_INTERVAL_TICKS);
	}
}
