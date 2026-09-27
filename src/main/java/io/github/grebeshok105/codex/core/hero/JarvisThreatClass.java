package io.github.grebeshok105.codex.core.hero;

import net.minecraft.resources.ResourceLocation;

/**
 * J.A.R.V.I.S. threat classification for heroes, declared by each hero via
 * {@link Hero#getThreatClass()} (audit debt 4 — the lookup table used to import every
 * hero into one map here).
 */
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

	public static JarvisThreatClass forHero(ResourceLocation heroId) {
		Hero hero = Heroes.get(heroId);
		return hero != null ? hero.getThreatClass() : C;
	}
}
