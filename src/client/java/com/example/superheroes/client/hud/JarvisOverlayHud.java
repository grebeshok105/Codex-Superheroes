package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.ironman.IronManSuitVariant;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientNanoWeaponState;
import com.example.superheroes.client.ClientRepulsorChargeState;
import com.example.superheroes.client.ClientSuitVariantState;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
import com.example.superheroes.hero.IronManHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * HUD Железного Человека от 1-го лица «Mark HUD». Полностью на SDF-шейдерах
 * ({@link WildRenderer}): арк-реактор-прицел, левый блок энергии и заряда
 * репульсоров (дуговые гейджи), правый HP-блок сегментными полосами, нижний
 * селектор костюмов и нано-оружия, верхний баннер J.A.R.V.I.S. и панель
 * окружения (время/погода/градусы). Всё анимировано: вращение колец,
 * пульсация, плавный лёрп значений, intro-анимация при надевании костюма.
 */
public final class JarvisOverlayHud {
	// ---- палитра ----
	private static final int GOLD = 0xFFC400;
	private static final int GOLD_DIM = 0x8A6A00;
	private static final int RED = 0xE2342B;
	private static final int CYAN = 0x46D8FF;
	private static final int WHITE = 0xFFF4D6;
	private static final int DARK = 0x0A0E14;

	// ---- анимационное состояние ----
	private static long bootMs = 0L;
	private static long lastFrameMs = 0L;
	private static float dispEnergy = 0f;
	private static float dispHp = 0f;
	private static float dispCharge = 0f;
	private static float dispSuit = 0f;
	private static float dispWeapon = 0f;

	private JarvisOverlayHud() {
	}

	public static void render(GuiGraphics g, DeltaTracker tracker) {
		if (!ClientHeroState.data().hasHero()) {
			bootMs = 0L;
			return;
		}
		ResourceLocation heroId = ClientHeroState.data().heroId();
		if (!IronManHero.ID.equals(heroId)) {
			bootMs = 0L;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) {
			return;
		}
		// Шейдеры — основа этого HUD; без них откатываемся на старую логику не нужно
		// (другие HUD-ы тоже требуют шейдеры), просто молча выходим.
		if (!WildShaders.circleReady() || !WildShaders.rectReady()) {
			return;
		}

		long now = System.currentTimeMillis();
		if (bootMs == 0L) {
			bootMs = now;
			// мгновенно подхватываем реальные значения, чтобы intro не «догонял» с нуля скачком
			dispEnergy = clamp01(ClientHeroState.data().energy() / Math.max(1f, ClientHeroState.energyMax()));
			dispHp = clamp01(player.getHealth() / Math.max(1f, player.getMaxHealth()));
		}
		float boot = ease(clamp01((now - bootMs) / 650f));
		float dt = lastFrameMs == 0L ? 0.016f : Math.min(0.1f, (now - lastFrameMs) / 1000f);
		lastFrameMs = now;

		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		float cx = sw / 2f;
		float cy = sh / 2f;
		float tt = now / 1000f;

		// ---- данные ----
		float energy = clamp01(ClientHeroState.data().energy() / Math.max(1f, ClientHeroState.energyMax()));
		float hp = clamp01(player.getHealth() / Math.max(1f, player.getMaxHealth()));
		float charge = ClientRepulsorChargeState.charge();
		int suitIdx = ClientSuitVariantState.variantFor(player.getUUID());
		int weaponIdx = ClientNanoWeaponState.selectedIndex();

		// плавный лёрп отображаемых значений
		float k = 1f - (float) Math.exp(-dt / 0.12f);
		dispEnergy += (energy - dispEnergy) * k;
		dispHp += (hp - dispHp) * k;
		dispCharge += (charge - dispCharge) * Math.min(1f, dt / 0.05f);
		dispSuit += (suitIdx - dispSuit) * (1f - (float) Math.exp(-dt / 0.10f));
		dispWeapon += (weaponIdx - dispWeapon) * (1f - (float) Math.exp(-dt / 0.10f));

		Font font = mc.font;
		drawCorners(g, sw, sh, tt, boot);
		drawTopBanner(g, font, sw, tt, boot);
		drawCenterReticle(g, cx, cy, tt, dispCharge, boot);
		drawLeftGauges(g, font, sh, tt, dispEnergy, dispCharge, boot);
		drawRightHealth(g, font, sw, sh, dispHp, tt, boot);
		drawSuitStrip(g, font, cx, sh, suitIdx, boot, tt);
		drawWeaponStrip(g, font, cx, sh, weaponIdx, boot, tt);
		drawEnvironment(g, font, sw, sh, player.level(), player, boot);
	}

