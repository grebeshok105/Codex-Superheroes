package com.example.superheroes.client.iris;

import com.example.superheroes.network.MirrorDimensionStatusC2SPayload;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The ONLY class allowed to touch Iris. Everything is guarded so the mod
 * stays fully functional without Iris installed.
 *
 * Strategy: snapshot the user's current shader state (enabled flag, selected
 * pack, Acid options file), write the Acid "round world" options
 * (MODE/J shader options -> shaderpacks/&lt;pack&gt;.txt), select the Acid pack and
 * enable shaders. Restoring puts back the exact previous state.
 *
 * Pack selection uses Iris internals (net.irisshaders.iris.Iris) because the
 * public v0 API cannot pick a pack; all calls are wrapped against Throwable
 * so a future Iris refactor degrades to a clean failure instead of a crash.
 */
public final class IrisShaderBridge {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-iris-bridge");

	private static MirrorRestoreFile.Snapshot activeSnapshot;

	private IrisShaderBridge() {
	}

	public static boolean isIrisLoaded() {
		return FabricLoader.getInstance().isModLoaded("iris");
	}

	/** True when we can actually drive a warp (Iris present AND an Acid pack found). */
	public static boolean canWarp() {
		return isIrisLoaded() && findAcidPack() != null;
	}

	/** @return a {@link MirrorDimensionStatusC2SPayload} status code. */
	public static int applyAcid(int mode, int scale) {
		if (!isIrisLoaded()) {
			return MirrorDimensionStatusC2SPayload.NO_IRIS;
		}
		Path pack = findAcidPack();
		if (pack == null) {
			return MirrorDimensionStatusC2SPayload.NO_PACK;
		}
		try {
			return applyAcidUnsafe(pack, mode, scale);
		} catch (Throwable t) {
			LOGGER.error("Iris bridge failed to apply Acid shaders", t);
			return MirrorDimensionStatusC2SPayload.IRIS_API_FAIL;
		}
	}

	/** @return true when a restore actually ran. */
	public static boolean restore() {
		MirrorRestoreFile.Snapshot snapshot = activeSnapshot;
		activeSnapshot = null;
		if (snapshot == null) {
			MirrorRestoreFile.delete();
			return false;
		}
		boolean ok = restoreSnapshot(snapshot);
		MirrorRestoreFile.delete();
		return ok;
	}

	/**
	 * True when shaders are on AND the selected pack is the Acid pack, i.e. the
	 * warp is actually rendering. Used to detect that the warp got dropped
	 * (alt-tab pause, a foreign Iris reload, the user toggling shaders) so we
	 * can heal it without losing the original snapshot.
	 */
	public static boolean isAcidWarpActive() {
		if (!isIrisLoaded()) {
			return false;
		}
		try {
			if (!net.irisshaders.iris.api.v0.IrisApi.getInstance().getConfig().areShadersEnabled()) {
				return false;
			}
			String current = net.irisshaders.iris.Iris.getIrisConfig().getShaderPackName().orElse("");
			return current != null && current.toLowerCase(Locale.ROOT).contains("acid");
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Re-applies the Acid warp WITHOUT taking a new snapshot (the original is
	 * still held in {@link #activeSnapshot}). Called when the warp got dropped
	 * while the ability is still active so the victim never escapes by losing
	 * focus. No-op when we have no active session.
	 */
	public static void reassertAcid(int mode, int scale) {
		if (!isIrisLoaded() || activeSnapshot == null) {
			return;
		}
		Path pack = findAcidPack();
		if (pack == null) {
			return;
		}
		try {
			net.irisshaders.iris.config.IrisConfig config = net.irisshaders.iris.Iris.getIrisConfig();
			Path optionsFile = pack.resolveSibling(pack.getFileName() + ".txt");
			Files.writeString(optionsFile, "MODE=" + mode + "\nJ=" + scale + "\n", StandardCharsets.UTF_8);
			config.setShaderPackName(pack.getFileName().toString());
			config.setShadersEnabled(true);
			config.save();
			net.irisshaders.iris.Iris.reload();
		} catch (Throwable t) {
			LOGGER.error("Iris bridge failed to re-assert Acid shaders", t);
		}
	}

	/** Called once on client start: heals shader config after a crash mid-warp. */
	public static void restoreAfterCrashIfNeeded() {
		MirrorRestoreFile.Snapshot snapshot = MirrorRestoreFile.read();
		if (snapshot == null) {
			return;
		}
		LOGGER.info("Found mirror dimension restore snapshot from a previous session, restoring shader state");
		restoreSnapshot(snapshot);
		MirrorRestoreFile.delete();
	}

	private static int applyAcidUnsafe(Path pack, int mode, int scale) throws Throwable {
		net.irisshaders.iris.config.IrisConfig config = net.irisshaders.iris.Iris.getIrisConfig();

		MirrorRestoreFile.Snapshot snapshot = new MirrorRestoreFile.Snapshot();
		snapshot.shadersWereEnabled = net.irisshaders.iris.api.v0.IrisApi.getInstance().getConfig().areShadersEnabled();
		snapshot.previousPackName = config.getShaderPackName().orElse(null);
		Path optionsFile = pack.resolveSibling(pack.getFileName() + ".txt");
		snapshot.acidOptionsFileName = optionsFile.getFileName().toString();
		snapshot.previousAcidOptions = Files.exists(optionsFile)
				? Files.readString(optionsFile, StandardCharsets.UTF_8) : null;
		activeSnapshot = snapshot;
		MirrorRestoreFile.write(snapshot);

		Files.writeString(optionsFile, "MODE=" + mode + "\nJ=" + scale + "\n", StandardCharsets.UTF_8);
		config.setShaderPackName(pack.getFileName().toString());
		config.setShadersEnabled(true);
		config.save();
		net.irisshaders.iris.Iris.reload();
		return MirrorDimensionStatusC2SPayload.OK_APPLIED;
	}

	private static boolean restoreSnapshot(MirrorRestoreFile.Snapshot snapshot) {
		if (!isIrisLoaded()) {
			return false;
		}
		try {
			Path shaderpacks = shaderpacksDir();
			if (snapshot.acidOptionsFileName != null) {
				Path optionsFile = shaderpacks.resolve(snapshot.acidOptionsFileName);
				if (snapshot.previousAcidOptions != null) {
					Files.writeString(optionsFile, snapshot.previousAcidOptions, StandardCharsets.UTF_8);
				} else {
					Files.deleteIfExists(optionsFile);
				}
			}
			net.irisshaders.iris.config.IrisConfig config = net.irisshaders.iris.Iris.getIrisConfig();
			config.setShaderPackName(snapshot.previousPackName);
			config.setShadersEnabled(snapshot.shadersWereEnabled);
			config.save();
			net.irisshaders.iris.Iris.reload();
			return true;
		} catch (Throwable t) {
			LOGGER.error("Iris bridge failed to restore previous shader state", t);
			return false;
		}
	}

	private static Path shaderpacksDir() {
		return FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
	}

	private static Path findAcidPack() {
		Path dir = shaderpacksDir();
		if (!Files.isDirectory(dir)) {
			return null;
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files
					.filter(p -> {
						String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
						return name.contains("acid") && !name.endsWith(".txt");
					})
					.findFirst()
					.orElse(null);
		} catch (IOException e) {
			LOGGER.warn("Could not scan shaderpacks directory", e);
			return null;
		}
	}
}
