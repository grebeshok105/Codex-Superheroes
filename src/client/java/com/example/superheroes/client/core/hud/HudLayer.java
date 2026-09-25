package com.example.superheroes.client.core.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface HudLayer {
	void render(GuiGraphics graphics, DeltaTracker delta);
}
