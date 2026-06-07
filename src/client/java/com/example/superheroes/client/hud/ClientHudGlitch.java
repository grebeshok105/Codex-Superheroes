package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMadnessState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.Random;

public final class ClientHudGlitch {
	private static final long STEP_INTERVAL_MS = 15000L;
	private static final float RAMP_BASE = 0.1f;
	private static final float RAMP_STEP_PER_INTERVAL = 0.1f;
	private static final float RAMP_CAP = 0.8f;
	private static final int BLOOD_BLACK = 0xFF330000;
	private static final Random RNG = new Random(0x1eaffeedL);

	private static long lastJitterMs = 0L;
	private static int jitterX = 0;
	private static int jitterY = 0;
	private static long lastBadgeJitterMs = 0L;
	private static int badgeJitterX = 0;
	private static int badgeJitterY = 0;

	private ClientHudGlitch() {
	}

	public static float ramp() {
		if (!ClientMadnessState.isMadness()) return 0f;
		long start = ClientMadnessState.madnessStartedAtMs();
		if (start <= 0L) return 0f;
		long elapsed = System.currentTimeMillis() - start;
		float steps = elapsed / (float) STEP_INTERVAL_MS;
		return Mth.clamp(RAMP_BASE + steps * RAMP_STEP_PER_INTERVAL, 0f, RAMP_CAP);
	}

	public static int jitterX() {
		updateJitter();
		return jitterX;
	}

	public static int jitterY() {
		updateJitter();
		return jitterY;
	}

	private static void updateJitter() {
		long now = System.currentTimeMillis();
		float r = ramp();
		if (r <= 0.001f) {
			jitterX = 0;
			jitterY = 0;
			return;
		}
		long interval = (long) Mth.lerp(r, 200f, 80f);
		if (now - lastJitterMs < interval) return;
		lastJitterMs = now;
		int amp = Math.max(1, (int) Math.ceil(r * 2.5f));
		jitterX = RNG.nextInt(amp * 2 + 1) - amp;
		jitterY = RNG.nextInt(amp * 2 + 1) - amp;
	}

	public static int badgeJitterX() {
		updateBadgeJitter();
		return badgeJitterX;
	}

	public static int badgeJitterY() {
		updateBadgeJitter();
		return badgeJitterY;
	}

	private static void updateBadgeJitter() {
		long now = System.currentTimeMillis();
		float r = ramp();
		if (r <= 0.001f) {
			badgeJitterX = 0;
			badgeJitterY = 0;
			return;
		}
		long interval = (long) Mth.lerp(r, 130f, 50f);
		if (now - lastBadgeJitterMs < interval) return;
		lastBadgeJitterMs = now;
		int amp = Math.max(2, (int) Math.ceil(r * 4.0f));
		badgeJitterX = RNG.nextInt(amp * 2 + 1) - amp;
		badgeJitterY = RNG.nextInt(amp * 2 + 1) - amp;
	}

	public static int tintColor(int baseArgb) {
		float r = ramp();
		if (r <= 0.001f) return baseArgb;
		int a = (baseArgb >>> 24) & 0xFF;
		int br = (baseArgb >>> 16) & 0xFF;
		int bg = (baseArgb >>> 8) & 0xFF;
		int bb = baseArgb & 0xFF;
		int tr = (BLOOD_BLACK >>> 16) & 0xFF;
		int tg = (BLOOD_BLACK >>> 8) & 0xFF;
		int tb = BLOOD_BLACK & 0xFF;
		int outR = (int) Mth.lerp(r, br, tr);
		int outG = (int) Mth.lerp(r, bg, tg);
		int outB = (int) Mth.lerp(r, bb, tb);
		return (a << 24) | (outR << 16) | (outG << 8) | outB;
	}

	public static boolean ghostDouble() {
		return ramp() >= 0.5f;
	}

	public static int ghostOffsetX() {
		float r = ramp();
		return (int) Mth.lerp(r, 0f, 2f);
	}

	public static boolean shouldObfuscate() {
		return ramp() >= 0.05f;
	}

	public static float obfuscateChance() {
		return ramp();
	}

	public static Component maybeObfuscate(Component component) {
		float chance = obfuscateChance();
		if (chance <= 0.001f) {
			return component;
		}
		if (RNG.nextFloat() >= chance) {
			return component;
		}
		MutableComponent copy = component.copy();
		return copy.withStyle(ChatFormatting.OBFUSCATED);
	}

	public static String maybeObfuscateString(String text) {
		float chance = obfuscateChance();
		if (chance <= 0.001f || RNG.nextFloat() >= chance) {
			return text;
		}
		return ChatFormatting.OBFUSCATED + text + ChatFormatting.RESET;
	}
}
