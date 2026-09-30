package io.github.grebeshok105.codex.client.core.camera;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ThirdPersonFraming}: per-tick smoothed third-person offset with
 * half-life 3 ticks (plan §7 stage 6). The smoothed value is a world-space
 * delta the camera adds to its vanilla base — an entity with no framing
 * target samples {@code null}, which the mixin turns into "position
 * untouched", so standing third-person stays byte-identical (weight-0
 * identity).
 */
class ThirdPersonFramingTest {

	private static final UUID PLAYER = UUID.randomUUID();
	private static final UUID OTHER = UUID.randomUUID();
	/** One tick of exponential approach at half-life 3 ticks. */
	private static final double STEP = 1.0 - Math.pow(0.5, 1.0 / 3.0);

	@AfterEach
	void tearDown() {
		ThirdPersonFraming.reset();
	}

	@Test
	void noFramingTargetKeepsVanillaIdentity() {
		// Weight 0 / no provider: nothing to add — the camera base stays
		// the vanilla eye position untouched.
		assertNull(ThirdPersonFraming.sample(PLAYER, 0.5f));
	}

	@Test
	void firstTargetSeedsImmediately() {
		// Engaging must not drag the offset in from (0,0,0): the first
		// target seeds the smoothed value so flight start does not jump.
		ThirdPersonFraming.advance(PLAYER, new Vec3(0.0, 0.4, 0.0));
		assertVecEquals(new Vec3(0.0, 0.4, 0.0), ThirdPersonFraming.sample(PLAYER, 0.25f));
		assertVecEquals(new Vec3(0.0, 0.4, 0.0), ThirdPersonFraming.sample(PLAYER, 0.75f));
	}

	@Test
	void smoothingFollowsHalfLifeThreeTicks() {
		ThirdPersonFraming.advance(PLAYER, Vec3.ZERO);
		Vec3 target = new Vec3(0.0, 1.0, 0.0);
		for (int i = 0; i < 3; i++) {
			ThirdPersonFraming.advance(PLAYER, target);
		}
		// Three ticks at half-life 3 → half the distance remains.
		Vec3 half = ThirdPersonFraming.sample(PLAYER, 1f);
		assertNotNull(half);
		assertEquals(0.5, half.y, 1e-6);
		for (int i = 0; i < 3; i++) {
			ThirdPersonFraming.advance(PLAYER, target);
		}
		Vec3 threeQuarter = ThirdPersonFraming.sample(PLAYER, 1f);
		assertNotNull(threeQuarter);
		assertEquals(0.75, threeQuarter.y, 1e-6);
	}

	@Test
	void partialTickInterpolatesBetweenTicks() {
		ThirdPersonFraming.advance(PLAYER, Vec3.ZERO);
		ThirdPersonFraming.advance(PLAYER, new Vec3(1.0, 0.0, 0.0));
		Vec3 prev = Vec3.ZERO;
		Vec3 curr = new Vec3(STEP, 0.0, 0.0);
		assertVecEquals(prev.lerp(curr, 0.5), ThirdPersonFraming.sample(PLAYER, 0.5f));
		assertVecEquals(prev.lerp(curr, 1.0), ThirdPersonFraming.sample(PLAYER, 1.0f));
	}

	@Test
	void nullTargetReturnsToIdentity() {
		// The provider going quiet mid-flight (weight decayed to 0) drops
		// the framing state entirely — the next sample is identity again.
		ThirdPersonFraming.advance(PLAYER, new Vec3(0.0, 0.5, 0.0));
		ThirdPersonFraming.advance(PLAYER, null);
		assertNull(ThirdPersonFraming.sample(PLAYER, 0.5f));
	}

	@Test
	void focusedEntitySwitchReseeds() {
		// A different focused entity starts from its own first target —
		// the previous entity's smoothed offset must not leak across.
		ThirdPersonFraming.advance(PLAYER, new Vec3(0.0, 0.5, 0.0));
		ThirdPersonFraming.advance(OTHER, new Vec3(0.0, 0.9, 0.0));
		assertNull(ThirdPersonFraming.sample(PLAYER, 0.5f));
		assertVecEquals(new Vec3(0.0, 0.9, 0.0), ThirdPersonFraming.sample(OTHER, 0.0f));
	}

	@Test
	void nullEntityClearsFraming() {
		ThirdPersonFraming.advance(PLAYER, new Vec3(0.0, 0.5, 0.0));
		ThirdPersonFraming.advance(null, new Vec3(0.0, 0.5, 0.0));
		assertNull(ThirdPersonFraming.sample(PLAYER, 0.5f));
	}

	@Test
	void resetClearsFraming() {
		ThirdPersonFraming.advance(PLAYER, new Vec3(0.0, 0.5, 0.0));
		ThirdPersonFraming.reset();
		assertNull(ThirdPersonFraming.sample(PLAYER, 0.5f));
	}

	private static void assertVecEquals(Vec3 expected, Vec3 actual) {
		assertNotNull(actual);
		assertEquals(expected.x, actual.x, 1e-6);
		assertEquals(expected.y, actual.y, 1e-6);
		assertEquals(expected.z, actual.z, 1e-6);
	}
}
