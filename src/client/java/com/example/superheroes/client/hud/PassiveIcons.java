package com.example.superheroes.client.hud;

import com.example.superheroes.client.hud.HudIcons.PassiveGlyph;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps hero passives (heroId + passive index) to mini glyphs shown in the
 * hero info panel. Order matches the passive list in the lang files /
 * AbilityDescriptions.passiveCount().
 */
public final class PassiveIcons {
	private static final Map<String, PassiveGlyph[]> MAP = new HashMap<>();

	static {
		MAP.put("homelander", new PassiveGlyph[]{PassiveGlyph.HEART, PassiveGlyph.FLAME, PassiveGlyph.FEATHER});
		MAP.put("iron_man", new PassiveGlyph[]{PassiveGlyph.SHIELD, PassiveGlyph.FEATHER, PassiveGlyph.REACTOR});
		MAP.put("regulus", new PassiveGlyph[]{PassiveGlyph.HEART, PassiveGlyph.SHIELD, PassiveGlyph.STAR, PassiveGlyph.SKULL});
		MAP.put("sung_jinwoo", new PassiveGlyph[]{PassiveGlyph.SHADOW, PassiveGlyph.SHIELD, PassiveGlyph.EYE, PassiveGlyph.HEART});
		MAP.put("doomsday", new PassiveGlyph[]{PassiveGlyph.FIST, PassiveGlyph.STAR, PassiveGlyph.HEART, PassiveGlyph.BOLT, PassiveGlyph.SKULL});
		MAP.put("goku", new PassiveGlyph[]{PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.FEATHER});
		MAP.put("naruto", new PassiveGlyph[]{PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.SKULL});
		MAP.put("captain_america", new PassiveGlyph[]{PassiveGlyph.SHIELD, PassiveGlyph.FIST, PassiveGlyph.FEATHER});
		MAP.put("kratos", new PassiveGlyph[]{PassiveGlyph.FIST, PassiveGlyph.SWORD, PassiveGlyph.SHIELD, PassiveGlyph.BOLT});
		MAP.put("loki", new PassiveGlyph[]{PassiveGlyph.MAGIC, PassiveGlyph.FEATHER, PassiveGlyph.BOLT});
		MAP.put("thanos", new PassiveGlyph[]{PassiveGlyph.FIST, PassiveGlyph.SHIELD, PassiveGlyph.STAR, PassiveGlyph.COSMIC});
		MAP.put("reinhard", new PassiveGlyph[]{PassiveGlyph.FEATHER, PassiveGlyph.SHIELD, PassiveGlyph.STAR, PassiveGlyph.SWORD, PassiveGlyph.HEART, PassiveGlyph.BOLT});
		MAP.put("invincible", new PassiveGlyph[]{PassiveGlyph.SHIELD, PassiveGlyph.FIST, PassiveGlyph.FEATHER, PassiveGlyph.HEART});
		MAP.put("omniman", new PassiveGlyph[]{PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.FEATHER, PassiveGlyph.HEART});
		MAP.put("kazuha", new PassiveGlyph[]{PassiveGlyph.LEAF, PassiveGlyph.SWORD, PassiveGlyph.FEATHER});
		MAP.put("scaramouche", new PassiveGlyph[]{PassiveGlyph.SPIRAL, PassiveGlyph.FIST, PassiveGlyph.FEATHER});
		MAP.put("battle_beast", new PassiveGlyph[]{PassiveGlyph.BEAST, PassiveGlyph.FIST, PassiveGlyph.FLAME});
		MAP.put("rem", new PassiveGlyph[]{PassiveGlyph.ICE, PassiveGlyph.SKULL, PassiveGlyph.HEART});
		MAP.put("a_train", new PassiveGlyph[]{PassiveGlyph.BOLT, PassiveGlyph.HEART, PassiveGlyph.FIST});
	}

	private PassiveIcons() {
	}

	public static PassiveGlyph glyph(ResourceLocation heroId, int index) {
		PassiveGlyph[] glyphs = MAP.get(heroId.getPath());
		if (glyphs == null || index < 0 || index >= glyphs.length) {
			return PassiveGlyph.GENERIC;
		}
		return glyphs[index];
	}
}
