package io.github.grebeshok105.codex.client.core;

import net.minecraft.network.chat.Component;

/**
 * One source of HUD corruption (pose jitter, badge jitter, ghost double-render, colour
 * bleed, text obfuscation). {@link HudJitter} delegates to the strongest active source —
 * one whose {@link #ramp()} is above the idle floor — and answers identity/zero otherwise,
 * so shared HUD code never names the hero that owns the glitch.
 */
public interface HudGlitchSource {
	/** Current corruption level in {@code [0,1]}; {@code 0} means idle. */
	float ramp();

	int jitterX();

	int jitterY();

	int badgeJitterX();

	int badgeJitterY();

	boolean ghostDouble();

	int ghostOffsetX();

	/** The base colour shifted toward this source's corruption tint; returns it unchanged when idle. */
	int tintColor(int baseArgb);

	Component maybeObfuscate(Component component);
}