	// ===================== центр: арк-реактор =====================
	private static void drawCenterReticle(GuiGraphics g, float cx, float cy, float tt, float charge, float boot) {
		float a = boot;
		float pulse = 0.5f + 0.5f * (float) Math.sin(tt * 2.2f);
		int gold = col(GOLD, 0.85f * a);
		int goldSoft = col(GOLD, 0.35f * a);
		int cyan = col(CYAN, (0.5f + 0.4f * pulse) * a);
		// внешнее вращающееся кольцо-сектора
		float rOuter = 19f;
		float rot = tt * 0.6f;
		for (int i = 0; i < 4; i++) {
			float a0 = rot + i * (float) (Math.PI / 2.0);
			WildRenderer.arc(g, cx, cy, rOuter, 1.6f, a0 + 0.15f, a0 + 1.20f,
					0, gold, col(GOLD, 0.25f * a), 4f);
		}
		// тонкое полное кольцо
		WildRenderer.ring(g, cx, cy, 13.5f, 1.0f, goldSoft, 0, 0f);
		// внутреннее контр-вращающееся
		float rot2 = -tt * 1.1f;
		for (int i = 0; i < 3; i++) {
			float a0 = rot2 + i * (float) (Math.PI * 2.0 / 3.0);
			WildRenderer.arc(g, cx, cy, 9.5f, 1.2f, a0, a0 + 0.7f, 0, col(CYAN, 0.7f * a), col(CYAN, 0.3f * a), 3f);
		}
		// заряд репульсора — красное кольцо, ярчает при выстреле
		if (charge > 0.02f) {
			WildRenderer.ring(g, cx, cy, 23f, 1.4f + 1.6f * charge, col(RED, 0.85f * charge * a),
					col(RED, 0.5f * charge * a), 6f + 6f * charge);
		}
		// центральный реактор-орб
		WildRenderer.orb(g, cx, cy, 3.2f + 0.6f * pulse, cyan, col(CYAN, 0.9f * a), 1f, col(CYAN, 0.5f * a), 6f);
		// мушка
		int cross = col(GOLD, 0.95f * a);
		float gap = 5f, len = 4f;
		WildRenderer.fill(g, cx - gap - len, cy - 0.6f, len, 1.2f, 0.6f, cross);
		WildRenderer.fill(g, cx + gap, cy - 0.6f, len, 1.2f, 0.6f, cross);
		WildRenderer.fill(g, cx - 0.6f, cy - gap - len, 1.2f, len, 0.6f, cross);
		WildRenderer.fill(g, cx - 0.6f, cy + gap, 1.2f, len, 0.6f, cross);
	}

