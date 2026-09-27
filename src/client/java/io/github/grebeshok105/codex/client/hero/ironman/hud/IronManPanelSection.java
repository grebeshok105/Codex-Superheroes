package io.github.grebeshok105.codex.client.hero.ironman.hud;

import io.github.grebeshok105.codex.client.core.hud.HeroPanelSection;
import io.github.grebeshok105.codex.client.hud.HudAnimator;
import io.github.grebeshok105.codex.client.hud.HudScaler;
import io.github.grebeshok105.codex.client.hud.HudUtil;
import io.github.grebeshok105.codex.client.render.WildRenderer;
import io.github.grebeshok105.codex.client.render.WildShaders;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Железный Человек: HP/энергия живут в J.A.R.V.I.S.-оверлее, поэтому тут вместо
 * полосок — тематический статус дуг-реактора. Панель показывается только от
 * 1-го лица; в 3-м лице (F5) убираем.
 */
public final class IronManPanelSection implements HeroPanelSection {
	@Override
	public boolean firstPersonOnly() {
		return true;
	}

	@Override
	public int heightPx() {
		return 29;
	}

	@Override
	public void draw(GuiGraphics g, Minecraft mc, int x, int y, int w, HeroTheme theme, Player player) {
		int cyan = 0xFF46D8FF;
		// строка 1: эмодзи-реактор убран по запросу — лейаут не сдвигаем, остаётся
		// только «online» точка справа. «ARMOR ONLINE» надпись тоже убрана ранее.
		// «online» точка справа
		int dotSz = HudScaler.scale(4);
		int dotX = x + w - dotSz - HudScaler.scale(2);
		int dotY = y + HudScaler.scale(1);
		float pulse = HudAnimator.pulse(1.3f);
		if (WildShaders.rectReady()) {
			WildRenderer.fill(g, dotX - 1, dotY - 1, dotSz + 2, dotSz + 2, (dotSz + 2) / 2f,
					applyAlpha(0xFF4CFF8C, (int) (60 + 80 * pulse), 1f));
			WildRenderer.fill(g, dotX, dotY, dotSz, dotSz, dotSz / 2f, 0xFF4CFF8C);
		} else {
			g.fill(dotX, dotY, dotX + dotSz, dotY + dotSz, 0xFF4CFF8C);
		}

		// строка 2: лейбл ARC REACTOR + анимированная полоска мощности
		int row2 = y + HudScaler.scale(14);
		Component label = HudUtil.text("ARC REACTOR");
		g.drawString(mc.font, label, x, row2, 0xFF8FB7C8, true);
		int barX = x + mc.font.width(label) + HudScaler.scale(6);
		int barW = x + w - barX;
		int barH = HudScaler.scale(4);
		int barY = row2 + HudScaler.scale(1);
		float power = 0.78f + 0.18f * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 240.0));
		if (barW > 4) {
			if (WildShaders.rectReady()) {
				WildRenderer.panel(g, barX, barY, barW, barH, barH / 2f, 0x70000000, 0xA8000000, 0, 0f, 0, 0f);
				WildRenderer.panel(g, barX, barY, (int) (barW * power), barY + barH, barH / 2f,
						cyan, 0xFF1C6E8C, 0, 0f, applyAlpha(cyan, 90, 1f), 6f);
			} else {
				g.fill(barX, barY, barX + barW, barY + barH, 0x99000000);
				g.fillGradient(barX, barY, barX + (int) (barW * power), barY + barH, cyan, 0xFF1C6E8C);
			}
		}
	}

	private static int applyAlpha(int argb, int alpha, float mult) {
		int originalA = (argb >>> 24) & 0xFF;
		int finalA = Math.min(255, Math.max(0, (int) (originalA * (alpha / 255f) * mult)));
		return (finalA << 24) | (argb & 0x00FFFFFF);
	}
}
