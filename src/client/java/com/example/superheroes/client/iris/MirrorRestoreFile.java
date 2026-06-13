package com.example.superheroes.client.iris;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Crash-resilience for the Mirror Dimension: before the mod touches the
 * victim's Iris setup it snapshots the previous state to disk. If the game
 * dies while the Acid warp is active, the snapshot is re-applied on next
 * client start so nobody's shader config is ever permanently hijacked.
 */
public final class MirrorRestoreFile {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-mirror-restore");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final class Snapshot {
		public boolean shadersWereEnabled;
		public String previousPackName; // null = none selected
		public String acidOptionsFileName; // e.g. "Acid Shaders.zip.txt"
		public String previousAcidOptions; // null = options file did not exist
	}

	private MirrorRestoreFile() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("superheroes").resolve("mirror_restore.json");
	}

	public static void write(Snapshot snapshot) {
		try {
			Path path = file();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(snapshot), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Could not persist mirror dimension restore snapshot", e);
		}
	}

	public static Snapshot read() {
		try {
			Path path = file();
			if (!Files.exists(path)) {
				return null;
			}
			return GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Snapshot.class);
		} catch (Exception e) {
			LOGGER.warn("Could not read mirror dimension restore snapshot", e);
			return null;
		}
	}

	public static void delete() {
		try {
			Files.deleteIfExists(file());
		} catch (IOException e) {
			LOGGER.warn("Could not delete mirror dimension restore snapshot", e);
		}
	}
}
