package io.github.grebeshok105.codex.client.hero.homelander.emf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmfModelConversionTest {

	@Test
	void pivotFlipsYAboutModelHeight() {
		assertArrayEquals(new float[]{0f, 12f, 0f},
				EmfModelConversion.pivot(new float[]{0f, 12f, 0f}), 1e-6f);
		assertArrayEquals(new float[]{2f, 0f, -1f},
				EmfModelConversion.pivot(new float[]{2f, 24f, -1f}), 1e-6f);
	}

	@Test
	void relativePivotIsParentSpaceDelta() {
		assertArrayEquals(new float[]{1f, -2f, 3f},
				EmfModelConversion.relativePivot(
						new float[]{1f, 14f, 3f}, new float[]{0f, 12f, 0f}), 1e-6f);
	}

	@Test
	void cubeMinIsPivotRelativeWithYFlip() {
		// bb cube at origin(2,14,0) size(4,6,2) inside bone pivot(0,12,0):
		// vanilla min = (dx, -(cy + sy - py)... ) — spec'd helper.
		float[] min = EmfModelConversion.cubeMin(
				new float[]{2f, 14f, 0f}, new float[]{4f, 6f, 2f}, new float[]{0f, 12f, 0f});
		assertArrayEquals(new float[]{2f, 12f - (14f + 6f) + 0f, 0f}, min, 1e-6f);
	}

	@Test
	void faceNamesSwapUpDown() {
		assertEquals("down", EmfModelConversion.faceName("up"));
		assertEquals("up", EmfModelConversion.faceName("down"));
		assertEquals("north", EmfModelConversion.faceName("north"));
		assertEquals("south", EmfModelConversion.faceName("south"));
		assertEquals("east", EmfModelConversion.faceName("east"));
		assertEquals("west", EmfModelConversion.faceName("west"));
	}

	@Test
	void rotationsNegateAllAxesToRadians() {
		assertArrayEquals(new float[]{(float) Math.toRadians(-86), 0f,
						(float) Math.toRadians(-30)},
				EmfModelConversion.rotationRad(86f, 0f, 30f), 1e-5f);
	}

	@Test
	void positionDeltaNegatesYOnly() {
		assertArrayEquals(new float[]{1f, -2f, 3f},
				EmfModelConversion.positionDelta(1f, 2f, 3f), 1e-6f);
	}
}
