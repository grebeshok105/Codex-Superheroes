package com.example.superheroes.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactChargeRulesTest {
	@Test
	void lessThanTwoSecondsIsTierOne() {
		assertEquals(ImpactTier.TIER_1, ImpactChargeRules.tierFor(0));
		assertEquals(ImpactTier.TIER_1, ImpactChargeRules.tierFor(39));
	}

	@Test
	void twoSecondsUnlocksTierTwoAndFiveSecondsUnlocksTierThree() {
		assertEquals(ImpactTier.TIER_2, ImpactChargeRules.tierFor(40));
		assertEquals(ImpactTier.TIER_2, ImpactChargeRules.tierFor(99));
		assertEquals(ImpactTier.TIER_3, ImpactChargeRules.tierFor(100));
	}

	@Test
	void chargePowerCapsAfterSevenSeconds() {
		assertEquals(140, ImpactChargeRules.cappedTicks(999));
		assertTrue(ImpactChargeRules.chargeScale(999) <= 1.0f);
		assertEquals(ImpactChargeRules.chargeScale(140), ImpactChargeRules.chargeScale(999));
	}
}
