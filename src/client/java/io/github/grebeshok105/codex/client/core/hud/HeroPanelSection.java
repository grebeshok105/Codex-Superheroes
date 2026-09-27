package io.github.grebeshok105.codex.client.core.hud;

import io.github.grebeshok105.codex.core.hero.HeroTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * A hero-owned block drawn inside {@link io.github.grebeshok105.codex.client.hud.HeroInfoPanelHud}
 * instead of the default HP/energy rows — e.g. Iron Man's arc-reactor status.
 * The hero module registers one instance; absence means the default rows draw.
 */
public interface HeroPanelSection {
	/** When true the whole hero panel renders only in first person. */
	boolean firstPersonOnly();

	/** Vertical space the section consumes, in unscaled px (HudScaler is applied by the host). */
	int heightPx();

	void draw(GuiGraphics graphics, Minecraft mc, int x, int y, int w, HeroTheme theme, Player player);
}
