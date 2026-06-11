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
 * окончания (длительность реплики ~10.4 c => 212 тиков с запасом).
 * Несколько обнаружений выстраиваются в очередь.
 */
public final class JarvisDetectionHud {
	private static final int SOUND_TICKS = 212;
	private static final int INFO_TICKS = 160;
	private static final int INFO_FADE_TICKS = 12;

	private record Detection(String playerName, ResourceLocation heroId, int distance) {
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

	public static void onDetection(String playerName, ResourceLocation heroId, int distance) {
		QUEUE.addLast(new Detection(playerName, heroId, distance));
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || !IronManHero.ID.equals(ClientHeroState.data().heroId())) {
			// снял костюм/вышел из мира — сбрасываем всё
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
					mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.IRONMAN_JARVIS_DETECT, 1.0f, 1.0f));
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

		Component title = Component.translatable("hud.superheroes.jarvis.detect.title");
		Component line1 = Component.translatable("hud.superheroes.jarvis.detect.player", current.playerName());
		Component line2 = Component.translatable("hud.superheroes.jarvis.detect.hero",
				Component.translatable("hero.superheroes." + current.heroId().getPath()));
		Component line3 = Component.translatable("hud.superheroes.jarvis.detect.distance", current.distance());

		int pad = HudScaler.scale(6);
		int lineH = mc.font.lineHeight + HudScaler.scale(2);
		int w = mc.font.width(title);
		w = Math.max(w, mc.font.width(line1));
		w = Math.max(w, mc.font.width(line2));
		w = Math.max(w, mc.font.width(line3));
		w += pad * 2;
		int h = pad * 2 + lineH * 4 + HudScaler.scale(3);

		int sw = mc.getWindow().getGuiScaledWidth();
		int x = sw - w - HudScaler.scale(10);
		int y = HudScaler.scale(40);

		int a = (int) (alpha * 255);
		HudUtil.neonPanel(g, x, y, w, h,
				withAlpha(0x181C2A, (int) (a * 0.88)),
				withAlpha(0x080A14, (int) (a * 0.82)),
				withAlpha(0xFFC400, (int) (a * 0.65)),
				withAlpha(0xFFC400, (int) (a * 0.22)));

		int tx = x + pad;
		int ty = y + pad;
		g.drawString(mc.font, title, tx, ty, withAlpha(0xFFD24A, a), true);
		ty += lineH + HudScaler.scale(3);
		g.drawString(mc.font, line1, tx, ty, withAlpha(0xD8DCE8, a), true);
		ty += lineH;
		g.drawString(mc.font, line2, tx, ty, withAlpha(0xD8DCE8, a), true);
		ty += lineH;
		g.drawString(mc.font, line3, tx, ty, withAlpha(0x9AA4B8, a), true);
	}

	private static int withAlpha(int rgb, int alpha) {
		int a = Math.max(4, Math.min(255, alpha));
		return (a << 24) | (rgb & 0x00FFFFFF);
	}
}
