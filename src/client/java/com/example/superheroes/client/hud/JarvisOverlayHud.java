package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.ironman.IronManSuitVariant;
import com.example.superheroes.client.ClientHeroState;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * HUD Железного Человека от 1-го лица «Mark HUD». Полностью на SDF-шейдерах
 * ({@link WildRenderer}): чистый центрированный арк-реактор-прицел, левый блок
 * энергии + ДИНАМИЧЕСКОГО заряда репульсоров (растёт в присяде), правый HP-блок
 * сегментными дугами, нижний селектор костюмов (без подписи — её описывает
 * правая панель окружения), верхний баннер J.A.R.V.I.S., боковые телеметрии и
 * панель окружения красивым моноширинным шрифтом J.A.R.V.I.S. с иконками.
 */
public final class JarvisOverlayHud {
	// ---- палитра ----
	private static final int GOLD = 0xFFC400;
	private static final int GOLD_DIM = 0x8A6A00;
	private static final int RED = 0xE2342B;
	private static final int CYAN = 0x46D8FF;
	private static final int WHITE = 0xFFF4D6;
	private static final int DARK = 0x0A0E14;

	/** Кастомный моноширинный «терминальный» шрифт J.A.R.V.I.S. (Share Tech Mono). */
	private static final Style JARVIS_FONT = Style.EMPTY.withFont(ModId.of("jarvis"));

	// ---- анимационное состояние ----
	private static long bootMs = 0L;
	private static long lastFrameMs = 0L;
	private static float dispEnergy = 0f;
	private static float dispHp = 0f;
	private static float dispCharge = 0f;
	private static float dispSuit = 0f;

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
		if (mc.options.hideGui) {
			return;
		}
		// HUD Железного Человека рисуется ТОЛЬКО от 1-го лица; в 3-м лице (F5) убираем.
		if (!mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		if (!WildShaders.circleReady() || !WildShaders.rectReady()) {
			return;
		}

		long now = System.currentTimeMillis();
		if (bootMs == 0L) {
			bootMs = now;
			dispEnergy = clamp01(ClientHeroState.data().energy() / Math.max(1f, ClientHeroState.energyMax()));
			dispHp = clamp01(player.getHealth() / Math.max(1f, player.getMaxHealth()));
		}
		// «Загорание экрана» при активации костюма: плавное проявление за ~850мс
		// с лёгким CRT-мерцанием/просадками яркости в начале (эффект включения HUD).
		long bootAge = now - bootMs;
		float bootRaw = clamp01(bootAge / 850f);
		float boot = ease(bootRaw);
		if (bootRaw < 0.40f) {
			float flicker = 0.72f + 0.28f * (float) Math.sin(bootAge * 0.055f);
			float dip = (bootAge % 140L) < 26L ? 0.5f : 1f; // редкие короткие «провалы»
			boot = clamp01(boot * flicker * dip);
		}
		float dt = lastFrameMs == 0L ? 0.016f : Math.min(0.1f, (now - lastFrameMs) / 1000f);
		lastFrameMs = now;

		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		// центр экрана, округлён до пикселя — иначе прицел «съезжает» на 0.5px
		float cx = Math.round(sw / 2f);
		float cy = Math.round(sh / 2f);
		float tt = now / 1000f;

		// ---- данные ----
		float energy = clamp01(ClientHeroState.data().energy() / Math.max(1f, ClientHeroState.energyMax()));
		float hp = clamp01(player.getHealth() / Math.max(1f, player.getMaxHealth()));
		float charge = ClientRepulsorChargeState.charge();
		int suitIdx = ClientSuitVariantState.variantFor(player.getUUID());

		float k = 1f - (float) Math.exp(-dt / 0.12f);
		dispEnergy += (energy - dispEnergy) * k;
		dispHp += (hp - dispHp) * k;
		dispCharge += (charge - dispCharge) * Math.min(1f, dt / 0.06f);
		dispSuit += (suitIdx - dispSuit) * (1f - (float) Math.exp(-dt / 0.10f));

		Font font = mc.font;
		drawCorners(g, sw, sh, tt, boot);
		drawSideRails(g, sw, sh, tt, boot);
		drawTopBanner(g, font, sw, tt, boot);
		drawCenterReticle(g, cx, cy, tt, dispCharge, boot);
		drawCenterReadout(g, font, cx, cy, player, boot);
		drawLeftGauges(g, sh, tt, dispEnergy, dispCharge, boot);
		drawRightHealth(g, font, sw, sh, dispHp, tt, boot);
		drawSuitStrip(g, cx, sh, suitIdx, boot, tt);
		drawEnvironment(g, font, sw, sh, player.level(), player, suitIdx, boot);
	}

