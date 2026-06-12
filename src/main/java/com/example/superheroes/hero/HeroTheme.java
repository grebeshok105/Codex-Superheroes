package com.example.superheroes.hero;

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
	public static final HeroTheme HOMELANDER = new HeroTheme(
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

	public static final HeroTheme IRON_MAN = new HeroTheme(
			0xE03A0608,
			0xD01A0204,
			0x99FFD24A,
			0x44FFEEAA,
			0xFFFFE060,
			0xFF7A0000,
			0xFFFF3030,
			0x66FF6060,
			0xFFFF3838,
			0xFF8A4A00,
			0xFFFFC85A,
			0x66FFE090,
			0xFFFFC85A,
			0x66FF8A38,
			0xFFFFD24A,
			0xFFFFD24A,
			0xFFFFEFB0,
			0x66FF8A38
	);

	public static final HeroTheme GOKU = new HeroTheme(
			0xE0331504,
			0xD0140602,
			0x88E85D04,
			0x33FFCC88,
			0xFFFF9A4A,
			0xFF7A2900,
			0xFFE85D04,
			0x55FFAA66,
			0xFFFFAA44,
			0xFF1A0A00,
			0xFFFF7A28,
			0x55FF8844,
			0xFFFF7A28,
			0x55E85D04,
			0xFFFFAA44,
			0xFFFFAA44,
			0xFFFFE0B0,
			0x55FFAA66
	);

	public static final HeroTheme NARUTO = new HeroTheme(
			0xE0332B00,
			0xD0141000,
			0x88FFD60A,
			0x33FFE99A,
			0xFFFFE85A,
			0xFF7A6700,
			0xFFFFD60A,
			0x55FFEC8A,
			0xFFFFE060,
			0xFF1A1500,
			0xFFFFCC1A,
			0x55FFE070,
			0xFFFFCC1A,
			0x55FFD60A,
			0xFFFFE060,
			0xFFFFE060,
			0xFFFFF7C0,
			0x55FFE070
	);

	public static final HeroTheme CAPTAIN_AMERICA = new HeroTheme(
			0xE00A1E40,
			0xD0040A1A,
			0x881E40AF,
			0x33EF4444,
			0xFF60A0FF,
			0xFF0E2860,
			0xFF1E40AF,
			0x556090FF,
			0xFF60A0FF,
			0xFF1A0606,
			0xFFEF4444,
			0x55FF7878,
			0xFFEF4444,
			0x551E40AF,
			0xFFEF4444,
			0xFFEF4444,
			0xFFFFFFFF,
			0x556090FF
	);

	public static final HeroTheme KRATOS = new HeroTheme(
			0xE0200808,
			0xD00C0202,
			0x88AA1010,
			0x33FFB060,
			0xFFE03030,
			0xFF601010,
			0xFFE03030,
			0x55FF7060,
			0xFFE03030,
			0xFF1A0202,
			0xFFFFB060,
			0x55FFCC80,
			0xFFFFB060,
			0x55AA1010,
			0xFFFFB060,
			0xFFFFB060,
			0xFFFFFFFF,
			0x55FF7060
	);

	public static final HeroTheme LOKI = new HeroTheme(
			0xE0102008,
			0xD0040A02,
			0x881E8030,
			0x3360E060,
			0xFF60E060,
			0xFF1E5020,
			0xFF40C040,
			0x5560E060,
			0xFF60E060,
			0xFF200818,
			0xFFFFD030,
			0x55FFE070,
			0xFFFFD030,
			0x551E8030,
			0xFFFFD030,
			0xFFFFD030,
			0xFFFFFFFF,
			0x5560E060
	);

	public static final HeroTheme THANOS = new HeroTheme(
			0xE0140828,
			0xD0050210,
			0x88B44CFF,
			0x33FFD040,
			0xFFD58CFF,
			0xFF3A1668,
			0xFFB44CFF,
			0x55D58CFF,
			0xFFB44CFF,
			0xFF1A0608,
			0xFFFFAA40,
			0x55FFCC80,
			0xFFFFAA40,
			0x55B44CFF,
			0xFFFFD040,
			0xFFFFD040,
			0xFFFFFFFF,
			0x55D58CFF
	);

	public static final HeroTheme REINHARD = new HeroTheme(
			0xE02A0608,
			0xD0140204,
			0x99E62020,
			0x44FFB0B0,
			0xFFFF4848,
			0xFF6E0000,
			0xFFE61A1A,
			0x66FF6464,
			0xFFFF3030,
			0xFF1A0000,
			0xFFFF6060,
			0x66FFA0A0,
			0xFFFF6060,
			0x66E61A1A,
			0xFFFF4040,
			0xFFFF4040,
			0xFFFFE0E0,
			0x66FF6464
	);

	public static final HeroTheme RAIDEN = new HeroTheme(
			0xE01A0A2E,
			0xD00A0418,
			0x99A464FF,
			0x44E0CCFF,
			0xFFB890FF,
			0xFF3A1A88,
			0xFFA464FF,
			0x66C8A0FF,
			0xFFB890FF,
			0xFF1A0044,
			0xFFC8A0FF,
			0x66E0C8FF,
			0xFFC8A0FF,
			0x66A464FF,
			0xFFB890FF,
			0xFFB890FF,
			0xFFFFE8FF,
			0x66C8A0FF
	);

	public static final HeroTheme SCORPION = new HeroTheme(
			0xE01A0A02,
			0xD00A0301,
			0x88FF6A14,
			0x33FFB060,
			0xFFFF8A2A,
			0xFF6A1E00,
			0xFFFFB048,
			0x55FF8A30,
			0xFFFFB048,
			0xFF200800,
			0xFFFF9A30,
			0x55FFCC80,
			0xFFFF9A30,
			0x55FF6A14,
			0xFFFFB048,
			0xFFFFB048,
			0xFFFFF0E0,
			0x55FF8A30
	);

	public static final HeroTheme DEFAULT = HOMELANDER;
}
