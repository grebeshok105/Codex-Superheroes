package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMadnessState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

public final class MadnessHudOverlay {
	private static final Component[] VOICES = new Component[]{
			Component.translatable("superheroes.madness.voice.1"),
			Component.translatable("superheroes.madness.voice.2"),
			Component.translatable("superheroes.madness.voice.3"),
			Component.translatable("superheroes.madness.voice.4"),
			Component.translatable("superheroes.madness.voice.5"),
			Component.translatable("superheroes.madness.voice.6"),
			Component.translatable("superheroes.madness.voice.7"),
			Component.translatable("superheroes.madness.voice.8"),
			Component.translatable("superheroes.madness.voice.9"),
			Component.translatable("superheroes.madness.voice.10"),
			Component.translatable("superheroes.madness.voice.11"),
			Component.translatable("superheroes.madness.voice.12"),
			Component.translatable("superheroes.madness.voice.13"),
			Component.translatable("superheroes.madness.voice.14"),
			Component.translatable("superheroes.madness.voice.15"),
			Component.translatable("superheroes.madness.voice.16"),
			Component.translatable("superheroes.madness.voice.17"),
			Component.translatable("superheroes.madness.voice.18"),
	};

	private static final String[] FLOATING_SYMBOLS = new String[]{
			// occult / religious
			"卐", "✟", "\u2620", "\u2695", "\u26B0", "\u2694", "\u2623",
			// greek / math
			"Ω", "Σ", "Δ", "Ψ", "Λ", "Θ", "Ξ", "∞", "⊗", "⟁",
			// kanji / sin / pride
			"罪", "獅", "王", "血", "狂", "神", "死",
			// runic / ogham / cuneiform
			"ᚱ", "ᛉ", "ᚦ", "ᛟ", "ᛞ", "ᛊ", "ᛜ",
			"𐎀", "𐎁", "𐎂", "𐎃", "𐎄",
			// zalgo / combining diacritics (noise glyphs)
			"R̷̢̡͞", "Ȩ̷͞", "G̵̢͠", "U̶̧̧̢", "L̸̨", "U̵̧", "S̴̢",
			// encrypted/nonsense words
			"Жадность", "Корнеас", "Лев", "Грех", "Regulus",
			"Х̴р̶и̸з̵о̴с̴", "Х̸о̷м̶", "А̴м̸е̶н̴", "In̷f̴e̸r̶n̷u̴m̸",
			// occult text fragments (looks encrypted)
			"ΨΛΞΣ", "RΞGᐖ⌘", "▲▽▲▽", "ᚨᚺᛋ", "Ꮷ⎺⎺ᗰ", "𓂀𓁹𓆣", "⌇⌇⌇"
	};

	private static final long HEARTBEAT_RAMP_MS = 180_000L;
	private static final double HEARTBEAT_INTERVAL_START_MS = 1100.0;
	private static final double HEARTBEAT_INTERVAL_END_MS = 160.0;

	private static long lastBeatMs = 0L;
	private static long lastVoiceMs = 0L;
	private static Component currentVoice = null;
	private static long currentVoiceStartedMs = 0L;
	private static long currentVoiceUntilMs = 0L;
	private static final Random RNG = new Random();

	private MadnessHudOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		if (p == null) return;
		int sw = graphics.guiWidth();
		int sh = graphics.guiHeight();

