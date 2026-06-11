package com.example.superheroes.client.screen;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.hud.HudLayoutManager;
import com.example.superheroes.client.hud.HudScaler;
import com.example.superheroes.client.hud.HudUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Drag editor for every HUD element (hero panel, hotbar, ability bar, chat,
 * status effects, melee charge gauge). Opened from the pause menu.
 * Elements are shown as dark glass cards with neon accent chips
 * (wildclient-style); positions persist via HudLayoutManager.
 */
public class HudEditScreen extends Screen {
	private record Element(String id, String labelKey, int accent) {
	}

	private static final List<Element> ELEMENTS = List.of(
			new Element(HudLayoutManager.HERO_PANEL, "hud.superheroes.edit.hero_panel", 0xFFFF7A6B),
			new Element(HudLayoutManager.HOTBAR, "hud.superheroes.edit.hotbar", 0xFFFFE76B),
			new Element(HudLayoutManager.ABILITY_BAR, "hud.superheroes.edit.ability_bar", 0xFF8E7BFF),
			new Element(HudLayoutManager.CHAT, "hud.superheroes.edit.chat", 0xFF6BFFB4),
			new Element(HudLayoutManager.EFFECTS, "hud.superheroes.edit.effects", 0xFF4ADBD2),
			new Element(HudLayoutManager.MELEE_CHARGE, "hud.superheroes.edit.melee_charge", 0xFFFF8BD8),
			new Element(HudLayoutManager.TOOLTIPS, "hud.superheroes.edit.tooltips", 0xFF6BD9FF));

	private String dragging = null;
	private double grabDx;
	private double grabDy;
	private String hovered = null;

	public HudEditScreen() {
		super(Component.translatable("hud.superheroes.edit.title"));
	}

	@Override
	protected void init() {
		int bw = 90;
		addRenderableWidget(new NeonButton(width / 2 - bw - 4, height - 28, bw, 20,
				Component.translatable("hud.superheroes.edit.reset"),
				b -> HudLayoutManager.resetAll(), 0xFFFF7A6B, false));
		addRenderableWidget(new NeonButton(width / 2 + 4, height - 28, bw, 20,
				Component.translatable("hud.superheroes.edit.done"),
				b -> onClose(), 0xFF6BFF8C, false));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		// Light dim only — the world stays crisp (blur is cancelled by GameRendererBlurMixin)
		graphics.fillGradient(0, 0, width, height, 0x55060410, 0x770A0616);

		hovered = elementAt(mouseX, mouseY);

		for (Element e : ELEMENTS) {
			int[] r = bounds(e.id());
			boolean hot = e.id().equals(dragging) || (dragging == null && e.id().equals(hovered));
			drawCard(graphics, e, r[0], r[1], r[2], r[3], hot);
		}

		// Title + hint
		graphics.drawCenteredString(font, title, width / 2, 14, 0xFFF2F3F8);
		graphics.drawCenteredString(font, Component.translatable("hud.superheroes.edit.hint"),
				width / 2, 28, 0x99A8AEC2);

		super.render(graphics, mouseX, mouseY, partial);
	}

	private void drawCard(GuiGraphics g, Element e, int x, int y, int w, int h, boolean hot) {
		int accent = e.accent();
		// outer neon glow
		if (hot) {
			HudUtil.roundedRectFill(g, x - 3, y - 3, w + 6, h + 6, (0x33 << 24) | (accent & 0x00FFFFFF));
		}
		HudUtil.dropShadow(g, x, y, w, h, 3, 0x55000000);
		// dark warm glass like the reference
		HudUtil.roundedRectGradient(g, x, y, w, h, hot ? 0xF02A2030 : 0xE01E1722, hot ? 0xF01A1420 : 0xD0141016);
		HudUtil.roundedRectBorder(g, x, y, w, h, hot ? accent : ((0x66 << 24) | (accent & 0x00FFFFFF)));
		g.fill(x + 3, y + 1, x + w - 3, y + 2, 0x26FFFFFF);

		// label + neon chip (chip on the right, like the theme cards)
		Component label = Component.translatable(e.labelKey());
		int chipW = Math.min(28, w / 4);
		int chipH = 10;
		if (w >= 70 && h >= 16) {
			g.drawString(font, label, x + 8, y + (h - 8) / 2, 0xFFEFE9F2, true);
			int chipX = x + w - chipW - 8;
			int chipY = y + (h - chipH) / 2;
			HudUtil.roundedRectFill(g, chipX, chipY, chipW, chipH, accent);
			HudUtil.roundedRectFill(g, chipX + 2, chipY + 1, chipW - 4, 3, 0x55FFFFFF);
		} else {
			// tiny elements: label above the card
			int lw = font.width(label);
			g.drawString(font, label, x + (w - lw) / 2, y - 11, 0xFFEFE9F2, true);
			HudUtil.roundedRectFill(g, x + 2, y + 2, Math.max(4, w - 4), 3, accent);
		}
	}

