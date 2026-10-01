package io.github.grebeshok105.codex.client.core.vfx.pattern;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link TrailGeometry}: the ×3 Catmull-Rom subdivision contract and
 * the head→tail taper / ease-out fade curves the soft trail profile draws.
 */
class TrailGeometryTest {
	private static Vector3f[] scratch(int n) {
		Vector3f[] out = new Vector3f[n];
		for (int i = 0; i < n; i++) {
			out[i] = new Vector3f();
		}
		return out;
	}

	private static IntFunction<Vec3> of(Vec3... points) {
		return i -> points[i];
	}

	private static void assertVec(Vec3 expected, Vector3f actual, double eps) {
		assertEquals(expected.x, actual.x, eps);
		assertEquals(expected.y, actual.y, eps);
		assertEquals(expected.z, actual.z, eps);
	}

	@Test
	void subdivideEmitsThreeSamplesPerSegment() {
		Vector3f[] out = scratch(TrailGeometry.subdividedCapacity(4));
		int written = TrailGeometry.subdivide(
				of(new Vec3(4, 0, 0), new Vec3(3, 0, 0), new Vec3(2, 0, 0), new Vec3(1, 0, 0)),
				4, out);
		assertEquals((4 - 1) * TrailGeometry.SOFT_SUBDIVISIONS + 1, written);
	}

	@Test
	void subdividePreservesEndpoints() {
		Vec3 head = new Vec3(1, 2, 3);
		Vec3 tail = new Vec3(9, 8, 7);
		Vector3f[] out = scratch(TrailGeometry.subdividedCapacity(3));
		int written = TrailGeometry.subdivide(
				of(head, new Vec3(5, 5, 5), tail), 3, out);
		assertEquals(head, new Vec3(out[0]));
		assertEquals(tail, new Vec3(out[written - 1]));
	}

	@Test
	void subdividePassesThroughStoredPoints() {
		// Catmull-Rom interpolates: every stored point lands on the resampled spine.
		Vec3[] stored = {new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 1, 0),
				new Vec3(2, 1, 0)};
		Vector3f[] out = scratch(TrailGeometry.subdividedCapacity(stored.length));
		int written = TrailGeometry.subdivide(of(stored), stored.length, out);
		assertEquals(TrailGeometry.subdividedCapacity(stored.length), written);
		for (int i = 0; i < stored.length; i++) {
			assertEquals(stored[i], new Vec3(out[i * TrailGeometry.SOFT_SUBDIVISIONS]));
		}
	}

	@Test
	void subdivideInteriorOfCollinearPointsIsEvenlySpaced() {
		// Collinear interior with symmetric neighbours: Catmull-Rom reduces to
		// linear — the sub-segments split the stored segment into thirds exactly.
		// (Clamped end segments are not required to space evenly.)
		Vector3f[] out = scratch(TrailGeometry.subdividedCapacity(4));
		TrailGeometry.subdivide(of(new Vec3(0, 0, 0), new Vec3(1, 0, 0),
				new Vec3(2, 0, 0), new Vec3(3, 0, 0)), 4, out);
		assertVec(new Vec3(1, 0, 0), out[3], 1e-6);
		assertVec(new Vec3(4.0 / 3, 0, 0), out[4], 1e-6);
		assertVec(new Vec3(5.0 / 3, 0, 0), out[5], 1e-6);
		assertVec(new Vec3(2, 0, 0), out[6], 1e-6);
	}

	@Test
	void subdivideCurvedStaysInsideControlHull() {
		// A sharp bend: every emitted sample stays within the stored points'
		// bounding box — the resample cannot explode away from the spine.
		Vec3[] stored = {new Vec3(0, 0, 0), new Vec3(1, 1, 0), new Vec3(2, -1, 0),
				new Vec3(3, 0, 0)};
		Vector3f[] out = scratch(TrailGeometry.subdividedCapacity(stored.length));
		int written = TrailGeometry.subdivide(of(stored), stored.length, out);
		for (int i = 0; i < written; i++) {
			assertTrue(out[i].x >= -0.5f && out[i].x <= 3.5f, "x escaped hull at " + i);
			assertTrue(Math.abs(out[i].y) <= 1.5f, "y escaped hull at " + i);
		}
	}

	@Test
	void subdivideDegenerateInputs() {
		Vector3f[] out = scratch(TrailGeometry.subdividedCapacity(4));
		assertEquals(0, TrailGeometry.subdivide(of(), 0, out));
		Vec3 lone = new Vec3(7, 8, 9);
		assertEquals(1, TrailGeometry.subdivide(of(lone), 1, out));
		assertEquals(lone, new Vec3(out[0]));
	}

	@Test
	void widthScaleTapersHeadToTail() {
		assertEquals(1f, TrailGeometry.widthScale(0f), 1e-6);
		assertEquals(0f, TrailGeometry.widthScale(1f), 1e-6);
		float prev = Float.MAX_VALUE;
		for (float t = 0; t <= 1f; t += 0.05f) {
			float w = TrailGeometry.widthScale(t);
			assertTrue(w < prev, "taper not monotonic at " + t);
			prev = w;
		}
	}

	@Test
	void fadeAlphaEasesOut() {
		assertEquals(1f, TrailGeometry.fadeAlpha(0f), 1e-6);
		assertEquals(0f, TrailGeometry.fadeAlpha(1f), 1e-6);
		// Ease-out: the fade stays strong through the mid-trail (a gradual
		// contrail fade, not a steep drop behind the head).
		assertTrue(TrailGeometry.fadeAlpha(0.5f) > 0.5f);
		float prev = Float.MAX_VALUE;
		for (float t = 0; t <= 1f; t += 0.05f) {
			float a = TrailGeometry.fadeAlpha(t);
			assertTrue(a < prev, "fade not monotonic at " + t);
			prev = a;
		}
	}

	@Test
	void curvesClampOutOfRange() {
		assertEquals(TrailGeometry.widthScale(0f), TrailGeometry.widthScale(-0.3f), 1e-6);
		assertEquals(TrailGeometry.widthScale(1f), TrailGeometry.widthScale(1.4f), 1e-6);
		assertEquals(TrailGeometry.fadeAlpha(0f), TrailGeometry.fadeAlpha(-0.3f), 1e-6);
		assertEquals(TrailGeometry.fadeAlpha(1f), TrailGeometry.fadeAlpha(1.4f), 1e-6);
	}
}
