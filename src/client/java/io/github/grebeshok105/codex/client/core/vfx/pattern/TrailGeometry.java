package io.github.grebeshok105.codex.client.core.vfx.pattern;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.IntFunction;

/**
 * Pure math behind {@link TrailPattern}'s soft profile: Catmull-Rom
 * subdivision of the stored trail points plus the head→tail width taper and
 * the ease-out alpha fade. No render or game state — every function is
 * deterministic on its inputs and exercised directly by JUnit.
 */
final class TrailGeometry {
	/** Interior samples emitted per stored segment in soft mode ("×3"). */
	static final int SOFT_SUBDIVISIONS = 3;

	private TrailGeometry() {
	}

	/** Subdivided-point capacity needed for {@code storedCapacity} stored points. */
	static int subdividedCapacity(int storedCapacity) {
		return Math.max(0, storedCapacity - 1) * SOFT_SUBDIVISIONS + 1;
	}

	/**
	 * Resamples the {@code size} newest-first points read through {@code get}
	 * into {@code out}: each adjacent pair emits {@code SOFT_SUBDIVISIONS}
	 * Catmull-Rom samples (ends clamped by repeating the boundary point) and
	 * the oldest point is appended once. Returns the written count —
	 * {@code (size - 1) * SOFT_SUBDIVISIONS + 1} for {@code size >= 2};
	 * {@code 0}/{@code 1} for empty/single input (the lone point passes through).
	 */
	static int subdivide(IntFunction<Vec3> get, int size, Vector3f[] out) {
		if (size <= 0) {
			return 0;
		}
		if (size == 1) {
			set(out[0], get.apply(0));
			return 1;
		}
		int w = 0;
		for (int i = 0; i + 1 < size; i++) {
			Vec3 p0 = get.apply(Math.max(0, i - 1));
			Vec3 p1 = get.apply(i);
			Vec3 p2 = get.apply(i + 1);
			Vec3 p3 = get.apply(Math.min(size - 1, i + 2));
			for (int s = 0; s < SOFT_SUBDIVISIONS; s++) {
				float t = (float) s / SOFT_SUBDIVISIONS;
				out[w++].set(
						(float) catmullRom(p0.x, p1.x, p2.x, p3.x, t),
						(float) catmullRom(p0.y, p1.y, p2.y, p3.y, t),
						(float) catmullRom(p0.z, p1.z, p2.z, p3.z, t));
			}
		}
		set(out[w++], get.apply(size - 1));
		return w;
	}

	/** Uniform Catmull-Rom between {@code p1} and {@code p2}; {@code t} in [0, 1]. */
	private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
		double t2 = t * t;
		double t3 = t2 * t;
		return 0.5 * (2 * p1 + (p2 - p0) * t
				+ (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
				+ (3 * p1 - p0 - 3 * p2 + p3) * t3);
	}

	/**
	 * Width multiplier for age fraction {@code t} (0 = newest, 1 = oldest):
	 * an eased taper that keeps the head broad and pinches off toward the tail.
	 */
	static float widthScale(float t) {
		float c = clamp01(t);
		return 1f - c * c;
	}

	/**
	 * Alpha multiplier for age fraction {@code t}: cubic ease-out on the
	 * remaining fade — bright near the head, exits quickly at the tail.
	 */
	static float fadeAlpha(float t) {
		float c = clamp01(t);
		return 1f - c * c * c;
	}

	private static float clamp01(float t) {
		return t < 0f ? 0f : (t > 1f ? 1f : t);
	}

	private static void set(Vector3f out, Vec3 v) {
		out.set((float) v.x, (float) v.y, (float) v.z);
	}
}
