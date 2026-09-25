package com.example.superheroes.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Audit B15: cooldown deadlines must ride a monotonic clock (level game time), never
 * {@code LocalPlayer.tickCount} — that resets on respawn and made the HUD show hours.
 * The injectable clock stands in for {@code ClientLevel#getGameTime()}, which never
 * resets across deaths/respawns.
 */
class ClientAbilityCooldownsTest {
	private static final ResourceLocation ABILITY =
			ResourceLocation.fromNamespaceAndPath("superheroes", "test_ability");
	private final AtomicLong now = new AtomicLong();

	@AfterEach
	void restore() {
		ClientAbilityCooldowns.clear();
		ClientAbilityCooldowns.clearClockForTesting();
	}

	@Test
	void remainingTicksDecreasesAsGameTimeAdvances() {
		ClientAbilityCooldowns.setClockForTesting(now::get);
		now.set(10_000L);
		ClientAbilityCooldowns.update(ABILITY, 40);

		now.addAndGet(15);
		assertEquals(25, ClientAbilityCooldowns.remainingTicks(ABILITY));
	}

	@Test
	void remainingTicksClampsAtZeroAndPurgesExpired() {
		ClientAbilityCooldowns.setClockForTesting(now::get);
		now.set(1L);
		ClientAbilityCooldowns.update(ABILITY, 20);

		now.addAndGet(10_000L);
		assertEquals(0, ClientAbilityCooldowns.remainingTicks(ABILITY));
		assertEquals(0, ClientAbilityCooldowns.totalTicks(ABILITY));
	}

	@Test
	void deadlineSurvivesClockJumpFromRespawn() {
		// The bug: with LocalPlayer.tickCount the clock reset to 0 on respawn, so
		// deadline - now ballooned into hours. With a monotonic level clock a death
		// mid-cooldown just keeps ticking down.
		ClientAbilityCooldowns.setClockForTesting(now::get);
		now.set(500_000L);
		ClientAbilityCooldowns.update(ABILITY, 60);

		now.addAndGet(40); // died and respawned somewhere in here; level time kept ticking
		assertEquals(20, ClientAbilityCooldowns.remainingTicks(ABILITY));
	}

	@Test
	void nonPositiveRemainingClearsTheCooldown() {
		ClientAbilityCooldowns.setClockForTesting(now::get);
		ClientAbilityCooldowns.update(ABILITY, 60);
		ClientAbilityCooldowns.update(ABILITY, 0);
		assertEquals(0, ClientAbilityCooldowns.remainingTicks(ABILITY));
		assertEquals(0, ClientAbilityCooldowns.totalTicks(ABILITY));
	}

	@Test
	void separateAbilitiesTrackIndependentDeadlines() {
		ClientAbilityCooldowns.setClockForTesting(now::get);
		ResourceLocation other = ResourceLocation.fromNamespaceAndPath("superheroes", "other_ability");
		now.set(0L);
		ClientAbilityCooldowns.update(ABILITY, 10);
		ClientAbilityCooldowns.update(other, 80);

		now.addAndGet(30);
		assertEquals(0, ClientAbilityCooldowns.remainingTicks(ABILITY));
		assertEquals(50, ClientAbilityCooldowns.remainingTicks(other));
		assertEquals(80, ClientAbilityCooldowns.totalTicks(other));
	}

	@Test
	void clearDropsAllDeadlines() {
		ClientAbilityCooldowns.setClockForTesting(now::get);
		ClientAbilityCooldowns.update(ABILITY, 100);
		ClientAbilityCooldowns.clear();
		assertEquals(0, ClientAbilityCooldowns.remainingTicks(ABILITY));
		assertEquals(0, ClientAbilityCooldowns.totalTicks(ABILITY));
	}
}
