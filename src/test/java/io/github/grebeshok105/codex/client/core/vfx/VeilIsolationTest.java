package io.github.grebeshok105.codex.client.core.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Isolation audit for the Veil backend: {@code foundry.veil} may only be
 * imported inside {@code client/core/vfx/veil/}. Everything else in
 * {@code client/core} — and all of {@code src/main} — must stay Veil-free so
 * a stray import is a bug, not a style issue.
 */
class VeilIsolationTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();
	private static final Path CLIENT_JAVA = ROOT.resolve("src/client/java");
	private static final Path MAIN_JAVA = ROOT.resolve("src/main/java");
	private static final Path VEIL_BACKEND = CLIENT_JAVA.resolve(
			"io/github/grebeshok105/codex/client/core/vfx/veil/VeilVfxBackend.java");
	private static final String VEIL_IMPORT = "import foundry.veil";
	private static final String VEIL_ENCLAVE = "/client/core/vfx/veil/";

	/**
	 * Guards against a vacuous pass: {@link VfxBackends} resolves
	 * {@code client.core.vfx.veil.VeilVfxBackend} by {@code Class.forName}, so a
	 * missing class degrades silently to the fallback backend. This assertion
	 * fails until the Veil backend source exists.
	 */
	@Test
	void veilBackendClassExists() {
		assertTrue(Files.isRegularFile(VEIL_BACKEND),
				VEIL_BACKEND + " is missing — VfxBackends resolves it by name when Veil is loaded");
	}

	@Test
	void veilImportsOnlyInVeilPackage() throws IOException {
		List<String> violations = new ArrayList<>();
		try (Stream<Path> files = Files.walk(CLIENT_JAVA)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				String normalized = file.toString().replace('\\', '/');
				if (!normalized.contains("/client/core/") || normalized.contains(VEIL_ENCLAVE)) {
					continue;
				}
				if (Files.readString(file).contains(VEIL_IMPORT)) {
					violations.add(ROOT.relativize(file).toString());
				}
			}
		}
		assertTrue(violations.isEmpty(),
				"foundry.veil imports outside client/core/vfx/veil: " + violations);
	}

	@Test
	void noVeilReferencesInMain() throws IOException {
		List<String> violations = new ArrayList<>();
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				if (Files.readString(file).contains("foundry.veil")) {
					violations.add(ROOT.relativize(file).toString());
				}
			}
		}
		assertTrue(violations.isEmpty(),
				"foundry.veil references in src/main (dedicated server cannot load Veil): " + violations);
	}
}
