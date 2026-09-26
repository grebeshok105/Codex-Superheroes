package io.github.grebeshok105.codex.client.core.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Extra drawing around an ability's radial-menu icon (a ready halo, a badge, …). Registered per ability id
 * via {@code HeroClientContext.abilityDecoration}; {@link #render} runs every frame the slot is drawn and
 * no-ops while the decoration's own condition isn't met. {@code iconCenterX}/{@code iconCenterY} are the
 * slot's center and {@code iconSize} its edge length in gui-scaled pixels, inside the wheel's pose transform.
 */
@FunctionalInterface
public interface AbilityDecoration {
	void render(GuiGraphics graphics, ResourceLocation abilityId, int iconCenterX, int iconCenterY, int iconSize);
}