	// ===================== лево: энергия + заряд =====================
	private static void drawLeftGauges(GuiGraphics g, Font font, int sh, float tt, float energy, float charge, float boot) {
		float a = boot;
		float slide = (1f - boot) * -24f;
		float gx = 58f + slide;
		float gy = sh / 2f;
		float rE = 30f;
		float rC = 22f;
		// фоновые кольца
		WildRenderer.ring(g, gx, gy, rE, 2.4f, col(GOLD_DIM, 0.30f * a), 0, 0f);
		WildRenderer.ring(g, gx, gy, rC, 2.0f, col(GOLD_DIM, 0.25f * a), 0, 0f);
		// энергия — внешняя дуга
		WildRenderer.clockArc(g, gx, gy, rE, 2.6f, energy, col(GOLD, 0.95f * a), col(GOLD, 0.5f * a), 6f);
		// заряд репульсоров — внутренняя дуга (циан->красный по заряду)
		float chShow = Math.max(0.06f, charge);
		int chCol = lerpColor(CYAN, RED, charge);
		WildRenderer.clockArc(g, gx, gy, rC, 2.2f, chShow, col(chCol, (0.5f + 0.45f * charge) * a),
				col(chCol, 0.4f * a), 5f + 5f * charge);
		// центр-реактор
		float pulse = 0.5f + 0.5f * (float) Math.sin(tt * 3f);
		WildRenderer.orb(g, gx, gy, 5f + pulse, col(CYAN, 0.9f * a), col(CYAN, 0.9f * a), 1.2f, col(CYAN, 0.5f * a), 7f);
		// подписи
		int ePct = Math.round(energy * 100f);
		drawText(g, font, "ENERGY", gx + rE + 6f, gy - 9f, col(GOLD, 0.8f * a));
		drawText(g, font, ePct + "%", gx + rE + 6f, gy + 1f, col(WHITE, 0.95f * a));
		drawText(g, font, "REPULSOR", gx - rE - 6f - font.width("REPULSOR"), gy - 9f, col(chCol, 0.8f * a));
	}

	// ===================== право: HP сегментами =====================
	private static void drawRightHealth(GuiGraphics g, Font font, int sw, int sh, float hp, float tt, float boot) {
		float a = boot;
		float slide = (1f - boot) * 24f;
		int segs = 16;
		int lit = (int) Math.ceil(hp * segs);
		float segW = 26f;
		float segH = 3.2f;
		float gap = 3.4f;
		float totalH = segs * (segH + gap);
		float baseX = sw - 14f + slide;
		float topY = sh / 2f - totalH / 2f;
		// цвет по здоровью: зелёный->жёлтый->красный
		int hpCol = hp > 0.5f ? lerpColor(0xFFD34A, 0x6BFF6B, (hp - 0.5f) * 2f)
				: lerpColor(RED, 0xFFD34A, hp * 2f);
		boolean low = hp <= 0.3f;
		float blink = low ? (0.45f + 0.55f * (float) Math.abs(Math.sin(tt * 4f))) : 1f;
		for (int i = 0; i < segs; i++) {
			// снизу вверх
			int idxFromBottom = i;
			float y = topY + (segs - 1 - i) * (segH + gap);
			// лёгкий изгиб «дуги» — смещение по x в зависимости от позиции
			float curve = (float) Math.sin((i / (float) (segs - 1) - 0.5f) * Math.PI) * 4f;
			float w = segW - Math.abs(curve) * 0.4f;
			float x = baseX - w + curve;
			if (idxFromBottom < lit) {
				int c = col(hpCol, 0.92f * a * blink);
				WildRenderer.bar(g, x, y, w, segH, c, col(hpCol, 0.5f * a * blink));
			} else {
				WildRenderer.fill(g, x, y, w, segH, segH / 2f, col(GOLD_DIM, 0.22f * a));
			}
		}
		int hpVal = Math.round(hp * 100f);
		String label = "INTEGRITY " + hpVal + "%";
		drawText(g, font, label, baseX - segW - font.width(label) - 6f, sh / 2f - 4f, col(hpCol, 0.9f * a));
	}

	// ===================== низ: костюмы =====================
	private static final ResourceLocation[] SUIT_ICONS = buildSuitIcons();

	private static ResourceLocation[] buildSuitIcons() {
		int n = IronManSuitVariant.count();
		ResourceLocation[] arr = new ResourceLocation[n];
		for (int i = 0; i < n; i++) {
			arr[i] = ModId.of("textures/gui/ironman/suit_" + i + ".png");
		}
		return arr;
	}