	// ===================== центр: арк-реактор (чистый, симметричный) =====
	private static void drawCenterReticle(GuiGraphics g, float cx, float cy, float tt, float charge, float boot) {
		float a = boot;
		float pulse = 0.5f + 0.5f * (float) Math.sin(tt * 2.2f);
		int gold = col(GOLD, 0.85f * a);
		int goldSoft = col(GOLD, 0.30f * a);
		int cyan = col(CYAN, (0.55f + 0.35f * pulse) * a);

		// мягкое тонкое полное кольцо
		WildRenderer.ring(g, cx, cy, 14f, 0.9f, goldSoft, 0, 0f);
		// симметричные «скобки захвата» — 4 короткие дуги строго по осям/диагоналям,
		// вращаются как единое целое (симметрия сохраняется => не выглядит криво)
		float rot = tt * 0.5f;
		float half = 0.42f; // полудлина дуги (рад)
		for (int i = 0; i < 4; i++) {
			float c0 = rot + i * (float) (Math.PI / 2.0);
			WildRenderer.arc(g, cx, cy, 19f, 1.5f, c0 - half, c0 + half, 0, gold, col(GOLD, 0.22f * a), 4f);
		}
		// внутреннее тонкое контр-вращающееся кольцо из 4 симметричных тиков
		float rot2 = -tt * 0.9f;
		for (int i = 0; i < 4; i++) {
			float c0 = rot2 + (float) (Math.PI / 4.0) + i * (float) (Math.PI / 2.0);
			WildRenderer.arc(g, cx, cy, 9.5f, 1.1f, c0 - 0.18f, c0 + 0.18f, 0, col(CYAN, 0.7f * a), 0, 0f);
		}
		// заряд репульсора — растущее красно-циановое кольцо (динамический заряд)
		if (charge > 0.02f) {
			int chCol = lerpColor(CYAN, RED, charge);
			WildRenderer.ring(g, cx, cy, 23.5f, 1.2f + 1.8f * charge, col(chCol, 0.85f * charge * a),
					col(chCol, 0.5f * charge * a), 5f + 7f * charge);
		}
		// вспышка разряда — резкая красная при выстреле
		float flash = ClientRepulsorChargeState.dischargeFlash();
		if (flash > 0.02f) {
			WildRenderer.ring(g, cx, cy, 27f, 1.0f + 3f * flash, col(RED, 0.9f * flash * a),
					col(RED, 0.6f * flash * a), 8f * flash);
		}
		// центральный реактор-орб
		WildRenderer.orb(g, cx, cy, 3.0f + 0.6f * pulse, cyan, col(CYAN, 0.9f * a), 1f, col(CYAN, 0.5f * a), 6f);
		// симметричная мушка (4 одинаковых штриха ровно по осям)
		int cross = col(GOLD, 0.95f * a);
		float gap = 5f, len = 4f, th = 1.2f;
		WildRenderer.fill(g, cx - gap - len, cy - th / 2f, len, th, 0.6f, cross);
		WildRenderer.fill(g, cx + gap, cy - th / 2f, len, th, 0.6f, cross);
		WildRenderer.fill(g, cx - th / 2f, cy - gap - len, th, len, 0.6f, cross);
		WildRenderer.fill(g, cx - th / 2f, cy + gap, th, len, 0.6f, cross);
	}

	/** Маленькая телеметрия под прицелом: высота (Y) и курс — заполняет «пустоту». */
	private static void drawCenterReadout(GuiGraphics g, Font font, float cx, float cy, Player player, float boot) {
		float a = boot;
		int alt = (int) Math.floor(player.getY());
		String heading = compass(player.getYRot());
		String line = "ALT " + alt + "   " + heading;
		drawJarvisCentered(g, font, line, cx, cy + 30f, col(GOLD, 0.55f * a));
	}

