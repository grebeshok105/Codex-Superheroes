package io.github.grebeshok105.codex.client.core.vfx.backend;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the active {@link VfxBackend}: the Veil backend when Veil is loaded,
 * otherwise {@link FallbackVfxBackend}.
 *
 * <p>The Veil backend class is resolved by name rather than imported: this
 * keeps {@code foundry.veil} out of every file outside {@code vfx/veil/} and
 * lets the seam compile before that package exists. If Veil is loaded but the
 * backend class is missing or fails to link, the failure is logged once and
 * the fallback serves the session.
 */
public final class VfxBackends {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-vfx");
	private static final String VEIL_BACKEND_CLASS =
			"io.github.grebeshok105.codex.client.core.vfx.veil.VeilVfxBackend";

	private static volatile VfxBackend current;
	private static boolean veilBackendFailed;

	private VfxBackends() {
	}

	public static VfxBackend current() {
		VfxBackend backend = current;
		if (backend == null) {
			backend = resolve();
			current = backend;
		}
		return backend;
	}

	private static VfxBackend resolve() {
		if (FabricLoader.getInstance().isModLoaded("veil")) {
			try {
				return (VfxBackend) Class.forName(VEIL_BACKEND_CLASS)
						.getDeclaredConstructor().newInstance();
			} catch (Throwable t) {
				if (!veilBackendFailed) {
					veilBackendFailed = true;
					LOGGER.warn("Veil is loaded but the VFX backend is unavailable; using the fallback", t);
				}
			}
		}
		return new FallbackVfxBackend();
	}
}
