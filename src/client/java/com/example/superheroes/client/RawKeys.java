package com.example.superheroes.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Polls the physical key bound to a KeyMapping directly via GLFW with edge detection.
 *
 * Why: vanilla's KeyMapping.MAP allows only ONE mapping per physical key. Our ability
 * binds (3/4/5) and hotbar lock (L) collide with vanilla hotbar slots / advancements,
 * so consumeClick() silently never fires depending on hash order. Raw polling always works
 * and still respects user rebinds (reads the currently bound key).
 */
public final class RawKeys {
	private static final Map<KeyMapping, Boolean> PREV = new IdentityHashMap<>();

	private RawKeys() {
	}

	/** True exactly once per physical press, only when no screen/overlay is open. */
	public static boolean pressed(KeyMapping mapping) {
		Minecraft mc = Minecraft.getInstance();
		boolean down = isDown(mapping, mc);
		boolean was = Boolean.TRUE.equals(PREV.put(mapping, down));
		if (mc.screen != null || mc.getOverlay() != null) {
			return false;
		}
		return down && !was;
	}

	private static boolean isDown(KeyMapping mapping, Minecraft mc) {
		InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
		if (key == null || key.getType() != InputConstants.Type.KEYSYM
				|| key.getValue() == InputConstants.UNKNOWN.getValue()) {
			return mapping.isDown();
		}
		return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
	}

	/** Throw away queued KeyMapping clicks so conflict winners do not leak actions. */
	public static void drain(KeyMapping mapping) {
		while (mapping.consumeClick()) {
			// discard
		}
	}
}
