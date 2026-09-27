package io.github.grebeshok105.codex.client.hud;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.core.hud.HudLayers;
import io.github.grebeshok105.codex.client.core.input.ModKeys;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * The shared (hero-agnostic) HUD set: layer registrations in ascending z-order, the
 * per-layer tick hooks, and the tooltip-visibility toggle key. Hero-owned HUD pieces
 * register through {@code HeroClientContext.hud/movableHud} in their client modules.
 */
public final class CoreHuds {
	private CoreHuds() {
	}

	public static void init() {
		HudLayers.registerMovable(300, ModId.of("hero_panel"), HeroInfoPanelHud::render, HeroInfoPanelHud.INSTANCE);
		HudLayers.registerMovable(400, ModId.of("hotbar"), HotbarOverrideHud::render, HotbarOverrideHud.INSTANCE);
		HudLayers.registerMovable(500, ModId.of("ability_bar"), AbilityBarHud::render, AbilityBarHud.INSTANCE);
		// Vanilla chat and effect icons are shifted by mixins, not drawn by a mod layer:
		// movable-only entries so the HUD editor keeps all 7 cards.
		HudLayers.registerMovable(550, ModId.of("chat"), (graphics, delta) -> {
		}, ChatHudMovable.INSTANCE);
		HudLayers.registerMovable(560, ModId.of("effects"), (graphics, delta) -> {
		}, EffectsHudMovable.INSTANCE);
		HudLayers.register(700, ModId.of("radial_menu"), RadialMenuHud::render);
		HudLayers.register(800, ModId.of("screen_flash"), ScreenFlashHud::render);
		HudLayers.registerMovable(1800, ModId.of("tooltips"), AbilitiesTooltipHud::render, AbilitiesTooltipHud.INSTANCE);
		HudLayers.registerMovable(2100, ModId.of("melee_charge"), MeleeChargeHud::render, MeleeChargeHud.INSTANCE);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			HeroInfoPanelHud.tick();
			AbilityBarHud.tick();
			AbilitiesTooltipHud.tick();
			RadialMenuHud.animTick();
			RadialMenuHud.clientTick(client);
			while (ModKeys.TOGGLE_TOOLTIPS.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()) {
					AbilitiesTooltipHud.toggleVisible();
				}
			}
		});
	}
}
