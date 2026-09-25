package com.example.superheroes.client;

import com.example.superheroes.client.hud.AbilityDescriptions;
import com.example.superheroes.client.hud.PassiveIcons;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class PassiveGlyphSnapshotTest {
	private static final List<String> HEROES = List.of("homelander", "iron_man", "regulus", "sung_jinwoo", "doomsday", "goku",
			"naruto", "captain_america", "kratos", "loki", "thanos", "reinhard", "raiden_shogun", "invincible", "omniman", "kazuha",
			"scaramouche", "battle_beast", "rem", "a_train", "scorpion", "pandora");

	@Test
	void dumpPassiveGlyphs() throws java.io.IOException {
		List<String> lines = new ArrayList<>();
		for (String path : HEROES) {
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath("superheroes", path);
			int count = AbilityDescriptions.passiveCount(id);
			List<String> glyphs = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				glyphs.add(PassiveIcons.glyph(id, i).name());
			}
			lines.add(path + "|" + String.join(",", glyphs));
		}
		Path out = Path.of("build/golden/passive_glyphs.txt");
		Files.createDirectories(out.getParent());
		Files.write(out, lines);
	}
}
