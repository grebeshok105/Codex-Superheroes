package io.github.grebeshok105.codex.ability;

import io.github.grebeshok105.codex.core.ability.AbilityAvailability;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability.Visibility;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase PLAYERS tick task (stage C4): recomputes the owner's ability visibility from server
 * state — the same rules the deleted client-side filter applied to client mirrors — and
 * writes the {@code ability_availability} attachment only when the answer changed.
 */
public final class AbilityAvailabilitySync {
	private AbilityAvailabilitySync() {
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		AbilityAvailability next = compute(player, data);
		AbilityAvailability prev = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		if (next.equals(prev) || (prev == null && next.entries().isEmpty())) {
			return;
		}
		player.setAttached(CoreAttachments.ABILITY_AVAILABILITY, next);
	}

	private static AbilityAvailability compute(ServerPlayer player, HeroData data) {
		Hero hero = data.hasHero() ? Heroes.get(data.heroId()) : null;
		if (hero == null) {
			return AbilityAvailability.EMPTY;
		}
		// A vanity-stripped victim loses the whole list, matching the old client filter.
		boolean stripped = ModEffects.isVanityStripped(player);
		Map<ResourceLocation, Visibility> entries = new HashMap<>();
		for (ResourceLocation id : hero.getAbilities()) {
			Visibility visibility = stripped ? Visibility.HIDDEN : hero.visibility(player, id);
			if (visibility != Visibility.AVAILABLE) {
				entries.put(id, visibility);
			}
		}
		return new AbilityAvailability(entries);
	}
}
