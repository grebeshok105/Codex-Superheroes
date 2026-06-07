package com.example.superheroes.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SuperheroesClientConfig {
	public enum VfxMode { CUSTOM, LEGACY }

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("superheroes-client.json");
	private static Data DATA = new Data();

	private SuperheroesClientConfig() {
	}

	public static VfxMode vfxMode() {
		return DATA.vfxMode == null ? VfxMode.CUSTOM : DATA.vfxMode;
	}

	public static void setVfxMode(VfxMode mode) {
		DATA.vfxMode = mode;
		save();
	}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				try (Reader r = Files.newBufferedReader(PATH)) {
					Data d = GSON.fromJson(r, Data.class);
					if (d != null) DATA = d;
				}
			} else {
				save();
			}
		} catch (IOException e) {
			DATA = new Data();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer w = Files.newBufferedWriter(PATH)) {
				GSON.toJson(DATA, w);
			}
		} catch (IOException ignored) {
		}
	}

	private static final class Data {
		VfxMode vfxMode = VfxMode.CUSTOM;
	}
}
