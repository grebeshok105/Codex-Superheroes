package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientReinhardSwordKillState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Полноэкранный кровавый оверлей для жертвы меча Рейнхарда.
 * Показывается, пока сервер держит игрока в state «marked for death» (между ALLOW_DAMAGE-перехватом
 * и финальным добиванием в {@link com.example.superheroes.effect.ReinhardSwordDeathMarkController#flushDeaths}).
 *
 * Только для игроков — на мобах эффекта нет (server-side фильтр).
 */
public final class ReinhardSwordDeathOverlay {
	private static final long FADE_IN_MS = 220L;

	private ReinhardSwordDeathOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientReinhardSwordKillState.active()) return;
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		long elapsed = System.currentTimeMillis() - ClientReinhardSwordKillState.activatedAtMs();
		float fade = Math.min(1f, elapsed / (float) FADE_IN_MS);
		float pulse = 0.85f + 0.15f * (float) Math.sin(elapsed / 180.0);

		// Кровавая виньетка по краям, насыщенный красный к центру выцветает.
		int edgeAlpha = (int) (Math.min(255f, 230f * fade * pulse));
		int corner = (edgeAlpha << 24) | 0x6E0000;
		int center = ((int) (60 * fade) << 24) | 0xA00000;
		graphics.fillGradient(0, 0, w, h, corner, center);
		graphics.fillGradient(0, 0, w, h / 2, corner, 0x00000000);
		graphics.fillGradient(0, h / 2, w, h, 0x00000000, corner);

		// Тёмный полупрозрачный слой, чтобы текст хорошо читался.
		int overlayAlpha = (int) (90 * fade);
		graphics.fill(0, 0, w, h, (overlayAlpha << 24) | 0x000000);

		Font font = mc.font;
		Component title = Component.translatable("hud.superheroes.reinhard.death_mark.title")
				.withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED);
		Component subtitle = Component.translatable("hud.superheroes.reinhard.death_mark.subtitle")
				.withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);

		int titleAlpha = (int) (255 * fade);
		int subAlpha = (int) (220 * fade);
		int titleColor = (titleAlpha << 24) | 0xFFFFFF;
		int subColor = (subAlpha << 24) | 0xFFCFCF;

		int titleW = font.width(title);
		int subW = font.width(subtitle);
		// Используем pose stack для масштабирования заголовка.
		var pose = graphics.pose();
		pose.pushPose();
		float scale = 2.0f;
		pose.scale(scale, scale, 1f);
		graphics.drawString(font, title,
				(int) ((w - titleW * scale) / (2 * scale)),
				(int) ((h / 2f - 24) / scale),
				titleColor, true);
		pose.popPose();

		graphics.drawString(font, subtitle, (w - subW) / 2, h / 2 + 16, subColor, true);
	}
}
