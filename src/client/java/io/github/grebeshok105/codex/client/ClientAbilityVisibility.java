package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability.Visibility;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the {@code ability_availability} attachment the server computes each tick
 * (stage C4 — replaces the deleted {@code ClientAbilityFilter}). A missing attachment
 * means "everything {@link Visibility#AVAILABLE}", the same as before any hero rule
 * applied, so HUDs behave identically while the answer is in flight.
 */
public final class ClientAbilityVisibility {
	private ClientAbilityVisibility() {
	}

	public static List<ResourceLocation> visibleFor(List<ResourceLocation> base) {
		Minecraft mc = Minecraft.getInstance();
		AbilityAvailability availability = mc.player == null ? null
				: mc.player.getAttached(ModAttachments.ABILITY_AVAILABILITY);
		if (availability == null || availability.entries().isEmpty()) {
			return new ArrayList<>(base);
		}
		ArrayList<ResourceLocation> out = new ArrayList<>(base.size());
		for (ResourceLocation id : base) {
			if (availability.visibilityOf(id) == Visibility.AVAILABLE) {
				out.add(id);
			}
		}
		return out;
	}

	public static List<ResourceLocation> visible() {
		return visibleFor(ClientHeroState.abilities());
	}
}