	private String elementAt(double mx, double my) {
		// topmost = last in list order; iterate reversed so small cards win
		for (int i = ELEMENTS.size() - 1; i >= 0; i--) {
			Element e = ELEMENTS.get(i);
			int[] r = bounds(e.id());
			if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
				return e.id();
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0) {
			String id = elementAt(mx, my);
			if (id != null) {
				dragging = id;
				int[] r = bounds(id);
				grabDx = mx - r[0];
				grabDy = my - r[1];
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
		if (dragging != null && button == 0) {
			int[] cur = bounds(dragging);
			int[] off = HudLayoutManager.offset(dragging);
			int defX = cur[0] - off[0];
			int defY = cur[1] - off[1];
			int nx = (int) Math.round(mx - grabDx) - defX;
			int ny = (int) Math.round(my - grabDy) - defY;
			// clamp so the card stays on screen
			nx = Math.max(-defX, Math.min(nx, width - cur[2] - defX));
			ny = Math.max(-defY, Math.min(ny, height - cur[3] - defY));
			HudLayoutManager.setOffset(dragging, nx, ny);
			return true;
		}
		return super.mouseDragged(mx, my, button, ddx, ddy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (dragging != null && button == 0) {
			dragging = null;
			HudLayoutManager.save();
			return true;
		}
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public void onClose() {
		HudLayoutManager.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Live bounds {x, y, w, h} of each element, offset included. Mirrors the HUD math. */
	private int[] bounds(String id) {
		Minecraft mc = Minecraft.getInstance();
		int sw = width;
		int sh = height;
		int margin = HudScaler.scale(8);
		int panelW = com.example.superheroes.client.hud.HeroInfoPanelHud.panelWidth();
		int panelH = HudScaler.scale(142);
		int[] off = HudLayoutManager.offset(id);
		switch (id) {
			case HudLayoutManager.HERO_PANEL -> {
				return new int[]{margin + off[0], sh - panelH - margin + off[1], panelW, panelH};
			}
			case HudLayoutManager.HOTBAR -> {
				int slot = HudScaler.scale(18);
				int gap = HudScaler.scale(1);
				int w = 9 * (slot + gap) + HudScaler.scale(46);
				int y = sh - panelH - margin - slot - HudScaler.scale(4) + off[1];
				return new int[]{margin + off[0], y, w, slot};
			}
			case HudLayoutManager.ABILITY_BAR -> {
				int n = ClientHeroState.data().hasHero() ? Math.max(1, ClientHeroState.abilities().size()) : 6;
				int slotSize = HudScaler.scale(Math.max(28, Math.min(44, 36 + (6 - n) * 2)));
				int gap = HudScaler.scale(4);
				int w = n * slotSize + (n - 1) * gap;
				int y = sh - HudScaler.scale(40) - slotSize + off[1];
				return new int[]{(sw - w) / 2 + off[0], y, w, slotSize + HudScaler.scale(12)};
			}
			case HudLayoutManager.CHAT -> {
				int autoLift = ClientHeroState.data().hasHero() ? -HudScaler.scale(104) : 0;
				int w = (int) (mc.options.chatWidth().get() * 280) + 40;
				int h = 120;
				return new int[]{2 + off[0], sh - 48 - h + autoLift + off[1], w, h};
			}
			case HudLayoutManager.EFFECTS -> {
				return new int[]{sw - 130 + off[0], 4 + off[1], 126, 60};
			}
			case HudLayoutManager.MELEE_CHARGE -> {
				return new int[]{sw / 2 + 10 + off[0], sh / 2 - 9 + off[1], 14, 24};
			}
			case HudLayoutManager.TOOLTIPS -> {
				// mirrors AbilitiesTooltipHud (raw gui px: right edge, below effects)
				int w = 250;
				int h = 140;
				return new int[]{sw - w - 10 + off[0], 70 + off[1], w, h};
			}
			default -> {
				return new int[]{0, 0, 10, 10};
			}
		}
	}
}