	private static void drawSuitStrip(GuiGraphics g, Font font, float cx, int sh, int suitIdx, float boot, float tt) {
		float a = boot;
		int n = IronManSuitVariant.count();
		float icon = 22f;
		float gap = 8f;
		float total = n * icon + (n - 1) * gap;
		float startX = cx - total / 2f + icon / 2f;
		float y = sh - 26f + (1f - boot) * 18f;
		// панель-подложка
		HudUtil.neonPanel(g, (int) (cx - total / 2f - 10), (int) (y - icon / 2f - 8), (int) (total + 20), (int) (icon + 16),
				col(DARK, 0.55f * a), col(DARK, 0.30f * a), col(GOLD, 0.30f * a), col(GOLD, 0.10f * a));
		// бегущий highlight на текущем
		float selX = startX + dispSuit * (icon + gap);
		float pulse = 0.5f + 0.5f * (float) Math.sin(tt * 3f);
		WildRenderer.ring(g, selX, y, icon / 2f + 3f, 1.6f, col(GOLD, (0.7f + 0.3f * pulse) * a), col(GOLD, 0.4f * a), 7f);
		for (int i = 0; i < n; i++) {
			float ix = startX + i * (icon + gap);
			boolean sel = i == suitIdx;
			float darken = sel ? 0f : 0.35f;
			WildRenderer.iconCircle(g, SUIT_ICONS[i], ix, y, icon, icon / 2f - 0.5f, darken);
			WildRenderer.ring(g, ix, y, icon / 2f, 1f, col(sel ? GOLD : GOLD_DIM, (sel ? 0.8f : 0.4f) * a), 0, 0f);
		}
		String name = IronManSuitVariant.get(suitIdx).name();
		drawCentered(g, font, name.toUpperCase(java.util.Locale.ROOT), cx, y + icon / 2f + 4f, col(GOLD, 0.85f * a));
	}

	// ===================== низ: нано-оружие =====================
	private static void drawWeaponStrip(GuiGraphics g, Font font, float cx, int sh, int weaponIdx, float boot, float tt) {
		float a = boot;
		var weapons = ClientNanoWeaponState.WEAPONS;
		int n = weapons.size();
		float icon = 18f;
		float gap = 8f;
		float total = n * icon + (n - 1) * gap;
		// справа от центра, чуть выше нижнего края (над костюмами)
		float startX = cx - total / 2f + icon / 2f;
		float y = sh - 62f + (1f - boot) * 14f;
		float selX = startX + dispWeapon * (icon + gap);
		float pulse = 0.5f + 0.5f * (float) Math.sin(tt * 4f);
		WildRenderer.ring(g, selX, y, icon / 2f + 3f, 1.5f, col(CYAN, (0.7f + 0.3f * pulse) * a), col(CYAN, 0.4f * a), 6f);
		for (int i = 0; i < n; i++) {
			float ix = startX + i * (icon + gap);
			boolean sel = i == weaponIdx;
			ResourceLocation tex = AbilityIcons.texture(weapons.get(i).abilityId());
			WildRenderer.iconCircle(g, tex, ix, y, icon, icon / 2f - 0.5f, sel ? 0f : 0.4f);
			WildRenderer.ring(g, ix, y, icon / 2f, 1f, col(sel ? CYAN : GOLD_DIM, (sel ? 0.8f : 0.35f) * a), 0, 0f);
		}
		drawCentered(g, font, weapons.get(weaponIdx).label(), cx, y - icon / 2f - 9f, col(CYAN, 0.8f * a));
	}

	// ===================== окружение =====================
	private static void drawEnvironment(GuiGraphics g, Font font, int sw, int sh, Level level, Player player, float boot) {
		float a = boot;
		long dayTime = level.getDayTime() % 24000;
		int hour = (int) ((dayTime / 1000 + 6) % 24);
		int minute = (int) ((dayTime % 1000) * 60 / 1000);
		boolean night = dayTime >= 13000 && dayTime < 23000;
		String clock = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);
		String weather = level.isThundering() ? "STORM" : level.isRaining() ? "RAIN" : "CLEAR";
		float baseTemp = level.getBiome(player.blockPosition()).value().getBaseTemperature();
		int celsius = Math.round(baseTemp * 20f);
		long day = level.getDayTime() / 24000L + 1L;
		String sun = night ? "MOON" : "SUN";

