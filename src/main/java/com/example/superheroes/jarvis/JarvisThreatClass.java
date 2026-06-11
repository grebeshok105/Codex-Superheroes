package com.example.superheroes.jarvis;

import com.example.superheroes.hero.*;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public enum JarvisThreatClass {
	S("S", "§4", "ЗАПРЕДЕЛЬНАЯ УГРОЗА"),
	A("A", "§c", "ВЫСОКАЯ УГРОЗА"),
	B("B", "§6", "СРЕДНЯЯ УГРОЗА"),
	C("C", "§e", "НИЗКАЯ УГРОЗА"),
	D("D", "§a", "МИНИМАЛЬНАЯ УГРОЗА");

	private final String label;
	private final String colorCode;
	private final String russianDesc;

	JarvisThreatClass(String label, String colorCode, String russianDesc) {
		this.label = label;
		this.colorCode = colorCode;
		this.russianDesc = russianDesc;
	}

	public String label() {
		return label;
	}

	public String colorCode() {
		return colorCode;
	}

	public String russianDesc() {
		return russianDesc;
	}

	public boolean usesExcitedSound() {
		return this == S;
	}

	private static final Map<ResourceLocation, JarvisThreatClass> HERO_THREATS = Map.ofEntries(
			Map.entry(OmnimanHero.ID, S),
			Map.entry(ThanosHero.ID, S),
			Map.entry(DoomsdayHero.ID, S),
			Map.entry(BattleBeastHero.ID, S),
			Map.entry(ReinhardHero.ID, S),
			Map.entry(RegulusHero.ID, S),

			Map.entry(HomelanderHero.ID, A),
			Map.entry(InvincibleHero.ID, A),
			Map.entry(SungJinwooHero.ID, A),
			Map.entry(GokuHero.ID, A),

			Map.entry(IronManHero.ID, B),
			Map.entry(NarutoHero.ID, B),
			Map.entry(CaptainAmericaHero.ID, B),

			Map.entry(KratosHero.ID, C),
			Map.entry(RemHero.ID, C),
			Map.entry(RaidenHero.ID, C),
			Map.entry(ATrainHero.ID, C),

			Map.entry(KazuhaHero.ID, D),
			Map.entry(ScaramoucheHero.ID, D),
			Map.entry(LokiHero.ID, D)
	);

	public static JarvisThreatClass forHero(ResourceLocation heroId) {
		return HERO_THREATS.getOrDefault(heroId, C);
	}
}
