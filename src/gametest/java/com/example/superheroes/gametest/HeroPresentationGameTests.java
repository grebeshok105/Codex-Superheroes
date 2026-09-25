package com.example.superheroes.gametest;

import com.example.superheroes.effect.HeroBleedingController;
import com.example.superheroes.effect.SuperJumpController;
import com.example.superheroes.hero.Hero;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

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
}
