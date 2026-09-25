package com.example.superheroes.client.screen;

import com.example.superheroes.client.config.SuperheroesClientConfig;
import com.example.superheroes.client.core.hud.HudBounds;
import com.example.superheroes.client.core.hud.HudLayers;
import com.example.superheroes.client.core.hud.MovableHud;
import com.example.superheroes.client.hud.HudLayoutManager;
import com.example.superheroes.client.hud.HudUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
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
	private NeonButton iconStyleBtn;

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

		// Переключатель формы иконок способностей: круглые <-> квадратные
		iconStyleBtn = new NeonButton(width / 2 - 90, height - 54, 180, 20,
				iconStyleLabel(), b -> {
					SuperheroesClientConfig.toggleIconStyle();
					iconStyleBtn.setMessage(iconStyleLabel());
				}, 0xFF8E7BFF, false);
		addRenderableWidget(iconStyleBtn);
	}

	private static Component iconStyleLabel() {
		boolean square = SuperheroesClientConfig.iconStyle() == SuperheroesClientConfig.IconStyle.SQUARE;
		return Component.translatable("hud.superheroes.edit.icon_style")
				.append(Component.literal(": "))
				.append(Component.translatable(square
						? "hud.superheroes.edit.icon_style.square"
						: "hud.superheroes.edit.icon_style.round"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		// Light dim only — the world stays crisp (blur is cancelled by GameRendererBlurMixin)
		graphics.fillGradient(0, 0, width, height, 0x55060410, 0x770A0616);

		hovered = elementAt(mouseX, mouseY);

		for (MovableHud m : orderedMovables()) {
			Element e = meta(m.layoutId());
			if (e == null) {
				continue;
			}
			HudBounds r = m.bounds(width, height);
			boolean hot = m.layoutId().equals(dragging) || (dragging == null && m.layoutId().equals(hovered));
			drawCard(graphics, e, r.x(), r.y(), r.width(), r.height(), hot);
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
		List<MovableHud> movables = orderedMovables();
		for (int i = movables.size() - 1; i >= 0; i--) {
			MovableHud m = movables.get(i);
			HudBounds r = m.bounds(width, height);
			if (mx >= r.x() && mx <= r.x() + r.width() && my >= r.y() && my <= r.y() + r.height()) {
				return m.layoutId();
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
				HudBounds r = bounds(id);
				grabDx = mx - r.x();
				grabDy = my - r.y();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
		if (dragging != null && button == 0) {
			HudBounds cur = bounds(dragging);
			int[] off = HudLayoutManager.offset(dragging);
			int defX = cur.x() - off[0];
			int defY = cur.y() - off[1];
			int nx = (int) Math.round(mx - grabDx) - defX;
			int ny = (int) Math.round(my - grabDy) - defY;
			// clamp so the card stays on screen
			nx = Math.max(-defX, Math.min(nx, width - cur.width() - defX));
			ny = Math.max(-defY, Math.min(ny, height - cur.height() - defY));
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

	/** Live bounds of a layout element, offset included — the movable's own math. */
	private HudBounds bounds(String id) {
		for (MovableHud m : HudLayers.movables()) {
			if (m.layoutId().equals(id)) {
				return m.bounds(width, height);
			}
		}
		return new HudBounds(0, 0, 10, 10);
	}

	/** Movable elements in the screen's card order (same relative order as the old list). */
	private static List<MovableHud> orderedMovables() {
		List<MovableHud> movables = new ArrayList<>(HudLayers.movables());
		movables.sort(Comparator.comparingInt(m -> elementIndex(m.layoutId())));
		return movables;
	}

	private static int elementIndex(String layoutId) {
		for (int i = 0; i < ELEMENTS.size(); i++) {
			if (ELEMENTS.get(i).id().equals(layoutId)) {
				return i;
			}
		}
		return ELEMENTS.size();
	}

	private static Element meta(String layoutId) {
		for (Element e : ELEMENTS) {
			if (e.id().equals(layoutId)) {
				return e;
			}
		}
		return null;
	}
}
