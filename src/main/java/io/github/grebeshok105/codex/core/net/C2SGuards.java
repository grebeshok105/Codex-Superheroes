package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side guards for C2S payload handlers. A handler whose guard fails must return without
 * side effects — the payload is ignored, not treated as an error.
 */
public final class C2SGuards {
	/** True only while {@code player} is transformed into {@code heroId}. */
	public static boolean requireHero(ServerPlayer player, ResourceLocation heroId) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return heroId.equals(data.heroId());
	}

	/** True only while {@code abilityId} is flagged active on {@code player}'s hero data. */
	public static boolean requireActiveAbility(ServerPlayer player, ResourceLocation abilityId) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.isActive(abilityId);
	}

	private C2SGuards() {
	}
}