	// ===================== лево: энергия + динамический заряд ============
	private static void drawLeftGauges(GuiGraphics g, int sh, float tt, float energy, float charge, float boot) {
		float a = boot;
		float slide = (1f - boot) * -24f;
		float gx = 60f + slide;
		float gy = sh / 2f;
		float rE = 30f;
		float rC = 22f;

		// радиальные тики по внешнему ободу — заполняют «пустоту», техно-вид
		int ticks = 36;
		for (int i = 0; i < ticks; i++) {
			float ang = (float) (i / (float) ticks * Math.PI * 2.0);
			boolean major = i % 3 == 0;
			float tr0 = rE + 3f;
			float tr1 = rE + (major ? 7f : 4.5f);
			float txA = gx + (float) Math.cos(ang);
			float tickAlpha = (i / (float) ticks <= energy ? 0.7f : 0.18f) * a;
			float sx = gx + (float) Math.cos(ang) * tr0;
			float sy = gy - (float) Math.sin(ang) * tr0;
			float ex = gx + (float) Math.cos(ang) * tr1;
			float ey = gy - (float) Math.sin(ang) * tr1;
			thinLine(g, sx, sy, ex, ey, col(major ? GOLD : GOLD_DIM, tickAlpha));
		}

		// фоновые кольца
		WildRenderer.ring(g, gx, gy, rE, 2.4f, col(GOLD_DIM, 0.28f * a), 0, 0f);
		WildRenderer.ring(g, gx, gy, rC, 2.0f, col(GOLD_DIM, 0.22f * a), 0, 0f);
		// энергия — внешняя дуга прогресса
		WildRenderer.clockArc(g, gx, gy, rE, 2.6f, energy, col(GOLD, 0.95f * a), col(GOLD, 0.5f * a), 6f);
		// заряд репульсоров — внутренняя дуга (циан→красный по заряду)
		float chShow = Math.max(0.05f, charge);
		int chCol = lerpColor(CYAN, RED, charge);
		WildRenderer.clockArc(g, gx, gy, rC, 2.2f, chShow, col(chCol, (0.45f + 0.5f * charge) * a),
				col(chCol, 0.4f * a), 5f + 6f * charge);

		// бегущий сканер-луч по внешнему ободу
		float sweep = (tt * 0.7f) % 1f;
		float sang = (float) ((0.5 - sweep) * Math.PI * 2.0 + Math.PI / 2.0);
		float sx = gx + (float) Math.cos(sang) * rE;
		float sy = gy - (float) Math.sin(sang) * rE;
		WildRenderer.orb(g, sx, sy, 1.6f, col(CYAN, 0.9f * a), 0, 0f, col(CYAN, 0.6f * a), 4f);

		// центр-реактор с пульсацией + лучами
		float pulse = 0.5f + 0.5f * (float) Math.sin(tt * 3f);
		for (int i = 0; i < 6; i++) {
			float ang = (float) (i / 6.0 * Math.PI * 2.0) + tt * 0.4f;
			float r0 = 6.5f, r1 = 11f;
			thinLine(g, gx + (float) Math.cos(ang) * r0, gy - (float) Math.sin(ang) * r0,
					gx + (float) Math.cos(ang) * r1, gy - (float) Math.sin(ang) * r1, col(CYAN, 0.35f * a));
		}
		WildRenderer.orb(g, gx, gy, 5f + pulse, col(CYAN, 0.9f * a), col(CYAN, 0.9f * a), 1.2f, col(CYAN, 0.55f * a), 8f);
	}

	// ===================== право: HP сегментами =====================
	private static void drawRightHealth(GuiGraphics g, Font font, int sw, int sh, float hp, float tt, float boot) {
		float a = boot;
		float slide = (1f - boot) * 24f;
		int segs = 16;
		int lit = (int) Math.ceil(hp * segs);
		// РОВНАЯ полоска: одинаковая ширина, единый правый край, скруглённые концы
		float segW = 26f;
		float segH = 3.4f;
		float gap = 3.2f;
		float totalH = segs * (segH + gap) - gap;
		float rightX = sw - 12f + slide;       // общий правый край
		float x = rightX - segW;               // общий левый край (все сегменты выровнены)
		float topY = sh / 2f - totalH / 2f;
		int hpCol = hp > 0.5f ? lerpColor(0xFFD34A, 0x6BFF6B, (hp - 0.5f) * 2f)
				: lerpColor(RED, 0xFFD34A, hp * 2f);
		boolean low = hp <= 0.3f;
		float blink = low ? (0.45f + 0.55f * (float) Math.abs(Math.sin(tt * 4f))) : 1f;
		for (int i = 0; i < segs; i++) {
			float y = topY + (segs - 1 - i) * (segH + gap);
			if (i < lit) {
				int c = col(hpCol, 0.92f * a * blink);
				WildRenderer.bar(g, x, y, segW, segH, c, col(hpCol, 0.5f * a * blink));
			} else {
				WildRenderer.fill(g, x, y, segW, segH, segH / 2f, col(GOLD_DIM, 0.22f * a));
			}
		}
	}