		if (ClientMadnessState.isReading()) {
			renderReading(graphics, sw, sh);
		}
		if (!ClientMadnessState.isMadness()) {
			lastBeatMs = 0L;
			currentVoice = null;
			return;
		}
		long now = System.currentTimeMillis();
		long elapsed = now - ClientMadnessState.madnessStartedAtMs();
		double phase = Math.min(1.0, elapsed / (double) HEARTBEAT_RAMP_MS);
		double eased = phase * phase * (3.0 - 2.0 * phase);
		double interval = HEARTBEAT_INTERVAL_START_MS
				- (HEARTBEAT_INTERVAL_START_MS - HEARTBEAT_INTERVAL_END_MS) * eased;
		long interv = Math.max(120L, (long) interval);
		if (lastBeatMs == 0L) lastBeatMs = now - interv;
		float beatStrength;
		if (now - lastBeatMs >= interv) {
			lastBeatMs = now;
			float volume = 0.6f + (float) eased * 0.7f;
			float pitch = 1.0f - (float) eased * 0.25f;
			mc.level.playLocalSound(p.getX(), p.getY(), p.getZ(),
					SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS,
					volume, pitch, false);
			if (eased > 0.65 && RNG.nextInt(3) == 0) {
				mc.level.playLocalSound(p.getX(), p.getY(), p.getZ(),
						SoundEvents.ELDER_GUARDIAN_HURT, net.minecraft.sounds.SoundSource.PLAYERS,
						0.35f, 1.3f, false);
			}
			beatStrength = 1f;
		} else {
			float t = (now - lastBeatMs) / (float) interv;
			beatStrength = Math.max(0f, 1f - t * 4f);
		}

		float alphaBase = 0.18f + (float) eased * 0.55f;
		float pulse = beatStrength * alphaBase;
		int redA = (int) (pulse * 255f);
		if (redA > 4) {
			int color = (Math.min(255, redA) << 24) | 0x00DD0000;
			graphics.fill(0, 0, sw, sh, color);
		}

		float vigStrength = 0.25f + (float) eased * 0.45f + beatStrength * 0.2f;
		drawVignette(graphics, sw, sh, vigStrength);

		tickAndDrawVoice(graphics, mc, sw, sh, now, eased);

		long seed = (now / 450L);
		Random r = new Random(seed);
		int symbolCount = 12 + (int) (eased * 14);
		for (int i = 0; i < symbolCount; i++) {
			String sym = FLOATING_SYMBOLS[r.nextInt(FLOATING_SYMBOLS.length)];
			int x = r.nextInt(Math.max(1, sw - 80)) + 20;
			int y = r.nextInt(Math.max(1, sh - 80)) + 20;
			int alpha = 140 + r.nextInt(90);
			int paletteRoll = r.nextInt(100);
			int tint;
			if (paletteRoll < 55) tint = 0x00FF1010;
			else if (paletteRoll < 78) tint = 0x00FF5050;
			else if (paletteRoll < 92) tint = 0x00B0000A;
			else tint = 0x00200000;
			int color = (alpha << 24) | tint;
			graphics.drawString(mc.font, sym, x, y, color, true);
		}

		// burning high-contrast mid-screen glyphs, flicker with heartbeat
		int flickerAlpha = (int) (beatStrength * 220f);
		if (flickerAlpha > 15) {
			long seed2 = now / 180L;
			Random r2 = new Random(seed2);
			int flashCount = 2 + r2.nextInt(3);
			for (int i = 0; i < flashCount; i++) {
				String sym = FLOATING_SYMBOLS[r2.nextInt(FLOATING_SYMBOLS.length)];
				int x = r2.nextInt(Math.max(1, sw - 40)) + 10;
				int y = r2.nextInt(Math.max(1, sh - 40)) + 10;
				int flashColor = (Math.min(240, flickerAlpha) << 24) | 0x00FFD0D0;
				graphics.drawString(mc.font, sym, x, y, flashColor, true);
			}
		}

