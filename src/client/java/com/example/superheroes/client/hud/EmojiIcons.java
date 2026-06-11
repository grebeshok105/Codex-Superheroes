package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * iOS-стиль эмодзи (официальные Apple-ассеты 64x64 в assets/superheroes/textures/gui/emoji/,
 * извлечены из шрифта Apple Color Emoji — samuelngs/apple-emoji-ttf) для худа: пассивки и
 * иконки статов рисуются настоящими эмодзи вместо векторных глифов. В текстурах гарантирован
 * прозрачный отступ 3px по краям, чтобы линейная фильтрация не «резала» и не грязнила края.
 */
public final class EmojiIcons {
	private static final int TEX_SIZE = 64;
	private static final Map<HudIcons.PassiveGlyph, ResourceLocation> CACHE =
			new EnumMap<>(HudIcons.PassiveGlyph.class);

	private EmojiIcons() {
	}

	public static ResourceLocation texture(HudIcons.PassiveGlyph glyph) {
		return CACHE.computeIfAbsent(glyph, g -> {
			ResourceLocation loc = ModId.of("textures/gui/emoji/" + g.name().toLowerCase(Locale.ROOT) + ".png");
			// плавное масштабирование вместо nearest — иначе эмодзи выглядят обрезанными
			net.minecraft.client.Minecraft.getInstance().getTextureManager()
					.getTexture(loc).setFilter(true, false);
			return loc;
		});
	}

	public static void draw(GuiGraphics g, HudIcons.PassiveGlyph glyph, int x, int y, int size) {
		g.blit(texture(glyph), x, y, size, size, 0f, 0f, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
	}

	/** Эмодзи с лёгкой неоновой подсветкой позади (мягкий шейдерный орб). */
	public static void drawWithGlow(GuiGraphics g, HudIcons.PassiveGlyph glyph, int x, int y, int size,
			int glowColor) {
		if (WildShaders.rectReady()) {
			float cx = x + size / 2f;
			float cy = y + size / 2f;
			WildRenderer.orb(g, cx, cy, size * 0.34f, 0, 0, 0f, glowColor, size * 0.65f);
		}
		draw(g, glyph, x, y, size);
	}
}
