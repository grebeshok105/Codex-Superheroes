package com.example.superheroes.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SuperheroesClientConfig {
	public enum VfxMode { CUSTOM, LEGACY }

	/** Форма иконок способностей в HUD: круглые орбы или квадратные чипы. */
	public enum IconStyle { ROUND, SQUARE }

	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes");
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

	public static IconStyle iconStyle() {
		return DATA.iconStyle == null ? IconStyle.ROUND : DATA.iconStyle;
	}

	public static void setIconStyle(IconStyle style) {
		DATA.iconStyle = style;
		save();
	}

	public static void toggleIconStyle() {
		setIconStyle(iconStyle() == IconStyle.ROUND ? IconStyle.SQUARE : IconStyle.ROUND);
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
		} catch (IOException | RuntimeException e) {
			// Corrupt or unreadable config must not kill startup (audit,
			// потенциальные): Gson throws JsonParseException (a RuntimeException)
			// on malformed JSON, not IOException.
			LOGGER.warn("Could not read {}; falling back to defaults ({})", PATH, e.toString());
			DATA = new Data();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			// Write-then-rename: a crash mid-write can no longer leave a
			// truncated JSON that would break the next launch.
			Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
			try (Writer w = Files.newBufferedWriter(tmp)) {
				GSON.toJson(DATA, w);
			}
			try {
				Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Could not write {} ({})", PATH, e.toString());
		}
	}

	private static final class Data {
		VfxMode vfxMode = VfxMode.CUSTOM;
		IconStyle iconStyle = IconStyle.ROUND;
	}
}
