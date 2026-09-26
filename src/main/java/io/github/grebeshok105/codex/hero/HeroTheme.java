package io.github.grebeshok105.codex.hero;

public record HeroTheme(
		int panelTop,
		int panelBottom,
		int panelBorder,
		int panelHighlight,
		int heroNameColor,
		int energyDark,
		int energyBright,
		int energyGlow,
		int energyIcon,
		int manaDark,
		int manaBright,
		int manaGlow,
		int manaIcon,
		int radialBorderIdle,
		int radialBorderActive,
		int radialKeyActive,
		int radialTextActive,
		int radialGlow
) {
	/** Neutral fallback for no hero (Homelander's palette). */
	public static final HeroTheme DEFAULT = new HeroTheme(
			0xE0181C2A,
			0xD0080A14,
			0x88FFD27A,
			0x33FFFFFF,
			0xFFFFE07A,
			0xFFB35900,
			0xFFFFD060,
			0x55FFE08A,
			0xFFFFC538,
			0xFF3B1F8A,
			0xFFB58CFF,
			0x55C7A8FF,
			0xFFB58CFF,
			0x55FFD27A,
			0xFFFFC538,
			0xFFFFC538,
			0xFFFFF1B0,
			0x55FFD27A
	);
}
