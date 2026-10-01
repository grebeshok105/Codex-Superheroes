package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Task-2 pins for {@link RegulusCastState} — the authored-event cast machine behind the
 * Regulus rework: the effect exists only from the authored fire tick (no charge, no
 * cooldown before it), a pre-fire cancel is free, one cast session per player, and the
 * session dies with its owner (leave/death/hero-clear via {@code OwnedSessionMap}).
 */
public final class RegulusCastStateGameTests implements FabricGameTest {

	private static final ResourceLocation CAST_A = ModId.of("regulus_cast_test_a");
	private static final ResourceLocation CAST_B = ModId.of("regulus_cast_test_b");

	private static RegulusCastState.Spec spec(ResourceLocation id, int fireTick, int castUntilTick,
			float activateCost, int cooldownTicks, boolean damageInterrupts,
			Runnable onFire, Runnable onInterrupt) {
		return new RegulusCastState.Spec(id, fireTick, castUntilTick, activateCost,
				cooldownTicks, damageInterrupts, onFire, onInterrupt);
	}

	/**
	 * The machine charges nothing before the authored event: a 900-cost cast leaves the
	 * player's energy untouched through the windup and spends it only on the fire tick,
	 * together with the cooldown.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void castDoesNotChargeBeforeAuthoredEvent(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		AtomicBoolean fired = new AtomicBoolean();
		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 1000f) < 0.001f,
				"precondition: fresh transform at full energy (1000)");

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 10, 40, 900f, 100, false, () -> fired.set(true), () -> {
				})), "startCast accepted");
		helper.assertTrue(RegulusCastState.isCasting(player, CAST_A), "cast session tracked");

		helper.runAfterDelay(8, () -> {
			helper.assertFalse(fired.get(), "nothing fired before the authored event");
			helper.assertFalse(RegulusCastState.hasFired(player, CAST_A), "hasFired false pre-fire");
			helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 1000f) < 0.001f,
					"no charge before the authored event, energy " + HeroDataStore.get(player).energy());
			helper.assertFalse(AbilityCooldowns.isOnCooldown(player, CAST_A),
					"no cooldown before the authored event");
		});
		helper.runAfterDelay(14, () -> {
			helper.assertTrue(fired.get(), "onFire ran on the fire tick");
			helper.assertTrue(RegulusCastState.hasFired(player, CAST_A), "hasFired after fire");
			helper.assertTrue(HeroDataStore.get(player).energy() < 500f,
					"the 900 charge landed on the fire tick, energy " + HeroDataStore.get(player).energy());
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, CAST_A),
					"cooldown armed on the fire tick");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * The fire event lands exactly on {@code startTick + fireTick} — recorded against the
	 * level game clock — and the session stays tracked until {@code castUntilTick} ends it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void castFiresExactlyAtFireTick(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		AtomicBoolean fired = new AtomicBoolean();
		AtomicLong firedAt = new AtomicLong(-1);
		long start = helper.getLevel().getGameTime();

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 6, 30, 0f, 100, false,
				() -> {
					fired.set(true);
					firedAt.set(helper.getLevel().getGameTime());
				}, () -> {
				})), "startCast accepted");
		helper.assertTrue(RegulusCastState.castTicksLeft(player, CAST_A) > 0,
				"cast session reports ticks left");

		helper.runAfterDelay(4, () -> {
			helper.assertFalse(fired.get(), "the fire tick has not arrived yet");
		});
		helper.runAfterDelay(9, () -> {
			helper.assertTrue(fired.get(), "onFire ran");
			helper.assertValueEqual(firedAt.get(), start + 6, "fired exactly at fireTick");
			helper.assertTrue(RegulusCastState.isCasting(player, CAST_A),
					"the session outlives the fire tick until castUntil");
		});
		helper.runAfterDelay(34, () -> {
			helper.assertFalse(RegulusCastState.isCasting(player, CAST_A),
					"the session is dropped once castUntil passes");
			helper.assertValueEqual(RegulusCastState.castTicksLeft(player, CAST_A), 0,
					"no ticks left after the session ended");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * A pre-fire cancel is a free abort: no charge, no cooldown, the interrupt hook runs
	 * (cast-side cleanup), and the slot is open for a new cast immediately.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void cancelledCastHasNoCooldown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		AtomicBoolean fired = new AtomicBoolean();
		AtomicBoolean interrupted = new AtomicBoolean();

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 10, 40, 900f, 100, false,
				() -> fired.set(true), () -> interrupted.set(true))), "startCast accepted");
		RegulusCastState.cancel(player);
		helper.assertTrue(interrupted.get(), "cancel runs the interrupt cleanup before fire");
		helper.assertFalse(RegulusCastState.isCasting(player, CAST_A), "cancel drops the session");
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, CAST_A), "a cancelled cast arms no cooldown");
		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_B, 20, 40, 0f, 100, false, () -> {
				}, () -> {
				})), "the slot is free for a new cast right away");

		helper.runAfterDelay(14, () -> {
			helper.assertFalse(fired.get(), "a cancelled cast never fires");
			helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 1000f) < 0.001f,
					"a cancelled cast never charges, energy " + HeroDataStore.get(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/** One cast session per player: a second {@code startCast} is refused while any cast runs. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void doubleCastRejected(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 5, 25, 0f, 0, false, () -> {
				}, () -> {
				})), "first cast accepted");
		helper.assertFalse(RegulusCastState.startCast(player, spec(
				CAST_B, 5, 25, 0f, 0, false, () -> {
				}, () -> {
				})), "a second cast is refused while one runs");
		helper.assertTrue(RegulusCastState.isCasting(player, CAST_A), "the first cast still owns the slot");
		helper.assertFalse(RegulusCastState.isCasting(player, CAST_B), "the refused cast never started");

		helper.runAfterDelay(28, () -> {
			helper.assertFalse(RegulusCastState.isCasting(player, CAST_A),
					"the session ends after castUntil");
			helper.assertTrue(RegulusCastState.startCast(player, spec(
					CAST_B, 5, 25, 0f, 0, false, () -> {
					}, () -> {
					})), "a fresh cast is accepted once the session expired");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * Death drops the session through {@code OwnedSessionMap} ClearOn.DEATH — the effect
	 * never happens: no fire, no charge, no cooldown. A fresh Regulus refuses the first
	 * lethal hit with its totem ward, which is not a death and must not drop the cast;
	 * the second hit kills and clears it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void castDiesWithPlayer(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		AtomicBoolean fired = new AtomicBoolean();

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 5, 40, 900f, 100, false, () -> fired.set(true), () -> {
				})), "startCast accepted");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 1000f) < 0.001f,
				"precondition: full energy, nothing charged on startCast");
		player.kill();
		helper.assertTrue(player.isAlive(), "the totem ward refuses the first death");
		helper.assertTrue(RegulusCastState.isCasting(player, CAST_A),
				"a refused death keeps the cast session");
		player.invulnerableTime = 0;
		player.kill();
		helper.assertFalse(player.isAlive(), "the caster is dead");
		helper.assertFalse(RegulusCastState.isCasting(player, CAST_A), "death drops the session");

		helper.runAfterDelay(10, () -> {
			helper.assertFalse(fired.get(), "a dead caster never fires");
			helper.assertFalse(AbilityCooldowns.isOnCooldown(player, CAST_A),
					"a dead caster arms no cooldown");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * {@code damageInterrupts} breaks the cast on incoming damage — the damage itself
	 * still lands (interrupt, not block), the interrupt hook runs, and nothing is charged
	 * or cooled down.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void damageInterruptsCastBeforeFire(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		AtomicBoolean fired = new AtomicBoolean();
		AtomicBoolean interrupted = new AtomicBoolean();

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 10, 40, 900f, 100, true,
				() -> fired.set(true), () -> interrupted.set(true))), "startCast accepted");
		helper.assertTrue(player.hurt(helper.getLevel().damageSources().generic(), 1f),
				"the interrupting damage still lands");
		helper.assertTrue(interrupted.get(), "damage interrupt runs the interrupt hook");
		helper.assertFalse(RegulusCastState.isCasting(player, CAST_A), "damage cancels the cast");

		helper.runAfterDelay(14, () -> {
			helper.assertFalse(fired.get(), "an interrupted cast never fires");
			helper.assertFalse(AbilityCooldowns.isOnCooldown(player, CAST_A),
					"an interrupted cast arms no cooldown");
			helper.assertTrue(HeroDataStore.get(player).energy() >= 990f,
					"an interrupted cast is never charged, energy " + HeroDataStore.get(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/** {@code damageInterrupts=false}: the hit lands but the cast keeps running to its fire tick. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void castSurvivesDamageWhenInterruptsFalse(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestPlayers.clearSpawnInvulnerability(player);
		AtomicBoolean fired = new AtomicBoolean();
		AtomicBoolean interrupted = new AtomicBoolean();

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 5, 30, 0f, 0, false,
				() -> fired.set(true), () -> interrupted.set(true))), "startCast accepted");
		helper.assertTrue(player.hurt(helper.getLevel().damageSources().generic(), 1f),
				"the damage lands");
		helper.assertTrue(RegulusCastState.isCasting(player, CAST_A),
				"a non-interrupting cast survives damage");
		helper.assertFalse(interrupted.get(), "no interrupt hook without damageInterrupts");

		helper.runAfterDelay(8, () -> {
			helper.assertTrue(fired.get(), "the surviving cast still fires on its tick");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * A charge that fails on the fire tick cancels the cast for free: the interrupt hook
	 * runs, no cooldown arms, and the session is gone.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void chargeFailureCancelsWithoutCooldown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		HeroDataStore.update(player, d -> d.withEnergy(50f));
		AtomicBoolean fired = new AtomicBoolean();
		AtomicBoolean interrupted = new AtomicBoolean();

		helper.assertTrue(RegulusCastState.startCast(player, spec(
				CAST_A, 5, 40, 900f, 100, false,
				() -> fired.set(true), () -> interrupted.set(true))), "startCast accepted");

		helper.runAfterDelay(9, () -> {
			helper.assertFalse(fired.get(), "a failed charge never fires the effect");
			helper.assertTrue(interrupted.get(), "a failed charge runs the interrupt hook");
			helper.assertFalse(RegulusCastState.isCasting(player, CAST_A),
					"a failed charge drops the session");
			helper.assertFalse(AbilityCooldowns.isOnCooldown(player, CAST_A),
					"a failed charge arms no cooldown");
			helper.assertTrue(HeroDataStore.get(player).energy() < 150f,
					"the failed charge spent nothing, energy " + HeroDataStore.get(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}
}
