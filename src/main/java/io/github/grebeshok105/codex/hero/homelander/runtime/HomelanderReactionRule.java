package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.sound.ModSounds;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Голосовые реакции героев на появление других героев.
 * Кто-то превращается в Омни-Мэна при живом Хоумлендере (или наоборот,
 * в Хоумлендера при живом Омни-Мэне) — реплику Хоумлендера слышат
 * ВСЕ игроки на сервере.
 */
public final class HomelanderReactionRule {
	private static final net.minecraft.resources.ResourceLocation HOMELANDER_ID = ModId.of("homelander");


	private HomelanderReactionRule() {
	}

	public static void onTransformed(ServerPlayer player, ResourceLocation heroId) {
		// The Omni-Man direction lives in hero/omniman (OmnimanReactionRule);
		// only the Homelander direction remains here until the Homelander wave
		// moves it and renames this hook off the shared controller. The paired
		// hero id is a string literal — no foreign import.
		if (HOMELANDER_ID.equals(heroId)) {
			if (anyOtherWithHero(player, ModId.of("omniman"))) {
				broadcastReaction(player);
			}
		}
	}

	private static boolean anyOtherWithHero(ServerPlayer player, ResourceLocation heroId) {
		for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
			if (p != player && heroId.equals(heroIdOf(p))) {
				return true;
			}
		}
		return false;
	}

	private static ResourceLocation heroIdOf(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.heroId();
	}

	private static void broadcastReaction(ServerPlayer trigger) {
		for (ServerPlayer p : trigger.server.getPlayerList().getPlayers()) {
			p.playNotifySound(ModSounds.HOMELANDER_OMNIMAN_REACT, SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}
}
