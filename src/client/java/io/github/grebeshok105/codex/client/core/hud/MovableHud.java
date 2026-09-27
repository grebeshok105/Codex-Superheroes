package io.github.grebeshok105.codex.client.core.hud;

/** A HUD element the player can drag in the HUD editor. {@link #layoutId()} is persisted in the layout config. */
public interface MovableHud {
	String layoutId();

	/** Where the element is drawn at the current offset — the same math its render uses. */
	HudBounds bounds(int screenWidth, int screenHeight);
}
