package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.client.core.emf.RenderedPoseCache;
import io.github.grebeshok105.codex.client.core.net.ScorchMarkStore;
import io.github.grebeshok105.codex.core.net.ScorchMark;
import io.github.grebeshok105.codex.client.hero.doomsday.state.ClientDoomsdayState;
import io.github.grebeshok105.codex.client.hero.homelander.state.ClientUraniumPressureState;
import io.github.grebeshok105.codex.client.hero.homelander.state.ClientUraniumThreatState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientNanoFormState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientNanoWeaponState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientReactorState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientRepulsorChargeState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientSuitVariantState;
import io.github.grebeshok105.codex.client.hero.kratos.state.ClientKratosRageState;
import io.github.grebeshok105.codex.client.hero.rem.state.ClientRemDemonismState;
import io.github.grebeshok105.codex.client.hero.sungjinwoo.state.ClientShadowArmyState;
import io.github.grebeshok105.codex.client.hero.reinhard.state.ClientReinhardCeremonyState;
import io.github.grebeshok105.codex.client.hero.reinhard.state.ClientReinhardDarknessState;
import io.github.grebeshok105.codex.client.hero.reinhard.state.ClientReinhardSwordGateState;
import io.github.grebeshok105.codex.client.hero.reinhard.state.ClientReinhardSwordKillState;
import io.github.grebeshok105.codex.client.hero.reinhard.state.ClientReinhardTimeSlowState;
import io.github.grebeshok105.codex.client.hero.pandora.hud.MirrorWarpFlashHud;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientHeartsState;
import io.github.grebeshok105.codex.client.hero.pandora.state.ClientPandoraDeathState;
import io.github.grebeshok105.codex.client.hero.pandora.state.ClientPandoraHouseState;
import net.minecraft.core.BlockPos;
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
			// ClientMadnessState is skipped: it is a read-through of the synced
			// regulus_madness attachment — dirtied only with a live player
			// (ProjectSanityTest covers its register(...) call).
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
			ClientHeartsState.update(List.of(1, 2), true, 5);
			ClientRemDemonismState.update(PLAYER, 30f, true, false);
			ClientShadowArmyState.update(PLAYER, true, 4, true);
			ClientSuitVariantState.update(PLAYER, 2);
			// ClientThanosState is skipped: its FULL_MASK pulls InfinityStoneType -> MC
			// registries, which need a bootstrapped game (ProjectSanityTest covers its
			// registration).
			ClientNanoFormState.update(PLAYER, 1);
			ClientMeleeChargeState.update(true, 12);
			ClientFlightState.update(42, true, io.github.grebeshok105.codex.mechanic.flight.FlightMode.IRON_MAN,
					io.github.grebeshok105.codex.mechanic.flight.FlightPhase.HOVER, 0f);
			MirrorWarpFlashHud.flashAndRun(() -> {
			});
			ClientAbilityCooldowns.update(ABILITY, 60);
			RenderedPoseCache.capture(42, new net.minecraft.client.model.PlayerModel<>(
					new net.minecraft.client.model.geom.ModelPart(List.of(), java.util.Map.ofEntries(
							java.util.Map.entry("head", part()), java.util.Map.entry("hat", part()),
							java.util.Map.entry("body", part()), java.util.Map.entry("right_arm", part()),
							java.util.Map.entry("left_arm", part()), java.util.Map.entry("right_leg", part()),
							java.util.Map.entry("left_leg", part()), java.util.Map.entry("ear", part()),
							java.util.Map.entry("cloak", part()), java.util.Map.entry("left_sleeve", part()),
							java.util.Map.entry("right_sleeve", part()), java.util.Map.entry("left_pants", part()),
							java.util.Map.entry("right_pants", part()), java.util.Map.entry("jacket", part()))),
					false));
			ScorchMarkStore.clear(); // reset the write cursor so slot 0 is deterministic
			ScorchMarkStore.receive(List.of(
					new ScorchMark(new BlockPos(4, 64, 4), (byte) 1, 0.5f, 0.5f, 0.5f, 0)), false);

			// Preconditions: every holder really is dirty.
			assertTrue(ClientReinhardTimeSlowState.active());
			assertTrue(ClientPandoraDeathState.active());
			assertTrue(ClientKratosRageState.active());
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
			assertTrue(ClientHeartsState.isLionHeartActive());
			assertTrue(!ClientHeartsState.heartEntityIds().isEmpty());
			assertTrue(ClientRemDemonismState.isActive(PLAYER));
			assertTrue(ClientShadowArmyState.hasShadows(PLAYER));
			assertEquals(2, ClientSuitVariantState.variantFor(PLAYER));
			assertEquals(1, ClientNanoFormState.formFor(PLAYER));
			assertTrue(ClientMeleeChargeState.charging());
			assertTrue(ClientFlightState.get(42) != null);
			assertTrue(MirrorWarpFlashHud.isCovering());
			assertTrue(ClientAbilityCooldowns.remainingTicks(ABILITY) > 0);
			assertTrue(RenderedPoseCache.headPose(42, new float[6]));
			assertTrue(ScorchMarkStore.live(0));

			ClientSessionState.resetAll();

			assertFalse(ClientReinhardTimeSlowState.active());
			assertFalse(ClientPandoraDeathState.active());
			assertFalse(ClientKratosRageState.active());
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
			assertFalse(ClientHeartsState.isLionHeartActive());
			assertTrue(ClientHeartsState.heartEntityIds().isEmpty());
			assertEquals(0, ClientHeartsState.overheatTicks());
			assertFalse(ClientRemDemonismState.isActive(PLAYER));
			assertFalse(ClientShadowArmyState.hasShadows(PLAYER));
			assertEquals(0, ClientSuitVariantState.variantFor(PLAYER));
			assertEquals(0, ClientNanoFormState.formFor(PLAYER));
			assertFalse(ClientMeleeChargeState.charging());
			assertNull(ClientFlightState.get(42));
			assertFalse(MirrorWarpFlashHud.isCovering());
			assertEquals(0, ClientAbilityCooldowns.remainingTicks(ABILITY));
			assertFalse(RenderedPoseCache.headPose(42, new float[6]));
			assertFalse(ScorchMarkStore.live(0));
		} finally {
			ClientAbilityCooldowns.clearClockForTesting();
		}
	}

	/**
	 * Level-swap resets (§7 stage 15): dimension change / respawn / rejoin must
	 * drop level-keyed caches (RenderedPoseCache's per-entity-id snapshots are
	 * only valid inside the ClientLevel they were captured in) WITHOUT touching
	 * session-scoped state — a mid-flight dimension change keeps the synced
	 * ClientFlightState entries.
	 */
	@Test
	void resetLevelDropsOnlyLevelKeyedCaches() {
		RenderedPoseCache.capture(42, new net.minecraft.client.model.PlayerModel<>(
				new net.minecraft.client.model.geom.ModelPart(List.of(), java.util.Map.ofEntries(
						java.util.Map.entry("head", part()), java.util.Map.entry("hat", part()),
						java.util.Map.entry("body", part()), java.util.Map.entry("right_arm", part()),
						java.util.Map.entry("left_arm", part()), java.util.Map.entry("right_leg", part()),
						java.util.Map.entry("left_leg", part()), java.util.Map.entry("ear", part()),
						java.util.Map.entry("cloak", part()), java.util.Map.entry("left_sleeve", part()),
						java.util.Map.entry("right_sleeve", part()), java.util.Map.entry("left_pants", part()),
						java.util.Map.entry("right_pants", part()), java.util.Map.entry("jacket", part()))),
				false));
		ClientFlightState.update(42, true, io.github.grebeshok105.codex.mechanic.flight.FlightMode.IRON_MAN,
				io.github.grebeshok105.codex.mechanic.flight.FlightPhase.HOVER, 0f);

		ClientSessionState.resetLevel();

		assertFalse(RenderedPoseCache.headPose(42, new float[6]));
		assertTrue(ClientFlightState.get(42) != null);
		ClientSessionState.resetAll();
	}

	private static net.minecraft.client.model.geom.ModelPart part() {
		net.minecraft.client.model.geom.ModelPart part =
				new net.minecraft.client.model.geom.ModelPart(List.of(), java.util.Map.of());
		part.setInitialPose(net.minecraft.client.model.geom.PartPose.ZERO);
		return part;
	}
}
