package com.example.superheroes.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactChargeRulesTest {
	@Test
	void lessThanOneAndAHalfSecondsIsTierOne() {
		assertEquals(ImpactTier.TIER_1, ImpactChargeRules.tierFor(0));
		assertEquals(ImpactTier.TIER_1, ImpactChargeRules.tierFor(29));
	}

	@Test
	void oneAndAHalfSecondsUnlocksTierTwoAndThreeSecondsUnlocksTierThree() {
		assertEquals(ImpactTier.TIER_2, ImpactChargeRules.tierFor(30));
		assertEquals(ImpactTier.TIER_2, ImpactChargeRules.tierFor(59));
		assertEquals(ImpactTier.TIER_3, ImpactChargeRules.tierFor(60));
	}

	@Test
	void chargePowerCapsAfterFourSeconds() {
		assertEquals(80, ImpactChargeRules.cappedTicks(999));
		assertTrue(ImpactChargeRules.chargeScale(999) <= 1.0f);
		assertEquals(ImpactChargeRules.chargeScale(80), ImpactChargeRules.chargeScale(999));
	}
}
