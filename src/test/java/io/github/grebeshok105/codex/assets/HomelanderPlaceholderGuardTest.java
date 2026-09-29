package io.github.grebeshok105.codex.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard for the Homelander placeholder manifest
 * (src/test/resources/contracts/homelander_placeholders.txt). Every uncommented
 * {@code <repo-relative path> <sha256>} line must still point at the placeholder file
 * this task created; a replaced file gets its line commented {@code # replaced} and is
 * never deleted, so the list stays truthful about what shipped as a placeholder.
 */
class HomelanderPlaceholderGuardTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();
	private static final Path MANIFEST = ROOT.resolve("src/test/resources/contracts/homelander_placeholders.txt");
	private static final String REPLACED_PREFIX = "# replaced";

	private record ManifestEntry(Path path, String sha256) {}

	@Test
	void manifestHashesMatchFiles() throws IOException {
		for (ManifestEntry entry : activeEntries()) {
			Path file = ROOT.resolve(entry.path());
			assertTrue(Files.exists(file), "manifest file missing " + file);
			assertEquals(entry.sha256(), sha256(file),
					entry.path() + " no longer matches its recorded placeholder hash; "
							+ "comment the line '# replaced' instead of leaving a stale hash");
		}
	}

	@Test
	@Disabled("enabled by the OMP branch (its Task 11); verified in Task 15")
	void finalBuildHasNoPlaceholders() throws IOException {
		List<ManifestEntry> active = activeEntries();
		assertTrue(active.isEmpty(),
				"placeholders still ship: " + active);
		for (ManifestEntry entry : replacedEntries()) {
			Path file = ROOT.resolve(entry.path());
			assertTrue(Files.exists(file),
					entry.path() + " marked '# replaced' but the file is gone");
			assertNotEquals(entry.sha256(), sha256(file),
					entry.path() + " marked '# replaced' still carries the placeholder hash");
		}
	}

	private static List<ManifestEntry> activeEntries() throws IOException {
		List<ManifestEntry> entries = new ArrayList<>();
		for (String line : manifestLines()) {
			if (line.startsWith("#")) {
				continue;
			}
			entries.add(parse(line));
		}
		return entries;
	}

	private static List<ManifestEntry> replacedEntries() throws IOException {
		List<ManifestEntry> entries = new ArrayList<>();
		for (String line : manifestLines()) {
			if (line.startsWith(REPLACED_PREFIX)) {
				entries.add(parse(line.substring(REPLACED_PREFIX.length()).trim()));
			}
		}
		return entries;
	}

	private static List<String> manifestLines() throws IOException {
		assertTrue(Files.exists(MANIFEST), "placeholder manifest missing " + MANIFEST);
		List<String> lines = new ArrayList<>();
		for (String raw : Files.readAllLines(MANIFEST)) {
			String line = raw.trim();
			if (!line.isEmpty()) {
				lines.add(line);
			}
		}
		return lines;
	}

	private static ManifestEntry parse(String line) {
		int split = line.lastIndexOf(' ');
		assertTrue(split > 0, "malformed manifest line '" + line + "' (expected '<path> <sha256>')");
		return new ManifestEntry(Path.of(line.substring(0, split)), line.substring(split + 1));
	}

	private static String sha256(Path file) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		try (InputStream in = Files.newInputStream(file)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
		}
		StringBuilder hex = new StringBuilder();
		for (byte b : digest.digest()) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}
}