		if (ClientMadnessState.isBonusLifeAvailable()) {
			Component lifeText = Component.literal("\u25C6 \u0414\u041E\u041F. \u0416\u0418\u0417\u041D\u042C")
					.withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
			int w = mc.font.width(lifeText);
			graphics.drawString(mc.font, lifeText, sw - w - 8, sh - 32, 0xFFFFD700, true);
		}
	}

	private static void drawVignette(GuiGraphics graphics, int sw, int sh, float strength) {
		int baseA = (int) Math.min(255f, strength * 200f);
		if (baseA < 4) return;
		int tintColor = 0x00200000;
		int topColor = (baseA << 24) | tintColor;
		int transparent = 0x00000000;
		int bandH = (int) (sh * 0.45f);
		graphics.fillGradient(0, 0, sw, bandH, topColor, transparent);
		graphics.fillGradient(0, sh - bandH, sw, sh, transparent, topColor);

		int sideW = (int) (sw * 0.28f);
		for (int i = 0; i < sideW; i++) {
			float t = 1f - (i / (float) sideW);
			float eased = t * t;
			int a = (int) (baseA * eased * 0.85f);
			if (a < 2) continue;
			int color = (a << 24) | tintColor;
			graphics.fill(i, 0, i + 1, sh, color);
			graphics.fill(sw - i - 1, 0, sw - i, sh, color);
		}
	}

	private static void tickAndDrawVoice(GuiGraphics graphics, Minecraft mc, int sw, int sh, long now, double eased) {
		long interval = (long) (9000.0 - 5000.0 * eased);
		if (currentVoice == null || now > currentVoiceUntilMs) {
			if (now - lastVoiceMs > interval) {
				currentVoice = VOICES[RNG.nextInt(VOICES.length)];
				currentVoiceStartedMs = now;
				currentVoiceUntilMs = now + 2800L;
				lastVoiceMs = now;
				if (mc.player != null && mc.level != null) {
					mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
							SoundEvents.EVOKER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS,
							0.35f, 0.7f + RNG.nextFloat() * 0.4f, false);
					mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
							SoundEvents.SCULK_CLICKING, net.minecraft.sounds.SoundSource.PLAYERS,
							0.45f, 0.6f, false);
				}
			}
		}
		if (currentVoice != null && now <= currentVoiceUntilMs) {
			long sinceStart = now - currentVoiceStartedMs;
			float fadeIn = Math.min(1f, sinceStart / 180f);
			long left = currentVoiceUntilMs - now;
			float fadeOut = left < 600L ? left / 600f : 1f;
			float fade = fadeIn * fadeOut;
			float scale = 1f + (1f - fadeIn) * 0.3f;
			int va = (int) (fade * 220f);
			int color = (Math.max(0, Math.min(255, va)) << 24) | 0x00FFC0C0;
			int shadowColor = (Math.max(0, Math.min(255, va / 2)) << 24) | 0x00400000;
			Component msg = Component.empty().append(currentVoice).withStyle(ChatFormatting.ITALIC);
			int w = mc.font.width(msg);
			int drawX = sw / 2 - (int) (w * scale / 2);
			int drawY = sh / 4;
			graphics.pose().pushPose();
			graphics.pose().translate(sw / 2f, drawY, 0);
			graphics.pose().scale(scale, scale, 1f);
			graphics.pose().translate(-sw / 2f, -drawY, 0);
			graphics.drawString(mc.font, msg, drawX + 1, drawY + 1, shadowColor, false);
			graphics.drawString(mc.font, msg, drawX, drawY, color, true);
			graphics.pose().popPose();
		}
	}

	private static void renderReading(GuiGraphics graphics, int sw, int sh) {
		long now = System.currentTimeMillis();
		long left = ClientMadnessState.readingUntilMs() - now;
		float t = 1.0f - Math.max(0, Math.min(5000, left)) / 5000.0f;
		int gold = (int) (t * 80f);
		int color = (Math.min(255, gold) << 24) | 0x00FFD700;
		graphics.fill(0, 0, sw, sh, color);
		Minecraft mc = Minecraft.getInstance();
		Component msg = Component.translatable("superheroes.madness.reading").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
		int w = mc.font.width(msg);
		graphics.drawString(mc.font, msg, sw / 2 - w / 2, sh / 3, 0xFFFFE47A, true);
	}
}
