package io.github.grebeshok105.codex.client.core.anim;

import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link PlayerAnimator} layered blending: ACTION overrides BASE per bone by its fade
 * weight, one-shots release back to BASE, loops wrap. Time comes from an injected
 * {@link PlayerAnimator.TickClock} so no client tick loop runs here.
 */
class PlayerAnimatorTest {
	private static final int ENTITY = 42;
	private static final ResourceLocation BASE_ID =
			ResourceLocation.fromNamespaceAndPath("superheroes", "test/base");
	private static final ResourceLocation ACTION_ID =
			ResourceLocation.fromNamespaceAndPath("superheroes", "test/action");
	private static final ResourceLocation OTHER_ID =
			ResourceLocation.fromNamespaceAndPath("superheroes", "test/other");

	private final float[] now = {0f};

	@BeforeEach
	void freshAnimator() {
		PlayerAnimator.reset();
		PlayerAnimator.useClock(() -> now[0]);
	}

	@Test
	void actionFadesInOverBase() {
		playClip(BASE_ID, AnimationClip.Loop.WRAP, 10f, "right_arm", new Vector3f(90, 0, 0), 0);
		playClip(ACTION_ID, AnimationClip.Loop.HOLD, 10f, "right_arm", new Vector3f(-90, 0, 0), 4);

		now[0] = 1f;
		assertEquals(45f, PlayerAnimator.sample(ENTITY, 0).rotationDeg().get("right_arm").x, 1e-3,
				"quarter faded: lerp(90, -90, 0.25)");
		now[0] = 2f;
		assertEquals(0f, PlayerAnimator.sample(ENTITY, 0).rotationDeg().get("right_arm").x, 1e-3,
				"mid-fade: lerp(90, -90, 0.5)");
		now[0] = 4f;
		assertEquals(-90f, PlayerAnimator.sample(ENTITY, 0).rotationDeg().get("right_arm").x, 1e-3,
				"fully faded: ACTION wins");
	}

	@Test
	void oneShotEndsAndReleasesToBase() {
		playClip(BASE_ID, AnimationClip.Loop.WRAP, 10f, "right_arm", new Vector3f(90, 0, 0), 0);
		playClip(ACTION_ID, AnimationClip.Loop.OFF, 0.2f, "right_arm", new Vector3f(-90, 0, 0), 0);

		now[0] = 2f; // mid-clip (0.1 s of 0.2 s)
		assertEquals(-90f, PlayerAnimator.sample(ENTITY, 0).rotationDeg().get("right_arm").x, 1e-3,
				"one-shot drives the bone while live");

		now[0] = 5f; // past 0.2 s, no fade-out tail
		PoseSample released = PlayerAnimator.sample(ENTITY, 0);
		assertEquals(90f, released.rotationDeg().get("right_arm").x, 1e-3,
				"ended one-shot releases the bone to BASE");
	}

	@Test
	void stopRemovesOnlyTheNamedClip() {
		// A flight-style BASE loop plus a foreign clip on the same lane:
		// stopping the loop must leave the other clip playing.
		playClip(BASE_ID, AnimationClip.Loop.WRAP, 10f, "right_arm", new Vector3f(90, 0, 0), 0);
		playClip(OTHER_ID, AnimationClip.Loop.WRAP, 10f, "left_arm", new Vector3f(-60, 0, 0), 0);

		PlayerAnimator.stop(ENTITY, PlayerAnimator.Layer.BASE, 0, BASE_ID);

		PoseSample sample = PlayerAnimator.sample(ENTITY, 0);
		assertEquals(-60f, sample.rotationDeg().get("left_arm").x, 1e-3,
				"unrelated clip on the lane keeps playing");
		assertNull(sample.rotationDeg().get("right_arm"),
				"only the named clip was stopped");
	}

	@Test
	void stopWithFadeLeavesSiblingClipsUnaffected() {
		playClip(BASE_ID, AnimationClip.Loop.WRAP, 10f, "right_arm", new Vector3f(90, 0, 0), 0);
		playClip(OTHER_ID, AnimationClip.Loop.WRAP, 10f, "left_arm", new Vector3f(-90, 0, 0), 0);

		PlayerAnimator.stop(ENTITY, PlayerAnimator.Layer.BASE, 4, OTHER_ID);
		now[0] = 2f;
		PoseSample sample = PlayerAnimator.sample(ENTITY, 0);
		assertEquals(-45f, sample.rotationDeg().get("left_arm").x, 1e-3,
				"stopped clip fades out: -90 at half weight");
		assertEquals(90f, sample.rotationDeg().get("right_arm").x, 1e-3,
				"sibling clip unaffected by the fade-out");
	}

	@Test
	void loopWraps() {
		Keyframes keys = new Keyframes(List.of(
				new Keyframes.Key(0f, new Vector3f(0, 0, 0)),
				new Keyframes.Key(0.1f, new Vector3f(90, 0, 0))));
		AnimationClip clip = new AnimationClip(BASE_ID, 0.1f, AnimationClip.Loop.WRAP,
				Map.of("right_arm", new BoneTrack(keys, Keyframes.EMPTY)), Map.of());
		PlayerAnimator.playClip(ENTITY, clip, PlayerAnimator.Layer.BASE, 0);

		now[0] = 5f; // 0.25 s = two full loops + 0.05 s
		assertEquals(45f, PlayerAnimator.sample(ENTITY, 0).rotationDeg().get("right_arm").x, 1e-3,
				"wrapped to 0.05 s on the 0→90 ramp");
	}

	private void playClip(ResourceLocation id, AnimationClip.Loop loop, float length,
			String bone, Vector3f rotation, int fadeTicks) {
		Keyframes keys = new Keyframes(List.of(new Keyframes.Key(0f, rotation)));
		AnimationClip clip = new AnimationClip(id, length, loop,
				Map.of(bone, new BoneTrack(keys, Keyframes.EMPTY)), Map.of());
		PlayerAnimator.playClip(ENTITY, clip,
				id.equals(ACTION_ID) ? PlayerAnimator.Layer.ACTION : PlayerAnimator.Layer.BASE, fadeTicks);
	}
}
