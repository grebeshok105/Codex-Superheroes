package io.github.grebeshok105.codex.hero.omniman.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * The Omni-Man half of the Homelander ↔ Omni-Man reaction bark: when someone
 * transforms into Omni-Man while a live Homelander exists, the Homelander
 * reaction line is broadcast to every player. The reverse direction stays in
 * {@code hero.homelander.runtime.HomelanderReactionRule} until the Homelander wave moves it.
 * The foreign hero id is a string literal; the bark sound comes from the
 * shared registry constant — no foreign hero import.
 */
public final class OmnimanReactionRule {
	private static final ResourceLocation OMNIMAN_ID = ModId.of("omniman");
	private static final ResourceLocation HOMELANDER_ID = ModId.of("homelander");

	private OmnimanReactionRule() {
	}

	public static void onTransformed(ServerPlayer player, ResourceLocation heroId) {
		if (!OMNIMAN_ID.equals(heroId)) {
			return;
		}
		if (anyOtherWithHero(player, HOMELANDER_ID)) {
			broadcastReaction(player);
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
