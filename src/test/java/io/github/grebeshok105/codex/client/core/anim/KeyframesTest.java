package io.github.grebeshok105.codex.client.core.anim;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link Keyframes} sampling: linear, clamping and Catmull-Rom pass-through. */
class KeyframesTest {

	@Test
	void linearMidpoint() {
		Keyframes keys = new Keyframes(List.of(
				new Keyframes.Key(0f, new Vector3f(0, 0, 0)),
				new Keyframes.Key(1f, new Vector3f(90, 0, 0))));
		assertEquals(45f, keys.sample(0.5f).x, 1e-4);
	}

	@Test
	void clampsAfterEnd() {
		Keyframes keys = new Keyframes(List.of(
				new Keyframes.Key(0f, new Vector3f(0, 0, 0)),
				new Keyframes.Key(1f, new Vector3f(90, 10, -20))));
		Vector3f after = keys.sample(5f);
		assertEquals(90f, after.x, 1e-4);
		assertEquals(10f, after.y, 1e-4);
		assertEquals(-20f, after.z, 1e-4);
		assertEquals(0f, keys.sample(-1f).x, 1e-4);
	}

	@Test
	void catmullromPassesThroughKeys() {
		Keyframes keys = new Keyframes(List.of(
				new Keyframes.Key(0f, new Vector3f(0, 0, 0), Keyframes.Easing.LINEAR, Keyframes.Lerp.CATMULLROM),
				new Keyframes.Key(0.5f, new Vector3f(10, 0, 0), Keyframes.Easing.LINEAR, Keyframes.Lerp.CATMULLROM),
				new Keyframes.Key(1f, new Vector3f(0, 0, 0), Keyframes.Easing.LINEAR, Keyframes.Lerp.CATMULLROM)));
		assertEquals(0f, keys.sample(0f).x, 1e-4);
		assertEquals(10f, keys.sample(0.5f).x, 1e-4);
		assertEquals(0f, keys.sample(1f).x, 1e-4);
	}
}
