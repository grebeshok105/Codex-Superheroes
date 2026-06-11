package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.sound.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Джарвис: голосовой доклад об обнаруженном герое + инфопанель.
 * Звук запускается на клиенте, инфопанель появляется СТРОГО после его
 * окончания. S-tier угрозы используют excited звук.
 */
public final class JarvisDetectionHud {
	private static final int SOUND_TICKS = 212;
	private static final int INFO_TICKS = 160;
	private static final int INFO_FADE_TICKS = 12;

	private record Detection(String playerName, ResourceLocation heroId, int distance,
			String threatClass, String jarvisQuote) {
	}

	private enum Phase {
		IDLE, SOUND, INFO
	}

	private static final Deque<Detection> QUEUE = new ArrayDeque<>();
	private static Phase phase = Phase.IDLE;
	private static int timer;
	private static Detection current;

	private JarvisDetectionHud() {
	}

	public static void onDetection(String playerName, ResourceLocation heroId, int distance,
			String threatClass, String jarvisQuote) {
		QUEUE.addLast(new Detection(playerName, heroId, distance, threatClass, jarvisQuote));
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || !IronManHero.ID.equals(ClientHeroState.data().heroId())) {
			QUEUE.clear();
			phase = Phase.IDLE;
			current = null;
			return;
		}
		switch (phase) {
			case IDLE -> {
				if (!QUEUE.isEmpty()) {
					current = QUEUE.pollFirst();
					phase = Phase.SOUND;
					timer = SOUND_TICKS;
					if ("S".equals(current.threatClass())) {
						mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.IRONMAN_JARVIS_DETECT_EXCITED, 1.0f, 1.0f));
					} else {
						mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.IRONMAN_JARVIS_DETECT, 1.0f, 1.0f));
					}
				}
			}
			case SOUND -> {
				if (--timer <= 0) {
					phase = Phase.INFO;
					timer = INFO_TICKS;
				}
			}
			case INFO -> {
				if (--timer <= 0) {
					phase = Phase.IDLE;
					current = null;
				}
			}
		}
	}

	public static void clear() {
		QUEUE.clear();
		phase = Phase.IDLE;
		current = null;
	}

	public static void render(GuiGraphics g, DeltaTracker tracker) {
		if (phase != Phase.INFO || current == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		float alpha = 1f;
		if (timer < INFO_FADE_TICKS) {
			alpha = timer / (float) INFO_FADE_TICKS;
		} else if (INFO_TICKS - timer < INFO_FADE_TICKS) {
			alpha = (INFO_TICKS - timer) / (float) INFO_FADE_TICKS;
		}

		int threatColor = threatLabelColor(current.threatClass());
		String threatDesc = threatDescription(current.threatClass());

		Component title = Component.translatable("hud.superheroes.jarvis.detect.title");
		Component line1 = Component.translatable("hud.superheroes.jarvis.detect.player", current.playerName());
		Component heroName = Component.translatable("hero.superheroes." + current.heroId().getPath());
		Component line2 = Component.literal("■ ")
				.append(heroName)
				.append(" [" + current.threatClass() + "] — " + current.distance() + "m");
		Component line3 = Component.literal(threatDesc);
		Component line4 = Component.literal("\"" + current.jarvisQuote() + "\"");

		int pad = HudScaler.scale(6);
		int lineH = mc.font.lineHeight + HudScaler.scale(2);
		int w = mc.font.width(title);
		w = Math.max(w, mc.font.width(line1));
		w = Math.max(w, mc.font.width(line2));
		w = Math.max(w, mc.font.width(line3));
		w = Math.max(w, Math.min(mc.font.width(line4), 200));
		w += pad * 2;
		int h = pad * 2 + lineH * 5 + HudScaler.scale(3);

		int sw = mc.getWindow().getGuiScaledWidth();
		int x = sw - w - HudScaler.scale(10);
		int y = HudScaler.scale(40);

		int a = (int) (alpha * 255);
		int borderCol = "S".equals(current.threatClass()) ? 0xFF2020 : 0xFFC400;
		int glowCol = "S".equals(current.threatClass()) ? 0xFF2020 : 0xFFC400;
		HudUtil.neonPanel(g, x, y, w, h,
				withAlpha(0x181C2A, (int) (a * 0.88)),
				withAlpha(0x080A14, (int) (a * 0.82)),
				withAlpha(borderCol, (int) (a * 0.65)),
				withAlpha(glowCol, (int) (a * 0.22)));

		int tx = x + pad;
		int ty = y + pad;
		g.drawString(mc.font, title, tx, ty, withAlpha(0xFFD24A, a), true);
		ty += lineH + HudScaler.scale(3);
		g.drawString(mc.font, line1, tx, ty, withAlpha(0xD8DCE8, a), true);
		ty += lineH;
		g.drawString(mc.font, line2, tx, ty, withAlpha(threatColor, a), true);
		ty += lineH;
		g.drawString(mc.font, line3, tx, ty, withAlpha(threatColor, a), true);
		ty += lineH;
		g.drawString(mc.font, line4, tx, ty, withAlpha(0x8890A8, a), true);
	}

	private static int threatLabelColor(String threatClass) {
		return switch (threatClass) {
			case "S" -> 0xFF4444;
			case "A" -> 0xFF6666;
			case "B" -> 0xFFAA00;
			case "C" -> 0xFFFF55;
			case "D" -> 0x55FF55;
			default -> 0xD8DCE8;
		};
	}

	private static String threatDescription(String threatClass) {
		return switch (threatClass) {
			case "S" -> "ЗАПРЕДЕЛЬНАЯ УГРОЗА";
			case "A" -> "ВЫСОКАЯ УГРОЗА";
			case "B" -> "СРЕДНЯЯ УГРОЗА";
			case "C" -> "НИЗКАЯ УГРОЗА";
			case "D" -> "МИНИМАЛЬНАЯ УГРОЗА";
			default -> "НЕИЗВЕСТНО";
		};
	}

	private static int withAlpha(int rgb, int alpha) {
		int a = Math.max(4, Math.min(255, alpha));
		return (a << 24) | (rgb & 0x00FFFFFF);
	}
}
