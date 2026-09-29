package io.github.grebeshok105.codex.assets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-scan lint for the Homelander Visual Core migration: every server-side
 * file whose presentation moved to {@code VfxFx} events/channels must be free
 * of direct vanilla particle and sound calls — leftovers are placeholder
 * visuals that double up with the event-driven effects.
 *
 * <p>Scope note: this only covers direct calls. {@code level.explode} visual
 * particles are handled by Task 10's {@code SilentParticles} swap and
 * entity-based visuals by its GameTest — this is a lint, not proof of zero
 * vanilla visuals.
 */
class HomelanderNoVanillaParticlesTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();

	/** Server files cleaned of vanilla presentation; later tasks append theirs. */
	private static final List<String> CLEANED = List.of(
			"src/main/java/io/github/grebeshok105/codex/hero/homelander/HomelanderHero.java",
			"src/main/java/io/github/grebeshok105/codex/hero/homelander/ability/EyeLasersAbility.java",
			"src/main/java/io/github/grebeshok105/codex/hero/homelander/item/MilkBottleItem.java",
			"src/main/java/io/github/grebeshok105/codex/hero/homelander/runtime/HomelanderMadnessAftermathController.java",
			"src/main/java/io/github/grebeshok105/codex/hero/homelander/runtime/HomelanderMadnessFlightController.java");

	private static final List<String> FORBIDDEN = List.of(
			"sendParticles(", "ParticleTypes.", "playSound(", "SoundEvents.");

	@Test
	void homelanderServerCodeSendsNoVanillaParticles() throws IOException {
		List<String> violations = new ArrayList<>();
		for (String relative : CLEANED) {
			Path file = ROOT.resolve(relative);
			assertTrue(Files.isRegularFile(file), "CLEANED file missing: " + file);
			String source = Files.readString(file);
			for (String token : FORBIDDEN) {
				int at = source.indexOf(token);
				if (at >= 0) {
					int line = source.substring(0, at).split("\n", -1).length;
					violations.add(relative + ":" + line + " contains '" + token + "'");
				}
			}
		}
		assertTrue(violations.isEmpty(),
				"vanilla presentation left in cleaned files: " + violations);
	}
}
