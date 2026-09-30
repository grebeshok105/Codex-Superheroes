package io.github.grebeshok105.codex.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Resource gate for the Quasar emitter/module JSONs and the VFX tuning params:
 * every {@code .json} under {@code assets/superheroes/quasar/} and
 * {@code assets/superheroes/vfx/} must parse to a JSON object, and every vfx
 * param file must feed through {@link VfxParams#parse} without throwing —
 * the same call {@code VfxParamsLoader} makes on resource reload.
 */
class VfxQuasarResourcesParseTest {
	private static final Path ASSETS = Path.of("").toAbsolutePath()
			.resolve("src/main/resources/assets/superheroes");

	@Test
	void quasarJsonsParse() throws IOException {
		assertAllJsonObjects(ASSETS.resolve("quasar"));
	}

	@Test
	void vfxJsonsParseAndLoadAsParams() throws IOException {
		for (Path file : assertAllJsonObjects(ASSETS.resolve("vfx"))) {
			VfxParams.parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
		}
	}

	private static List<Path> assertAllJsonObjects(Path dir) throws IOException {
		assertTrue(Files.isDirectory(dir), dir + " must exist");
		List<Path> files;
		try (Stream<Path> walk = Files.walk(dir)) {
			files = walk.filter(path -> path.toString().endsWith(".json")).sorted().toList();
		}
		assertFalse(files.isEmpty(), dir + " has no .json files; this check would pass vacuously");
		for (Path file : files) {
			JsonElement element;
			try {
				element = JsonParser.parseString(Files.readString(file));
			} catch (Exception e) {
				fail(file + " failed to parse: " + e.getMessage());
				continue;
			}
			assertTrue(element.isJsonObject(), file + " must be a JSON object");
		}
		return files;
	}
}
