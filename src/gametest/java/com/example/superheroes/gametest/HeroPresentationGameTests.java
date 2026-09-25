package com.example.superheroes.gametest;

import com.example.superheroes.effect.HeroBleedingController;
import com.example.superheroes.effect.SuperJumpController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HeroPresentationGameTests implements FabricGameTest {
	/** One line per hero; every column is read at runtime, after all hero classes are initialized. */
	static String legacyLine(Hero hero) {
		var id = hero.getId();
		return id.getPath() + "|" + hero.getTheme() + "|" + hero.getHudConfig().energyName() + "|" + hero.getHudConfig().energyIcon()
				+ "|" + hero.getHudConfig().hasUltimate() + "|" + hero.getHudConfig().ultimateName()
				+ "|" + hero.getImpactStyle() + "|" + hero.getImpactPower() + "|" + hero.getThreatClass()
				+ "|" + SuperJumpController.isAllowed(id)
				+ "|" + HeroBleedingController.bleedFor(id, 1) + "|" + HeroBleedingController.bleedFor(id, 7);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void dumpLegacyPresentation(GameTestHelper helper) throws java.io.IOException {
		List<String> lines = new ArrayList<>();
		for (Hero hero : Heroes.all().values()) {
			lines.add(legacyLine(hero));
		}
		Path out = Path.of("build/golden/hero_presentation.txt");
		Files.createDirectories(out.getParent());
		Files.write(out, lines);
		helper.succeed();
	}
}
