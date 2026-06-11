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
 * iOS-стиль эмодзи (48x48 PNG в assets/superheroes/textures/gui/emoji/) для худа:
 * пассивки и иконки статов рисуются настоящими эмодзи вместо векторных глифов.
 */
public final class EmojiIcons {
	private static final int TEX_SIZE = 48;
	private static final Map<HudIcons.PassiveGlyph, ResourceLocation> CACHE =
			new EnumMap<>(HudIcons.PassiveGlyph.class);

	private EmojiIcons() {
	}

	public static ResourceLocation texture(HudIcons.PassiveGlyph glyph) {
		return CACHE.computeIfAbsent(glyph,
				g -> ModId.of("textures/gui/emoji/" + g.name().toLowerCase(Locale.ROOT) + ".png"));
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
