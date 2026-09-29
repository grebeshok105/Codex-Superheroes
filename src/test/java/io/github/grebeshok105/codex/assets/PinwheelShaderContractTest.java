package io.github.grebeshok105.codex.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard for the Veil post-effect shaders: the {@code veil:space_helper}
 * convenience macros ({@code *SpacePosition}/{@code *SpaceDirection}) must not
 * be used in our programs. Under Veil 4.1.2's dynamic-shader recompile path
 * (the one driven by "shader active buffers") those macros expand with the
 * formal parameter left literal — the submitted fragment source reaches the GL
 * driver as {@code worldToScreenSpace(vec4(pos, 1.0))} with {@code pos}
 * undeclared, so the program fails to compile and every
 * {@code backend.distortion(...)} call silently no-ops (observed on the
 * dev client: {@code 0:136(42): error: 'pos' undeclared} followed by
 * {@code Failed to update shader active buffers: superheroes:vfx/distortion}).
 * Calling the underlying functions directly (e.g.
 * {@code worldToScreenSpace(vec4(uCenter, 1.0))}) avoids the macro path
 * entirely.
 */
class PinwheelShaderContractTest {
	private static final Path SHADERS =
			Path.of("").toAbsolutePath().resolve("src/client/resources/assets/superheroes/pinwheel/shaders");

	@Test
	void vfxShadersDoNotUseSpaceHelperPositionOrDirectionMacros() throws IOException {
		List<String> offenders;
		try (Stream<Path> paths = Files.walk(SHADERS)) {
			offenders = paths.filter(p -> p.toString().endsWith(".fsh") || p.toString().endsWith(".vsh"))
					.filter(PinwheelShaderContractTest::usesSpaceHelperMacro)
					.map(Path::toString)
					.toList();
		}
		assertTrue(offenders.isEmpty(),
				"shaders must call worldToScreenSpace/etc. directly, not the *Position/*Direction macros: "
						+ offenders);
	}

	private static boolean usesSpaceHelperMacro(Path shader) {
		String source;
		try {
			source = Files.readString(shader);
		} catch (IOException e) {
			throw new IllegalStateException("cannot read " + shader, e);
		}
		return source.contains("SpacePosition(") || source.contains("SpaceDirection(");
	}
}
