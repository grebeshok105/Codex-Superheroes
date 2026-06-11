package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.hero.OmnimanHero;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Голосовые реакции героев на появление других героев.
 * Хоумлендер слышит свою реплику, когда кто-то превращается в Омни-Мэна
 * (и наоборот — свежетрансформированный Хоумлендер реагирует на уже
 * присутствующего Омни-Мэна).
 */
public final class HeroReactionController {

	private HeroReactionController() {
	}

	public static void onTransformed(ServerPlayer player, ResourceLocation heroId) {
		if (OmnimanHero.ID.equals(heroId)) {
			for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
				if (p == player) {
					continue;
				}
				if (HomelanderHero.ID.equals(heroIdOf(p))) {
					playReaction(p);
				}
			}
		} else if (HomelanderHero.ID.equals(heroId)) {
			for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
				if (p != player && OmnimanHero.ID.equals(heroIdOf(p))) {
					playReaction(player);
					return;
				}
			}
		}
	}

	private static ResourceLocation heroIdOf(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.heroId();
	}

	private static void playReaction(ServerPlayer homelander) {
		homelander.playNotifySound(ModSounds.HOMELANDER_OMNIMAN_REACT, SoundSource.PLAYERS, 1.0f, 1.0f);
	}
}
