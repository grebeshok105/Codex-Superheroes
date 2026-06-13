package com.example.superheroes.client.iris;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Drives Iris for Pandora's death cinematic — the "Vanitas" shaderpack that paints every solid
 * pixel pure black and the background a red→white gradient (#6). NEVER touches Veil.
 *
 * <p>The pack is shipped inside the mod jar ({@code assets/superheroes/pandora_vanitas/...}) and
 * extracted to {@code shaderpacks/PandoraVanitas/} on first use. We snapshot the player's current
 * Iris state (enabled flag + selected pack) before swapping and restore it afterwards. A tiny
 * on-disk marker survives a crash so the player's shader choice is never permanently hijacked.
 *
 * <p>All Iris calls are wrapped against {@link Throwable} and guarded by {@link #isIrisLoaded()},
 * so the mod stays fully functional without Iris.
 */
public final class PandoraVanitasBridge {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-vanitas-bridge");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final String PACK_NAME = "PandoraVanitas";
	private static final String RESOURCE_ROOT = "/assets/superheroes/pandora_vanitas";
	private static final String[] GBUFFERS = {
			"gbuffers_basic", "gbuffers_textured", "gbuffers_textured_lit", "gbuffers_terrain",
			"gbuffers_entities", "gbuffers_skybasic", "gbuffers_skytextured", "gbuffers_clouds",
			"gbuffers_weather", "gbuffers_hand", "gbuffers_water", "gbuffers_block",
			"gbuffers_beaconbeam", "gbuffers_armor_glint", "gbuffers_spidereyes", "gbuffers_damagedblock",
			"final"
	};

	private static Snapshot active;

	private static final class Snapshot {
		boolean shadersWereEnabled;
		String previousPackName; // null = none
	}

	private PandoraVanitasBridge() {
	}

	public static boolean isIrisLoaded() {
		return FabricLoader.getInstance().isModLoaded("iris");
	}

	/** Swap Iris to the Vanitas death pack for the cinematic. No-op without Iris. */
	public static void apply() {
		if (!isIrisLoaded()) {
			return;
		}
		try {
			Path pack = ensurePackExtracted();
			net.irisshaders.iris.config.IrisConfig config = net.irisshaders.iris.Iris.getIrisConfig();

			Snapshot snapshot = new Snapshot();
			snapshot.shadersWereEnabled = net.irisshaders.iris.api.v0.IrisApi.getInstance().getConfig().areShadersEnabled();
			snapshot.previousPackName = config.getShaderPackName().orElse(null);
			active = snapshot;
			persist(snapshot);

			config.setShaderPackName(pack.getFileName().toString());
			config.setShadersEnabled(true);
			config.save();
			net.irisshaders.iris.Iris.reload();
		} catch (Throwable t) {
			LOGGER.error("Vanitas bridge failed to apply death shaderpack", t);
		}
	}

	/** Restore the player's previous shader state after the cinematic. */
	public static void restore() {
		if (!isIrisLoaded()) {
			return;
		}
		Snapshot snapshot = active != null ? active : readMarker();
		if (snapshot == null) {
			return;
		}
		try {
			net.irisshaders.iris.config.IrisConfig config = net.irisshaders.iris.Iris.getIrisConfig();
			config.setShaderPackName(snapshot.previousPackName);
			config.setShadersEnabled(snapshot.shadersWereEnabled);
			config.save();
			net.irisshaders.iris.Iris.reload();
		} catch (Throwable t) {
			LOGGER.error("Vanitas bridge failed to restore previous shader state", t);
		} finally {
			active = null;
			deleteMarker();
		}
	}

	/** Called on client start: heal shader config if the game died mid-cinematic. */
	public static void restoreAfterCrashIfNeeded() {
		if (!isIrisLoaded()) {
			return;
		}
		Snapshot snapshot = readMarker();
		if (snapshot == null) {
			return;
		}
		LOGGER.info("Found a Vanitas restore marker from a previous session, restoring shader state");
		active = snapshot;
		restore();
	}

	private static Path ensurePackExtracted() throws IOException {
		Path dir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks").resolve(PACK_NAME);
		Path shaders = dir.resolve("shaders");
		Files.createDirectories(shaders);
		copyResource(RESOURCE_ROOT + "/shaders.properties", dir.resolve("shaders.properties"));
		for (String name : GBUFFERS) {
			copyResource(RESOURCE_ROOT + "/shaders/" + name + ".vsh", shaders.resolve(name + ".vsh"));
			copyResource(RESOURCE_ROOT + "/shaders/" + name + ".fsh", shaders.resolve(name + ".fsh"));
		}
		return dir;
	}

	private static void copyResource(String resourcePath, Path target) throws IOException {
		try (InputStream in = PandoraVanitasBridge.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				return; // a missing optional program simply falls back inside Iris
			}
			Files.createDirectories(target.getParent());
			Files.write(target, in.readAllBytes());
		}
	}

	private static Path markerFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("superheroes").resolve("vanitas_restore.json");
	}

	private static void persist(Snapshot snapshot) {
		try {
			Path path = markerFile();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(snapshot), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Could not persist Vanitas restore marker", e);
		}
	}

	private static Snapshot readMarker() {
		try {
			Path path = markerFile();
			if (!Files.exists(path)) {
				return null;
			}
			return GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Snapshot.class);
		} catch (Exception e) {
			return null;
		}
	}

	private static void deleteMarker() {
		try {
			Files.deleteIfExists(markerFile());
		} catch (IOException e) {
			LOGGER.warn("Could not delete Vanitas restore marker", e);
		}
	}
}
