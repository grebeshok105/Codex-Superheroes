package io.github.grebeshok105.codex.client.core.input;

import io.github.grebeshok105.codex.client.ClientAbilityVisibility;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.core.net.ActivateAbilityC2SPayload;
import io.github.grebeshok105.codex.core.net.SuperJumpC2SPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Keybind → C2S send loops for the shared (hero-agnostic) actions: super jump and the
 * eight ability-slot keys. Runs on END_CLIENT_TICK.
 */
public final class InputSenders {
	private InputSenders() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ModKeys.SUPER_JUMP.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(SuperJumpC2SPayload.INSTANCE);
				}
			}
			// Raw GLFW polling: vanilla KeyMapping.MAP allows one mapping per key, so our
			// L / 3 / 4 / 5 binds conflict with vanilla and consumeClick() is unreliable.
			for (int i = 0; i < ModKeys.ABILITY_SLOTS.length; i++) {
				RawKeys.drain(ModKeys.ABILITY_SLOTS[i]);
				// Ability keys always fire; 3/4/5 also switch hotbar slots — intended.
				if (RawKeys.pressed(ModKeys.ABILITY_SLOTS[i])
						&& client.player != null && ClientHeroState.data().hasHero()) {
					List<ResourceLocation> abilities = ClientAbilityVisibility.visible();
					if (i < abilities.size()) {
						ClientPlayNetworking.send(new ActivateAbilityC2SPayload(abilities.get(i)));
					}
				}
			}
		});
	}
}
