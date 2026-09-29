package io.github.grebeshok105.codex.client.core.anim;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An ordered list of vector keyframes sampled at a second offset. Each key carries the
 * easing applied to the segment starting at that key (Bedrock convention) and the
 * interpolation mode (linear or Catmull-Rom). Sampling clamps outside the key range.
 */
public final class Keyframes {
	public static final Keyframes EMPTY = new Keyframes(List.of());

	/** Robert Penner curves for the easing names the OMP clips ship. */
	public enum Easing {
		LINEAR {
			@Override float apply(float t) { return t; }
		},
		EASE_IN_CUBIC {
			@Override float apply(float t) { return t * t * t; }
		},
		EASE_OUT_CUBIC {
			@Override float apply(float t) { float u = 1f - t; return 1f - u * u * u; }
		},
		EASE_OUT_QUAD {
			@Override float apply(float t) { float u = 1f - t; return 1f - u * u; }
		},
		EASE_IN_OUT_SINE {
			@Override float apply(float t) { return (1f - (float) Math.cos(Math.PI * t)) * 0.5f; }
		},
		EASE_OUT_SINE {
			@Override float apply(float t) { return (float) Math.sin(t * Math.PI * 0.5f); }
		};

		abstract float apply(float t);

		/** Maps a Bedrock {@code easing} name; {@code null} when unknown. */
		public static Easing byName(String name) {
			return switch (name) {
				case "linear" -> LINEAR;
				case "easeInCubic" -> EASE_IN_CUBIC;
				case "easeOutCubic" -> EASE_OUT_CUBIC;
				case "easeOutQuad" -> EASE_OUT_QUAD;
				case "easeInOutSine" -> EASE_IN_OUT_SINE;
				case "easeOutSine" -> EASE_OUT_SINE;
				default -> null;
			};
		}
	}

	public enum Lerp {
		LINEAR, CATMULLROM;

		/** Maps a Bedrock {@code lerp_mode} name; {@code null} when unknown. */
		public static Lerp byName(String name) {
			return switch (name) {
				case "linear" -> LINEAR;
				case "catmullrom" -> CATMULLROM;
				default -> null;
			};
		}
	}

	/** {@code easing} and {@code lerp} apply to the segment starting at this key. */
	public record Key(float t, Vector3f v, Easing easing, Lerp lerp) {
		public Key(float t, Vector3f v) {
			this(t, v, Easing.LINEAR, Lerp.LINEAR);
		}
	}

	private final List<Key> keys;

	public Keyframes(List<Key> keys) {
		List<Key> sorted = new ArrayList<>(keys);
		sorted.sort(Comparator.comparingDouble(Key::t));
		this.keys = List.copyOf(sorted);
	}

	public boolean isEmpty() {
		return keys.isEmpty();
	}

	/** Last key time in seconds, or 0 when empty. */
	public float endSeconds() {
		return keys.isEmpty() ? 0f : keys.get(keys.size() - 1).t();
	}

	public Vector3f sample(float seconds) {
		if (keys.isEmpty()) {
			return new Vector3f();
		}
		Key first = keys.get(0);
		Key last = keys.get(keys.size() - 1);
		if (seconds <= first.t()) {
			return new Vector3f(first.v());
		}
		if (seconds >= last.t()) {
			return new Vector3f(last.v());
		}
		int i = 0;
		while (i + 1 < keys.size() && keys.get(i + 1).t() <= seconds) {
			i++;
		}
		Key a = keys.get(i);
		Key b = keys.get(i + 1);
		float span = b.t() - a.t();
		float t = span <= 0f ? 1f : (seconds - a.t()) / span;
		t = a.easing().apply(Math.max(0f, Math.min(1f, t)));
		if (a.lerp() == Lerp.CATMULLROM) {
			Vector3f p0 = i > 0 ? keys.get(i - 1).v() : a.v();
			Vector3f p3 = i + 2 < keys.size() ? keys.get(i + 2).v() : b.v();
			return catmullRom(p0, a.v(), b.v(), p3, t);
		}
		return new Vector3f(
				lerp(a.v().x, b.v().x, t),
				lerp(a.v().y, b.v().y, t),
				lerp(a.v().z, b.v().z, t));
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	/** Uniform Catmull-Rom through p1→p2; endpoints repeat the boundary key. */
	private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
		float t2 = t * t;
		float t3 = t2 * t;
		return new Vector3f(
				catmull(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
				catmull(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
				catmull(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
	}

	private static float catmull(float p0, float p1, float p2, float p3, float t, float t2, float t3) {
		return 0.5f * ((2f * p1)
				+ (-p0 + p2) * t
				+ (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
				+ (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
	}
}
