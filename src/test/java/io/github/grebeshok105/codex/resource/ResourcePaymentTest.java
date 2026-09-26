package io.github.grebeshok105.codex.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePaymentTest {
	@Test
	void preferredResourcePaysWhenItCovers() {
		ResourcePayment p = ResourcePayment.pay(50f, 30f, ResourceKind.ENERGY, 20f);
		assertTrue(p.success());
		assertEquals(30f, p.energy());
		assertEquals(30f, p.mana());
		assertEquals(20f, p.energySpent());
		assertEquals(0f, p.manaSpent());
	}

	@Test
	void energyShortfallFallsBackToMana() {
		ResourcePayment p = ResourcePayment.pay(5f, 30f, ResourceKind.ENERGY, 20f);
		assertTrue(p.success());
		assertEquals(0f, p.energy());
		assertEquals(15f, p.mana());
		assertEquals(5f, p.energySpent());
		assertEquals(15f, p.manaSpent());
	}

	@Test
	void manaBindingFallsBackToEnergy() {
		ResourcePayment p = ResourcePayment.pay(40f, 10f, ResourceKind.MANA, 25f);
		assertTrue(p.success());
		assertEquals(25f, p.energy());
		assertEquals(0f, p.mana());
	}

	@Test
	void failedPaymentLeavesPoolUntouched() {
		ResourcePayment p = ResourcePayment.pay(5f, 5f, ResourceKind.ENERGY, 20f);
		assertFalse(p.success());
		assertEquals(5f, p.energy());
		assertEquals(5f, p.mana());
		assertEquals(0f, p.energySpent() + p.manaSpent());
	}

	@Test
	void zeroCostAlwaysSucceeds() {
		assertTrue(ResourcePayment.pay(0f, 0f, ResourceKind.MANA, 0f).success());
	}
}
