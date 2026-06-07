package com.example.superheroes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

public final class ClientReinhardTimeSlowState {
	private static volatile boolean active;
	private static volatile long activatedAtMs;

	private ClientReinhardTimeSlowState() {
	}

	public static void update(boolean newActive) {
		boolean wasActive = active;
		active = newActive;
		if (newActive && !wasActive) {
			activatedAtMs = System.currentTimeMillis();
			silenceWorldSounds();
		}
	}

	private static void silenceWorldSounds() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getSoundManager() == null) return;
		mc.getSoundManager().stop(null, SoundSource.BLOCKS);
		mc.getSoundManager().stop(null, SoundSource.HOSTILE);
		mc.getSoundManager().stop(null, SoundSource.NEUTRAL);
		mc.getSoundManager().stop(null, SoundSource.AMBIENT);
		mc.getSoundManager().stop(null, SoundSource.WEATHER);
		mc.getSoundManager().stop(null, SoundSource.RECORDS);
	}

	public static boolean active() {
		return active;
	}

	public static long activatedAtMs() {
		return activatedAtMs;
	}
}
