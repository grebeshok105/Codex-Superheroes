package com.example.superheroes.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores user-defined drag offsets for every movable HUD element.
 * Offsets are in gui-scaled pixels, applied on top of the default anchor position.
 * Persisted to config/superheroes-hud-layout.json.
 */
public final class HudLayoutManager {
	public static final String HERO_PANEL = "hero_panel";
	public static final String HOTBAR = "hotbar";
	public static final String ABILITY_BAR = "ability_bar";
	public static final String CHAT = "chat";
	public static final String EFFECTS = "effects";
	public static final String MELEE_CHARGE = "melee_charge";
	public static final String TOOLTIPS = "tooltips";

	public static final String[] ALL = {HERO_PANEL, HOTBAR, ABILITY_BAR, CHAT, EFFECTS, MELEE_CHARGE, TOOLTIPS};

	private static final int[] ZERO = {0, 0};
	private static final Map<String, int[]> OFFSETS = new HashMap<>();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static boolean loaded = false;

	private HudLayoutManager() {
	}

	public static int[] offset(String id) {
		ensureLoaded();
		int[] off = OFFSETS.get(id);
		return off != null ? off : ZERO;
	}

	public static void setOffset(String id, int dx, int dy) {
		ensureLoaded();
		OFFSETS.put(id, new int[]{dx, dy});
	}

	public static void resetAll() {
		OFFSETS.clear();
		save();
	}

	public static synchronized void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path file = configFile();
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : root.entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				OFFSETS.put(e.getKey(), new int[]{o.get("x").getAsInt(), o.get("y").getAsInt()});
			}
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, int[]> e : OFFSETS.entrySet()) {
			JsonObject o = new JsonObject();
			o.addProperty("x", e.getValue()[0]);
			o.addProperty("y", e.getValue()[1]);
			root.add(e.getKey(), o);
		}
		try {
			Files.writeString(configFile(), GSON.toJson(root));
		} catch (IOException ignored) {
		}
	}

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("superheroes-hud-layout.json");
	}
}
