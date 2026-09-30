package io.github.grebeshok105.codex.client.hero.homelander.emf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmfPlaybackStateTest {

	@Test
	void weightApproachesTargetWithHalfLife() {
		EmfPlaybackState state = new EmfPlaybackState();
		state.setHalfLifeTicks(4f);
		state.registerClip("hover", 192, true);
		state.setTargetWeight("hover", 1f);
		state.advance(4f);
		assertEquals(0.5f, state.weight("hover"), 1e-4);
		state.advance(4f);
		assertEquals(0.75f, state.weight("hover"), 1e-4);
	}

	@Test
	void timeAdvancesOnlyWhileWeightPositive() {
		EmfPlaybackState state = new EmfPlaybackState();
		state.setHalfLifeTicks(0.001f);
		state.registerClip("hover", 192, true);
		state.setTargetWeight("hover", 1f);
		state.advance(1f);
		float t1 = state.time("hover");
		assertTrue(t1 > 0, "time should advance once weight is on");
		state.setTargetWeight("hover", 0f);
		for (int i = 0; i < 30; i++) {
			state.advance(1f);
		}
		assertEquals(0f, state.weight("hover"), 1e-3);
		float tFrozen = state.time("hover");
		state.advance(5f);
		assertEquals(tFrozen, state.time("hover"), 1e-6,
				"loop time must not advance while hidden");
	}

	@Test
	void loopTimeWrapsAtFrameCount() {
		EmfPlaybackState state = new EmfPlaybackState();
		state.setHalfLifeTicks(0.001f);
		state.registerClip("hover", 192, true);
		state.setTargetWeight("hover", 1f);
		for (int i = 0; i < 70; i++) { // 70 ticks * 3 frames = 210 > 192
			state.advance(1f);
		}
		assertTrue(state.time("hover") < 192f, "loop time should wrap");
	}

	@Test
	void oneShotReportsFinished() {
		EmfPlaybackState state = new EmfPlaybackState();
		state.setHalfLifeTicks(0.001f);
		state.registerClip("takeoff", 48, false);
		state.setTargetWeight("takeoff", 1f);
		assertFalse(state.oneShotFinished("takeoff"));
		for (int i = 0; i < 17; i++) { // 17*3=51 >= 48
			state.advance(1f);
		}
		assertTrue(state.oneShotFinished("takeoff"));
	}
}
