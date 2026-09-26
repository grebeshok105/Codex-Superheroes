package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.effect.DoomsdayProgress;
import io.github.grebeshok105.codex.core.hero.BleedProfile;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.hero.PassiveGlyph;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Golden check for hero-owned presentation data: theme, HUD config, melee
 * impact, threat class, super jump, melee bleeding and passive glyphs must
 * match the snapshot taken from the legacy tables (B1.1). A hero without a
 * golden line fails the test, so a hook that silently defaults is caught.
 */
public final class HeroPresentationGameTests implements FabricGameTest {
	static String hookLine(Hero hero, @org.jetbrains.annotations.Nullable BleedProfile bleedTier1,
			@org.jetbrains.annotations.Nullable BleedProfile bleedTier7) {
		return hero.getId().getPath() + "|" + hero.getTheme() + "|" + hero.getHudConfig().energyName() + "|" + hero.getHudConfig().energyIcon()
				+ "|" + hero.getHudConfig().hasUltimate() + "|" + hero.getHudConfig().ultimateName()
				+ "|" + hero.getImpactStyle() + "|" + hero.getImpactPower() + "|" + hero.getThreatClass()
				+ "|" + hero.canSuperJump() + "|" + bleedTier1 + "|" + bleedTier7;
	}

	private static Map<String, String> goldenLines(String file) {
		String path = "/golden/" + file;
		try (InputStream in = HeroPresentationGameTests.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			Map<String, String> lines = new LinkedHashMap<>();
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
				if (line.isEmpty()) {
					continue;
				}
				lines.put(line.substring(0, line.indexOf('|')), line);
			}
			return lines;
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void presentationMatchesGolden(GameTestHelper helper) {
		Map<String, String> golden = goldenLines("hero_presentation.txt");
		List<String> problems = new ArrayList<>();
		for (Hero hero : Heroes.all().values()) {
			String expected = golden.get(hero.getId().getPath());
			if (expected == null) {
				problems.add(hero.getId() + " has no golden line");
				continue;
			}
			ServerPlayer player = TestPlayers.join(helper);
			TestHeroes.transform(player, hero.getId());
			BleedProfile bleedTier1;
			BleedProfile bleedTier7;
			if (DoomsdayHero.ID.equals(hero.getId())) {
				// Doomsday bleeds only from tier 3: capture the hook at min and max tier,
				// written the same way DoomsdayTierController writes it.
				player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, DoomsdayProgress.EMPTY.withTier(1));
				bleedTier1 = hero.getMeleeBleed(player);
				player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, DoomsdayProgress.EMPTY.withTier(7));
				bleedTier7 = hero.getMeleeBleed(player);
			} else {
				bleedTier1 = hero.getMeleeBleed(player);
				bleedTier7 = bleedTier1;
			}
			String actual = hookLine(hero, bleedTier1, bleedTier7);
			if (!expected.equals(actual)) {
				problems.add(hero.getId() + " mismatch:\n  golden: " + expected + "\n  actual: " + actual);
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("\n", problems));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void passiveGlyphsMatchGolden(GameTestHelper helper) {
		Map<String, String> golden = goldenLines("passive_glyphs.txt");
		List<String> problems = new ArrayList<>();
		for (Hero hero : Heroes.all().values()) {
			String expected = golden.get(hero.getId().getPath());
			if (expected == null) {
				problems.add(hero.getId() + " has no golden line");
				continue;
			}
			String actual = hero.getId().getPath() + "|" + hero.getPassiveGlyphs().stream()
					.map(PassiveGlyph::name).collect(Collectors.joining(","));
			if (!expected.equals(actual)) {
				problems.add(hero.getId() + " mismatch:\n  golden: " + expected + "\n  actual: " + actual);
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("\n", problems));
		helper.succeed();
	}
}