		int pw = 96;
		int ph = 46;
		int px = sw - pw - 6;
		int py = sh - ph - 6 + (int) ((1f - boot) * 20f);
		HudUtil.neonPanel(g, px, py, pw, ph, col(DARK, 0.55f * a), col(DARK, 0.32f * a),
				col(GOLD, 0.30f * a), col(CYAN, 0.10f * a));
		int tx = px + 8;
		int ty = py + 7;
		drawText(g, font, "ENVIRONMENT", tx, ty, col(GOLD, 0.7f * a));
		drawText(g, font, clock + "  " + sun, tx, ty + 11, col(WHITE, 0.9f * a));
		drawText(g, font, weather + "  " + celsius + "\u00b0C", tx, ty + 21, col(CYAN, 0.85f * a));
		drawText(g, font, "DAY " + day, tx, ty + 31, col(GOLD, 0.7f * a));
	}

	// ===================== баннер =====================
	private static void drawTopBanner(GuiGraphics g, Font font, int sw, float tt, float boot) {
		float a = boot;
		float cx = sw / 2f;
		float y = 6f - (1f - boot) * 12f;
		drawCentered(g, font, "S T A R K   I N D U S T R I E S", cx, y, col(GOLD, 0.55f * a));
		String sub = "J.A.R.V.I.S. ONLINE";
		drawCentered(g, font, sub, cx, y + 10f, col(CYAN, 0.85f * a));
		// разделительная линия с бегущим сканером
		float lw = 150f;
		WildRenderer.fill(g, cx - lw / 2f, y + 21f, lw, 1f, 0.5f, col(GOLD, 0.30f * a));
		float scan = (tt * 0.5f) % 1f;
		float sxp = cx - lw / 2f + scan * lw;
		WildRenderer.bar(g, sxp - 8f, y + 20.5f, 16f, 1.6f, col(CYAN, 0.9f * a), col(CYAN, 0.6f * a));
	}

	// ===================== углы =====================
	private static void drawCorners(GuiGraphics g, int sw, int sh, float tt, float boot) {
		float a = boot;
		float pulse = 0.55f + 0.45f * (float) Math.sin(tt * 1.6f);
		int c = col(GOLD, 0.55f * a * pulse);
		int glow = col(GOLD, 0.18f * a);
		float m = 6f;
		float armL = 26f;
		float th = 1.4f;
		// TL
		WildRenderer.bar(g, m, m, armL, th, c, glow);
		WildRenderer.bar(g, m, m, th, armL, c, glow);
		// TR
		WildRenderer.bar(g, sw - m - armL, m, armL, th, c, glow);
		WildRenderer.bar(g, sw - m - th, m, th, armL, c, glow);
		// BL
		WildRenderer.bar(g, m, sh - m - th, armL, th, c, glow);
		WildRenderer.bar(g, m, sh - m - armL, th, armL, c, glow);
		// BR
		WildRenderer.bar(g, sw - m - armL, sh - m - th, armL, th, c, glow);
		WildRenderer.bar(g, sw - m - th, sh - m - armL, th, armL, c, glow);
	}

	// ===================== helpers =====================
	private static void drawText(GuiGraphics g, Font font, String s, float x, float y, int argb) {
		if (((argb >>> 24) & 0xFF) < 4) {
			return;
		}
		g.drawString(font, Component.literal(s), (int) x, (int) y, argb, true);
	}

	private static void drawCentered(GuiGraphics g, Font font, String s, float cx, float y, int argb) {
		drawText(g, font, s, cx - font.width(s) / 2f, y, argb);
	}

	private static int col(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
		return (a << 24) | (rgb & 0x00FFFFFF);
	}

	private static int lerpColor(int c0, int c1, float t) {
		t = clamp01(t);
		int r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
		int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
		int r = Math.round(r0 + (r1 - r0) * t);
		int gg = Math.round(g0 + (g1 - g0) * t);
		int b = Math.round(b0 + (b1 - b0) * t);
		return (r << 16) | (gg << 8) | b;
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static float ease(float t) {
		// easeOutCubic
		float u = 1f - t;
		return 1f - u * u * u;
	}
}