	// ===================== низ: костюмы (без подписи) =====================
	private static final ResourceLocation[] SUIT_ICONS = buildSuitIcons();

	private static ResourceLocation[] buildSuitIcons() {
		int n = IronManSuitVariant.count();
		ResourceLocation[] arr = new ResourceLocation[n];
		for (int i = 0; i < n; i++) {
			arr[i] = ModId.of("textures/gui/ironman/suit_" + i + ".png");
		}
		return arr;
	}

	private static void drawSuitStrip(GuiGraphics g, float cx, int sh, int suitIdx, float boot, float tt) {
		float a = boot;
		int n = IronManSuitVariant.count();
		float icon = 22f;
		float gap = 8f;
		float total = n * icon + (n - 1) * gap;
		float startX = cx - total / 2f + icon / 2f;
		float y = sh - 26f + (1f - boot) * 18f;
		HudUtil.neonPanel(g, (int) (cx - total / 2f - 10), (int) (y - icon / 2f - 8), (int) (total + 20), (int) (icon + 16),
				col(DARK, 0.55f * a), col(DARK, 0.30f * a), col(GOLD, 0.30f * a), col(GOLD, 0.10f * a));
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
		// подпись костюма убрана — название показывает правая панель окружения.
	}

	// ===================== окружение (красивый шрифт + иконки + костюм) ==
	private static void drawEnvironment(GuiGraphics g, Font font, int sw, int sh, Level level, Player player,
			int suitIdx, float boot) {
		float a = boot;
		long dayTime = level.getDayTime() % 24000;
		int hour = (int) ((dayTime / 1000 + 6) % 24);
		int minute = (int) ((dayTime % 1000) * 60 / 1000);
		boolean night = dayTime >= 13000 && dayTime < 23000;
		String clock = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);
		boolean storm = level.isThundering();
		boolean rain = level.isRaining();
		String weather = storm ? "STORM" : rain ? "RAIN" : night ? "NIGHT" : "CLEAR";
		float baseTemp = level.getBiome(player.blockPosition()).value().getBaseTemperature();
		int celsius = Math.round(baseTemp * 20f);
		long day = level.getDayTime() / 24000L + 1L;
		IronManSuitVariant suit = IronManSuitVariant.get(suitIdx);
		String suitName = suit.name().toUpperCase(java.util.Locale.ROOT);
		String suitStat = String.format(java.util.Locale.ROOT, "ATK x%.2f", suit.damageMultiplier());

		int pw = 132;
		int ph = 70;
		int px = sw - pw - 8;
		int py = sh - ph - 8 + (int) ((1f - boot) * 22f);
		HudUtil.neonPanel(g, px, py, pw, ph, col(DARK, 0.62f * a), col(DARK, 0.38f * a),
				col(GOLD, 0.35f * a), col(CYAN, 0.12f * a));
		// заголовок + разделитель
		int tx = px + 10;
		int ty = py + 8;
		drawJarvis(g, font, "J.A.R.V.I.S. // ENV", tx, ty, col(CYAN, 0.8f * a));
		WildRenderer.fill(g, px + 8, py + 19, pw - 16, 0.8f, 0.4f, col(GOLD, 0.3f * a));

		int rowY = py + 24;
		int iconX = px + 12;
		// строка времени + солнце/луна
		if (night) {
			drawMoon(g, iconX, rowY + 3.5f, 4f, col(CYAN, 0.9f * a));
		} else {
			drawSun(g, iconX, rowY + 3.5f, 4f, tt(), col(GOLD, 0.95f * a));
		}
		drawJarvis(g, font, clock, iconX + 11, rowY, col(WHITE, 0.95f * a));
		drawJarvis(g, font, "D" + day, px + pw - 10 - jw(font, "D" + day), rowY, col(GOLD, 0.7f * a));

