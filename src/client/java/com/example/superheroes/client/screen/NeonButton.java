package com.example.superheroes.client.screen;

import com.example.superheroes.client.hud.HudUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Dark glass button with a soft neon accent border. Optionally draws a small
 * 2x2 "layout grid" icon to the left of the label (used for the HUD editor
 * entry in the pause menu).
 */
public class NeonButton extends Button {
	private final int accent;
	private final boolean gridIcon;

	public NeonButton(int x, int y, int w, int h, Component message, OnPress onPress, int accent, boolean gridIcon) {
		super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
		this.accent = accent;
		this.gridIcon = gridIcon;
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();
		boolean hot = isHoveredOrFocused() && this.active;

		if (hot) {
			HudUtil.roundedRectFill(g, x - 2, y - 2, w + 4, h + 4, (0x2E << 24) | (accent & 0x00FFFFFF));
		}
		HudUtil.dropShadow(g, x, y, w, h, 2, 0x44000000);
		HudUtil.roundedRectGradient(g, x, y, w, h,
				hot ? 0xF02A2236 : 0xE61E1826, hot ? 0xF0181221 : 0xD912101A);
		HudUtil.roundedRectBorder(g, x, y, w, h,
				hot ? accent : ((0x77 << 24) | (accent & 0x00FFFFFF)));
		g.fill(x + 3, y + 1, x + w - 3, y + 2, 0x22FFFFFF);

		Minecraft mc = Minecraft.getInstance();
		int textColor = this.active ? (hot ? 0xFFFFFFFF : 0xFFE8E4F2) : 0xFF8A8FA0;
		int labelW = mc.font.width(getMessage());
		int iconSpace = gridIcon ? 11 : 0;
		int contentX = x + (w - labelW - iconSpace) / 2;
		if (gridIcon) {
			drawGridIcon(g, contentX, y + (h - 7) / 2, hot ? accent : ((0xCC << 24) | (accent & 0x00FFFFFF)));
			contentX += iconSpace;
		}
		g.drawString(mc.font, getMessage(), contentX, y + (h - 8) / 2, textColor, true);
	}

	/** Tiny 2x2 layout grid: three filled cells + one accent cell. */
	private static void drawGridIcon(GuiGraphics g, int x, int y, int accent) {
		int c = 3;
		int gap = 1;
		g.fill(x, y, x + c, y + c, 0xCCDDDDE8);
		g.fill(x + c + gap, y, x + c + gap + c, y + c, 0xCCDDDDE8);
		g.fill(x, y + c + gap, x + c, y + c + gap + c, 0xCCDDDDE8);
		g.fill(x + c + gap, y + c + gap, x + c + gap + c, y + c + gap + c, accent);
	}
}
