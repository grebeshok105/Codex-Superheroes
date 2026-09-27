package io.github.grebeshok105.codex.client.screen;

import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.core.input.ModKeys;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

/**
 * Screen wiring: the keybind-driven openers ({@code BINDINGS} → {@link BindingsScreen},
 * {@code VFX_SETTINGS} → {@link VfxSettingsScreen}) and the "HUD" button on the pause menu
 * that opens the {@link HudEditScreen} drag editor.
 */
public final class ModScreens {
	private ModScreens() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ModKeys.BINDINGS.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()) {
					client.setScreen(new BindingsScreen());
				}
			}
			while (ModKeys.VFX_SETTINGS.consumeClick()) {
				if (client.player != null) {
					client.setScreen(new VfxSettingsScreen());
				}
			}
		});

		// "HUD" button in the pause menu -> drag editor for all HUD elements
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof PauseScreen) {
				Screens.getButtons(screen).add(
						new NeonButton(scaledWidth - 92, 8, 84, 20,
								Component.translatable("hud.superheroes.edit.open"),
								b -> client.setScreen(new HudEditScreen()),
								0xFF8E7BFF, true));
			}
		});
	}
}