		rowY += 13;
		// строка погоды + температура
		drawWeatherIcon(g, iconX, rowY + 3.5f, 4f, storm, rain, night, col(CYAN, 0.9f * a));
		drawJarvis(g, font, weather, iconX + 11, rowY, col(CYAN, 0.85f * a));
		String temp = celsius + "\u00b0C";
		drawJarvis(g, font, temp, px + pw - 10 - jw(font, temp), rowY, col(WHITE, 0.9f * a));

		rowY += 13;
		// строка костюма (описывает текущий костюм — по запросу)
		WildRenderer.fill(g, iconX, rowY + 1.5f, 7f, 5f, 1.2f, col(GOLD, 0.85f * a));
		drawJarvis(g, font, "SUIT " + suitName, iconX + 11, rowY, col(GOLD, 0.9f * a));
		drawJarvis(g, font, suitStat, px + pw - 10 - jw(font, suitStat), rowY, col(GOLD_DIM, 0.85f * a));
	}

	// ===================== баннер =====================
	private static void drawTopBanner(GuiGraphics g, Font font, int sw, float tt, float boot) {
		float a = boot;
		float cx = sw / 2f;
		float y = 6f - (1f - boot) * 12f;
		drawJarvisCentered(g, font, "S T A R K   I N D U S T R I E S", cx, y, col(GOLD, 0.55f * a));
		drawJarvisCentered(g, font, "J.A.R.V.I.S. ONLINE", cx, y + 11f, col(CYAN, 0.85f * a));
		float lw = 160f;
		WildRenderer.fill(g, cx - lw / 2f, y + 22f, lw, 1f, 0.5f, col(GOLD, 0.30f * a));
		float scan = (tt * 0.5f) % 1f;
		float sxp = cx - lw / 2f + scan * lw;
		WildRenderer.bar(g, sxp - 8f, y + 21.5f, 16f, 1.6f, col(CYAN, 0.9f * a), col(CYAN, 0.6f * a));
	}

	// ===================== боковые телеметрии (заполняют пустоту) ========
	private static void drawSideRails(GuiGraphics g, int sw, int sh, float tt, float boot) {
		float a = boot;
		float top = sh * 0.30f;
		float bot = sh * 0.70f;
		int n = 14;
		float move = (tt * 6f) % ((bot - top) / n);
		for (int i = 0; i <= n; i++) {
			float y = top + i * (bot - top) / n;
			boolean major = i % 2 == 0;
			float lenL = major ? 9f : 5f;
			// левый рельс
			thinLine(g, 6f, y, 6f + lenL, y, col(GOLD_DIM, (major ? 0.5f : 0.25f) * a));
			// правый рельс
			thinLine(g, sw - 6f, y, sw - 6f - lenL, y, col(GOLD_DIM, (major ? 0.5f : 0.25f) * a));
		}
		// бегущий маркер по левому рельсу
		float my = top + (((tt * 0.25f) % 1f) * (bot - top));
		WildRenderer.fill(g, 6f, my - 0.8f, 12f, 1.6f, 0.6f, col(CYAN, 0.7f * a));
		WildRenderer.fill(g, sw - 18f, my - 0.8f, 12f, 1.6f, 0.6f, col(CYAN, 0.7f * a));
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
		WildRenderer.bar(g, m, m, armL, th, c, glow);
		WildRenderer.bar(g, m, m, th, armL, c, glow);
		WildRenderer.bar(g, sw - m - armL, m, armL, th, c, glow);
		WildRenderer.bar(g, sw - m - th, m, th, armL, c, glow);
		WildRenderer.bar(g, m, sh - m - th, armL, th, c, glow);
		WildRenderer.bar(g, m, sh - m - armL, th, armL, c, glow);
		WildRenderer.bar(g, sw - m - armL, sh - m - th, armL, th, c, glow);
		WildRenderer.bar(g, sw - m - th, sh - m - armL, th, armL, c, glow);
	}

	// ===================== маленькие иконки =====================
	private static void drawSun(GuiGraphics g, float cx, float cy, float r, float t, int color) {
		WildRenderer.orb(g, cx, cy, r * 0.7f, color, color, 0.8f, col(color, 0.4f), r * 1.4f);
		for (int i = 0; i < 8; i++) {
			float ang = (float) (i / 8.0 * Math.PI * 2.0) + t * 0.5f;
			thinLine(g, cx + (float) Math.cos(ang) * (r + 0.5f), cy - (float) Math.sin(ang) * (r + 0.5f),
					cx + (float) Math.cos(ang) * (r + 2.6f), cy - (float) Math.sin(ang) * (r + 2.6f), color);
		}
	}

	private static void drawMoon(GuiGraphics g, float cx, float cy, float r, int color) {
		WildRenderer.ring(g, cx, cy, r, 1.1f, color, col(color, 0.4f), r);
		// «вырез» — затемнённый орб, смещённый вправо, создаёт серп
		WildRenderer.orb(g, cx + r * 0.7f, cy - r * 0.2f, r * 0.85f, col(DARK, 0.9f), 0, 0f, 0, 0f);
	}

	private static void drawWeatherIcon(GuiGraphics g, float cx, float cy, float r, boolean storm, boolean rain,
			boolean night, int color) {
		if (!storm && !rain) {
			if (night) {
				drawMoon(g, cx, cy, r, color);
			} else {
				WildRenderer.orb(g, cx, cy, r * 0.6f, color, color, 0.6f, col(color, 0.35f), r);
			}
			return;
		}
		// облако: 2-3 перекрывающихся орба
		WildRenderer.orb(g, cx - 1.6f, cy + 0.6f, r * 0.55f, color, 0, 0f, 0, 0f);
		WildRenderer.orb(g, cx + 1.6f, cy + 0.6f, r * 0.55f, color, 0, 0f, 0, 0f);
		WildRenderer.orb(g, cx, cy - 0.4f, r * 0.7f, color, 0, 0f, col(color, 0.3f), r);
		if (storm) {
			thinLine(g, cx, cy + 2f, cx - 1.2f, cy + 4f, col(GOLD, 0.95f));
			thinLine(g, cx - 1.2f, cy + 4f, cx + 0.6f, cy + 5.2f, col(GOLD, 0.95f));
		} else {
			thinLine(g, cx - 1.5f, cy + 2.5f, cx - 1.5f, cy + 4.5f, col(CYAN, 0.8f));
			thinLine(g, cx + 1.5f, cy + 2.5f, cx + 1.5f, cy + 4.5f, col(CYAN, 0.8f));
		}
	}

	// ===================== helpers =====================
	private static float tt() {
		return System.currentTimeMillis() / 1000f;
	}

	/** Тонкая линия между двумя точками через капсульный bar. */
	private static void thinLine(GuiGraphics g, float x0, float y0, float x1, float y1, int argb) {
		if (((argb >>> 24) & 0xFF) < 4) {
			return;
		}
		float dx = x1 - x0, dy = y1 - y0;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.01f) {
			return;
		}
		// аппроксимируем линию короткими залитыми точками вдоль направления
		int steps = Math.max(1, (int) (len / 1.2f));
		for (int i = 0; i <= steps; i++) {
			float f = i / (float) steps;
			float px = x0 + dx * f;
			float py = y0 + dy * f;
			WildRenderer.fill(g, px - 0.6f, py - 0.6f, 1.2f, 1.2f, 0.6f, argb);
		}
	}

	private static MutableComponent jarvis(String s) {
		return Component.literal(s).setStyle(JARVIS_FONT);
	}

	private static int jw(Font font, String s) {
		return font.width(jarvis(s));
	}

	private static void drawJarvis(GuiGraphics g, Font font, String s, float x, float y, int argb) {
		if (((argb >>> 24) & 0xFF) < 4) {
			return;
		}
		g.drawString(font, jarvis(s), (int) x, (int) y, argb, true);
	}

	private static void drawJarvisCentered(GuiGraphics g, Font font, String s, float cx, float y, int argb) {
		drawJarvis(g, font, s, cx - jw(font, s) / 2f, y, argb);
	}

	private static String compass(float yaw) {
		float y = ((yaw % 360) + 360) % 360;
		String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		int idx = Math.round(y / 45f) % 8;
		return dirs[idx];
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
		float u = 1f - t;
		return 1f - u * u * u;
	}
}
