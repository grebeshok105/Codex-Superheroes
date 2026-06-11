package com.example.superheroes.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
	public static final String CATEGORY = "key.categories.superheroes";

	public static KeyMapping RADIAL;
	public static KeyMapping BINDINGS;
	public static KeyMapping TOGGLE_TOOLTIPS;
	public static KeyMapping SUPER_JUMP;
	public static KeyMapping VFX_SETTINGS;
	public static KeyMapping RAIDEN_SWORD_DRAW;
	public static KeyMapping HOTBAR_LOCK;
	public static KeyMapping[] ABILITY_SLOTS;

	private static final int[] DEFAULT_SLOT_KEYS = {
			GLFW.GLFW_KEY_Z,
			GLFW.GLFW_KEY_X,
			GLFW.GLFW_KEY_C,
			GLFW.GLFW_KEY_V,
			GLFW.GLFW_KEY_B,
			GLFW.GLFW_KEY_3,
			GLFW.GLFW_KEY_4,
			GLFW.GLFW_KEY_5
	};

	private ModKeys() {
	}

	public static void init() {
		RADIAL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.radial",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				CATEGORY));
		BINDINGS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.bindings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_COMMA,
				CATEGORY));
		TOGGLE_TOOLTIPS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.toggle_tooltips",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				CATEGORY));
		SUPER_JUMP = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.super_jump",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				CATEGORY));
		VFX_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.vfx_settings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				CATEGORY));
		RAIDEN_SWORD_DRAW = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.raiden_sword_draw",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F,
				CATEGORY));
		HOTBAR_LOCK = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superheroes.hotbar_lock",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_L,
				CATEGORY));
		ABILITY_SLOTS = new KeyMapping[DEFAULT_SLOT_KEYS.length];
		for (int i = 0; i < DEFAULT_SLOT_KEYS.length; i++) {
			ABILITY_SLOTS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
					"key.superheroes.ability_" + (i + 1),
					InputConstants.Type.KEYSYM,
					DEFAULT_SLOT_KEYS[i],
					CATEGORY));
		}
	}
}
