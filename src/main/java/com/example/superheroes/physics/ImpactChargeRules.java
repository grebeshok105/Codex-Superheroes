package com.example.superheroes.physics;

public final class ImpactChargeRules {
	public static final int TIER_2_TICKS = 40;
	public static final int TIER_3_TICKS = 100;
	public static final int CAP_TICKS = 140;

	private ImpactChargeRules() {
	}

	public static ImpactTier tierFor(int heldTicks) {
		int ticks = Math.max(0, heldTicks);
		if (ticks >= TIER_3_TICKS) {
			return ImpactTier.TIER_3;
		}
		if (ticks >= TIER_2_TICKS) {
			return ImpactTier.TIER_2;
		}
		return ImpactTier.TIER_1;
	}

	public static int cappedTicks(int heldTicks) {
		return Math.max(0, Math.min(CAP_TICKS, heldTicks));
	}

	public static float chargeScale(int heldTicks) {
		return cappedTicks(heldTicks) / (float) CAP_TICKS;
	}
}
