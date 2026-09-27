package io.github.grebeshok105.codex.client.hud;

import io.github.grebeshok105.codex.client.core.hud.HudBounds;
import io.github.grebeshok105.codex.client.core.hud.MovableHud;

/**
 * Editor-side movable for the vanilla status-effect icons, which
 * {@code GuiEffectsMixin} shifts by the layout offset. No render layer of its
 * own — registered with a no-op layer so it exists in {@code HudLayers.movables()}.
 */
public final class EffectsHudMovable implements MovableHud {
	public static final EffectsHudMovable INSTANCE = new EffectsHudMovable();

	private EffectsHudMovable() {
	}

	@Override
	public String layoutId() {
		return HudLayoutManager.EFFECTS;
	}

	@Override
	public HudBounds bounds(int screenWidth, int screenHeight) {
		int[] off = HudLayoutManager.offset(HudLayoutManager.EFFECTS);
		return new HudBounds(screenWidth - 130 + off[0], 4 + off[1], 126, 60);
	}
}
