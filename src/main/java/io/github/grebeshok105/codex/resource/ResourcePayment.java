package io.github.grebeshok105.codex.resource;

/**
 * Result of paying a cost from the dual Energy/Mana pool: the preferred resource pays first and the
 * other one covers any shortfall. Pure, so the fallback rule is unit-tested.
 */
public record ResourcePayment(boolean success, float energy, float mana, float energySpent, float manaSpent) {
	public static ResourcePayment pay(float energy, float mana, ResourceKind preferred, float amount) {
		if (amount <= 0f) {
			return new ResourcePayment(true, energy, mana, 0f, 0f);
		}
		if (preferred == ResourceKind.ENERGY) {
			if (energy >= amount) {
				return new ResourcePayment(true, energy - amount, mana, amount, 0f);
			}
			float deficit = amount - energy;
			if (mana >= deficit) {
				return new ResourcePayment(true, 0f, mana - deficit, energy, deficit);
			}
		} else {
			if (mana >= amount) {
				return new ResourcePayment(true, energy, mana - amount, 0f, amount);
			}
			float deficit = amount - mana;
			if (energy >= deficit) {
				return new ResourcePayment(true, energy - deficit, 0f, deficit, mana);
			}
		}
		return new ResourcePayment(false, energy, mana, 0f, 0f);
	}
}
