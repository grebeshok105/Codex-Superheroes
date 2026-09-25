package com.example.superheroes.client;

import com.example.superheroes.client.hud.MirrorWarpFlashHud;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Audit B15 regression: leaving a world must drop EVERY piece of client session state —
 * previously only ~9 of 26 holders were reset on disconnect (e.g. an active time-slow
 * permanently muted world sounds for every later world).
 *
 * <p>Each holder is dirtied through its public writer (which class-loads it and runs the
 * static {@code ClientSessionState.register(...)} block), then {@link ClientSessionState#resetAll()}
 * must restore the defaults. Holders whose writers need a live Minecraft instance
 * (hero data, mirror dimension, suit-up animations, the detection HUD queue) are covered
 * by the {@code ProjectSanityTest} registration check instead.
 */
class ClientSessionStateResetTest {
	private static final UUID PLAYER = UUID.randomUUID();
	private static final ResourceLocation ABILITY =
			ResourceLocation.fromNamespaceAndPath("superheroes", "test_ability");

	@Test
	void resetAllDropsEveryRegisteredState() {
		AtomicLong now = new AtomicLong(1_000_000L);
		ClientAbilityCooldowns.setClockForTesting(now::get);
		try {
			// Dirty one representative of every headless-safe holder.
			ClientReinhardTimeSlowState.update(true);
			ClientPandoraDeathState.start(1, 2, 0.0, 0.0, 0.0);
			ClientKratosRageState.update(50f, true);
			ClientMadnessState.update(true, true, 5_000L, 5_000L);
			ClientNanoWeaponState.cycle(1);
			ClientRepulsorChargeState.flash(); // lastFireMs dirty; charge stays 0
			ClientRepulsorChargeState.clientTick(true, true);
			ClientUraniumThreatState.update(true, 3);
			ClientUraniumPressureState.update(List.of(PLAYER));
			ClientDoomsdayState.update(5, 12);
			ClientPandoraHouseState.set(true);
			ClientReactorState.update(true, 7, 10, false);
			ClientReinhardCeremonyState.update(true, 0.5f);
			ClientReinhardSwordGateState.update(true, 0.5f);
			ClientReinhardSwordKillState.update(true);
			ClientReinhardDarknessState.activate(200);
			ClientThinkMarkState.update(PLAYER, true);
			ClientRemDemonismState.update(PLAYER, 30f, true, false);
			ClientShadowArmyState.update(PLAYER, true, 4, true);
			ClientSuitVariantState.update(PLAYER, 2);
			// ClientThanosState is skipped: its FULL_MASK pulls InfinityStoneType -> MC
			// registries, which need a bootstrapped game (ProjectSanityTest covers its
			// registration).
			ClientNanoFormState.update(PLAYER, 1);
			ClientMeleeChargeState.update(true, 12);
			ClientFlightState.update(42, true, com.example.superheroes.flight.FlightMode.IRON_MAN,
					com.example.superheroes.flight.FlightPhase.HOVER, 0f);
			MirrorWarpFlashHud.flashAndRun(() -> {
			});
			ClientAbilityCooldowns.update(ABILITY, 60);

			// Preconditions: every holder really is dirty.
			assertTrue(ClientReinhardTimeSlowState.active());
			assertTrue(ClientPandoraDeathState.active());
			assertTrue(ClientKratosRageState.active());
			assertTrue(ClientMadnessState.isMadness());
			assertTrue(ClientNanoWeaponState.selectedIndex() != 0);
			assertTrue(ClientRepulsorChargeState.charge() > 0f);
			assertTrue(ClientUraniumThreatState.isSelfThreatened());
			assertTrue(ClientUraniumPressureState.anyPressured());
			assertTrue(ClientDoomsdayState.tier() > 1);
			assertTrue(ClientPandoraHouseState.isOpen());
			assertTrue(ClientReactorState.active());
			assertTrue(ClientReinhardCeremonyState.active());
			assertTrue(ClientReinhardSwordGateState.ready());
			assertTrue(ClientReinhardSwordKillState.active());
			assertTrue(ClientReinhardDarknessState.active());
			assertTrue(ClientThinkMarkState.isActive(PLAYER));
			assertTrue(ClientRemDemonismState.isActive(PLAYER));
			assertTrue(ClientShadowArmyState.hasShadows(PLAYER));
			assertEquals(2, ClientSuitVariantState.variantFor(PLAYER));
			assertEquals(1, ClientNanoFormState.formFor(PLAYER));
			assertTrue(ClientMeleeChargeState.charging());
			assertTrue(ClientFlightState.get(42) != null);
			assertTrue(MirrorWarpFlashHud.isCovering());
			assertTrue(ClientAbilityCooldowns.remainingTicks(ABILITY) > 0);

			ClientSessionState.resetAll();

			assertFalse(ClientReinhardTimeSlowState.active());
			assertFalse(ClientPandoraDeathState.active());
			assertFalse(ClientKratosRageState.active());
			assertFalse(ClientMadnessState.isMadness());
			assertFalse(ClientMadnessState.isBonusLifeAvailable());
			assertEquals(0L, ClientMadnessState.readingUntilMs());
			assertEquals(0L, ClientMadnessState.manaLockUntilMs());
			assertEquals(0L, ClientMadnessState.madnessStartedAtMs());
			assertEquals(0, ClientNanoWeaponState.selectedIndex());
			assertEquals(0L, ClientNanoWeaponState.lastSwitchMs());
			assertEquals(0f, ClientRepulsorChargeState.charge());
			assertEquals(0f, ClientRepulsorChargeState.dischargeFlash());
			assertFalse(ClientUraniumThreatState.isSelfThreatened());
			assertEquals(0, ClientUraniumThreatState.sourceCount());
			assertFalse(ClientUraniumPressureState.anyPressured());
			assertEquals(1, ClientDoomsdayState.tier());
			assertEquals(0, ClientDoomsdayState.adaptations());
			assertFalse(ClientPandoraHouseState.isOpen());
			assertFalse(ClientReactorState.active());
			assertEquals(0, ClientReactorState.progress());
			assertTrue(ClientReactorState.hasStock());
			assertFalse(ClientReinhardCeremonyState.active());
			assertFalse(ClientReinhardSwordGateState.ready());
			assertFalse(ClientReinhardSwordKillState.active());
			assertFalse(ClientReinhardDarknessState.active());
			assertFalse(ClientThinkMarkState.isActive(PLAYER));
			assertFalse(ClientRemDemonismState.isActive(PLAYER));
			assertFalse(ClientShadowArmyState.hasShadows(PLAYER));
			assertEquals(0, ClientSuitVariantState.variantFor(PLAYER));
			assertEquals(0, ClientNanoFormState.formFor(PLAYER));
			assertFalse(ClientMeleeChargeState.charging());
			assertNull(ClientFlightState.get(42));
			assertFalse(MirrorWarpFlashHud.isCovering());
			assertEquals(0, ClientAbilityCooldowns.remainingTicks(ABILITY));
		} finally {
			ClientAbilityCooldowns.clearClockForTesting();
		}
	}
}
