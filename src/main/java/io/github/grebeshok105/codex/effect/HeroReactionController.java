package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.HomelanderHero;
import io.github.grebeshok105.codex.hero.OmnimanHero;
import io.github.grebeshok105.codex.sound.ModSounds;
import io.github.grebeshok105.codex.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Голосовые реакции героев на появление других героев.
 * Кто-то превращается в Омни-Мэна при живом Хоумлендере (или наоборот,
 * в Хоумлендера при живом Омни-Мэне) — реплику Хоумлендера слышат
 * ВСЕ игроки на сервере.
 */
public final class HeroReactionController {

	private HeroReactionController() {
	}

	public static void onTransformed(ServerPlayer player, ResourceLocation heroId) {
		if (OmnimanHero.ID.equals(heroId)) {
			if (anyOtherWithHero(player, HomelanderHero.ID)) {
				broadcastReaction(player);
			}
		} else if (HomelanderHero.ID.equals(heroId)) {
			if (anyOtherWithHero(player, OmnimanHero.ID)) {
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.heroId();
	}

	private static void broadcastReaction(ServerPlayer trigger) {
		for (ServerPlayer p : trigger.server.getPlayerList().getPlayers()) {
			p.playNotifySound(ModSounds.HOMELANDER_OMNIMAN_REACT, SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}
}
