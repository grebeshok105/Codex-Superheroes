package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.hero.regulus.runtime.LionHeartController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusHearts;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * Task-4 pins for Lion's Heart — the reworked "void" absolute defense: a cast with the
 * authored 14-tick trigger, an {@code ALLOW_DAMAGE} gate that voids every external
 * source while the shield is up (internal true-cost types still land), hunger
 * exhaustion blocked, projectiles frozen in a 4-block radius and released individually,
 * and the overheat ramp past the hearts window with the hp<=4 forced-off + 600t
 * cooldown.
 */
public final class RegulusLionHeartGameTests implements FabricGameTest {

	private static final ResourceLocation LION_HEART = ModId.of("lion_heart");

	/**
	 * Past the authored fire tick the gate voids external damage: a generic hit is
	 * refused wholesale ({@code hurt} returns false, health untouched) while the
	 * shield is up.
	 */
	/**
	 * Joins a named player parked far above the structure pads: the shared batch can
	 * throw stray hits (cast interrupts) and second-owner freezes (lock contamination)
	 * at a test, so every lion-heart player works on its own empty column.
	 */
	private static ServerPlayer joinIsolated(GameTestHelper helper, String name) {
		ServerPlayer player = TestPlayers.join(helper, name);
		player.setNoGravity(true);
		player.teleportTo(player.getX(), player.getY() + 300, player.getZ());
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void lionHeartBlocksExternalDamageAfterTrigger(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-void");
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);

		AbilityRouter.activate(player, LION_HEART);
		helper.assertTrue(RegulusCastState.isCasting(player, LION_HEART), "the cast session opened");
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));

		helper.runAfterDelay(16, () -> {
			helper.assertTrue(LionHeartController.isBlocking(player),
					"the shield is up after the authored fire tick (14)");
			helper.assertFalse(player.hasEffect(MobEffects.POISON),
					"the shield-up cleanse strips harmful effects");
			helper.assertTrue(player.hasEffect(MobEffects.GLOWING),
					"neutral effects are not 'negative' — they survive the cleanse");
			helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
					"beneficial effects survive the cleanse");
			float hp = player.getHealth();
			helper.assertFalse(player.hurt(helper.getLevel().damageSources().generic(), 4f),
					"external damage is voided while blocking");
			helper.assertTrue(Math.abs(player.getHealth() - hp) < 0.001f,
					"health untouched by the voided hit");
			helper.assertFalse(player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
					"the rework grants no resistance effect — the gate replaces it");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * Internal true-cost types bypass the void: the heart backlash self-cost lands on a
	 * blocking player (it is the owner's own bookkeeping, not an attack).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void lionHeartAllowsInternalTrueCostDamage(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-internal");
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);

		AbilityRouter.activate(player, LION_HEART);

		helper.runAfterDelay(16, () -> {
			helper.assertTrue(LionHeartController.isBlocking(player), "precondition: blocking");
			float hp = player.getHealth();
			helper.assertTrue(player.hurt(RegulusDamageTypes.heartBacklash(player.serverLevel()), 2f),
					"internal backlash damage lands through the gate");
			helper.assertTrue(player.getHealth() < hp, "internal damage actually hurt the player");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * The shield starves the hunger pipeline too: {@code causeFoodExhaustion} is
	 * cancelled while blocking and works again after deactivate.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void lionHeartStopsExhaustion(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-exhaustion");
		TestHeroes.transform(player, RegulusHero.ID);

		AbilityRouter.activate(player, LION_HEART);

		helper.runAfterDelay(16, () -> {
			helper.assertTrue(LionHeartController.isBlocking(player), "precondition: blocking");
			player.getFoodData().setExhaustion(3.9f);
			player.causeFoodExhaustion(1.0f);
			helper.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel() - 3.9f) < 0.001f,
					"exhaustion never accumulates while blocking, exhaustion="
							+ player.getFoodData().getExhaustionLevel());

			AbilityRouter.deactivate(player, LION_HEART);
			player.getFoodData().setExhaustion(0f);
			player.causeFoodExhaustion(0.5f);
			helper.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel() - 0.5f) < 0.001f,
					"exhaustion accumulates again after deactivate");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * Incoming projectiles freeze mid-air inside the 4-block radius (zeroed velocity +
	 * NO_GRAVITY lock held by the owner) and drop once the shield goes down.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void projectilesFreezeAndDrop(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-freeze");
		TestHeroes.transform(player, RegulusHero.ID);

		AbilityRouter.activate(player, LION_HEART);

		helper.runAfterDelay(16, () -> {
			helper.assertTrue(LionHeartController.isBlocking(player), "precondition: blocking");
			Arrow arrow = EntityType.ARROW.create(helper.getLevel());
			arrow.moveTo(player.getX() + 3.0, player.getY() + 1.5, player.getZ(), 0f, 0f);
			arrow.setDeltaMovement(new Vec3(-0.4, 0.2, 0.0));
			helper.getLevel().addFreshEntity(arrow);

			TestPlayers.awaitVisible(helper, arrow, () -> helper.runAfterDelay(5, () -> {
				helper.assertTrue(arrow.getDeltaMovement().lengthSqr() < 0.0001,
						"the frozen arrow's velocity is zeroed every tick, v=" + arrow.getDeltaMovement());
				helper.assertTrue(arrow.isNoGravity(), "the freeze holds NO_GRAVITY");
				helper.assertTrue(TestPlayers.lockOwners(arrow, ControlLockKind.NO_GRAVITY)
								.contains(player.getUUID()),
						"the freeze lock is owned by the Regulus player");
				helper.assertTrue(arrow.distanceTo(player) < 4.5,
						"the arrow never reached the player, dist=" + arrow.distanceTo(player));

				AbilityRouter.deactivate(player, LION_HEART);
				helper.runAfterDelay(5, () -> {
					helper.assertTrue(TestPlayers.lockOwners(arrow, ControlLockKind.NO_GRAVITY).isEmpty(),
							"deactivate releases the freeze lock");
					helper.assertTrue(arrow.getDeltaMovement().y < 0,
							"the released arrow drops under gravity, v=" + arrow.getDeltaMovement());
					TestPlayers.leave(player);
					helper.succeed();
				});
			}));
		});
	}

	/**
	 * Locks release per-owner, never by {@code releaseOwnedBy}: a second owner's
	 * NO_GRAVITY ref on the same projectile survives the shield going down — the flag
	 * only restores when the last owner releases.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void projectileLocksReleaseIndividually(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-owners");
		ServerPlayer other = joinIsolated(helper, "other-owner");
		TestHeroes.transform(player, RegulusHero.ID);

		Arrow arrow = EntityType.ARROW.create(helper.getLevel());
		arrow.moveTo(player.getX() + 3.0, player.getY() + 1.5, player.getZ(), 0f, 0f);
		helper.getLevel().addFreshEntity(arrow);

		TestPlayers.awaitVisible(helper, arrow, () -> {
			EntityControlLock.acquire(arrow, ControlLockKind.NO_GRAVITY, other);
			helper.assertTrue(arrow.isNoGravity(), "precondition: the other owner's lock holds gravity");

			AbilityRouter.activate(player, LION_HEART);
			helper.runAfterDelay(20, () -> {
				Set<UUID> owners = TestPlayers.lockOwners(arrow, ControlLockKind.NO_GRAVITY);
				helper.assertTrue(owners.contains(player.getUUID()) && owners.contains(other.getUUID()),
						"both owners hold the frozen arrow, owners=" + owners);

				AbilityRouter.deactivate(player, LION_HEART);
				helper.runAfterDelay(3, () -> {
					Set<UUID> after = TestPlayers.lockOwners(arrow, ControlLockKind.NO_GRAVITY);
					helper.assertTrue(!after.contains(player.getUUID()) && after.contains(other.getUUID()),
							"only the Regulus ref released, owners=" + after);
					helper.assertTrue(arrow.isNoGravity(),
							"the flag stays locked while another owner holds a ref");
					EntityControlLock.release(arrow, ControlLockKind.NO_GRAVITY, other.getUUID());
					TestPlayers.leave(player);
					TestPlayers.leave(other);
					helper.succeed();
				});
			});
		});
	}

	/**
	 * The free window is {@code 60 + 40*hearts} ticks from the authored fire tick; past
	 * it the overheat ramp bites — health drops and the counter flows to the client
	 * through {@link RegulusHearts#syncedOverheatTicks}.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 130)
	public void windowThenOverheat(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-overheat");
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);

		AbilityRouter.activate(player, LION_HEART);

		// 0 hearts → window 60t from the fire tick (t≈14): first overheat hurt at over=10 → t≈84.
		helper.runAfterDelay(96, () -> {
			helper.assertTrue(LionHeartController.isBlocking(player),
					"overheat does not drop the shield on its own");
			helper.assertTrue(player.getHealth() < player.getMaxHealth(),
					"the overheat ramp burned the player, hp=" + player.getHealth());
			helper.assertTrue(RegulusHearts.syncedOverheatTicks(player) > 0,
					"the overheat counter left via the hearts sync payload, sent="
							+ RegulusHearts.syncedOverheatTicks(player));
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * hp <= 4 forces the shield off: deactivate + a hard 600-tick cooldown — the
	 * safety valve that keeps void-defense honest.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void forcedOffAtLowHp(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-forceoff");
		TestHeroes.transform(player, RegulusHero.ID);

		AbilityRouter.activate(player, LION_HEART);

		helper.runAfterDelay(16, () -> {
			helper.assertTrue(LionHeartController.isBlocking(player), "precondition: blocking");
			player.setHealth(4f);

			helper.runAfterDelay(3, () -> {
				helper.assertFalse(HeroDataStore.get(player).isActive(LION_HEART),
						"the toggle was forced off at hp<=4");
				helper.assertFalse(LionHeartController.isBlocking(player), "the shield dropped");
				helper.assertTrue(AbilityCooldowns.isOnCooldown(player, LION_HEART),
						"the forced-off armed the 600t cooldown");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * Damage during the windup interrupts the cast for free (the spec is
	 * {@code damageInterrupts=true}): the hit lands, the shield never comes up, no
	 * cooldown arms.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void castInterruptedBeforeTrigger(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-interrupt");
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);

		AbilityRouter.activate(player, LION_HEART);
		helper.assertTrue(RegulusCastState.isCasting(player, LION_HEART), "cast opened");
		helper.assertFalse(LionHeartController.isBlocking(player),
				"the shield is not up during the windup");

		helper.assertTrue(player.hurt(helper.getLevel().damageSources().generic(), 1f),
				"the interrupting hit lands — interrupt, not block");
		helper.assertFalse(RegulusCastState.isCasting(player, LION_HEART),
				"the hit cancelled the cast");

		helper.runAfterDelay(18, () -> {
			helper.assertFalse(LionHeartController.isBlocking(player),
					"an interrupted cast never raises the shield");
			helper.assertFalse(AbilityCooldowns.isOnCooldown(player, LION_HEART),
					"a pre-fire cancel is free — no cooldown");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * The 10-energy/tick drain starts at the fire tick (not at activation) and the
	 * deactivation shockwave pushes live targets out of the 4-block radius.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void lionHeartDrainsAndPushesOnDeactivate(GameTestHelper helper) {
		ServerPlayer player = joinIsolated(helper, "lh-drain");
		TestHeroes.transform(player, RegulusHero.ID);
		float energyAtActivate = HeroDataStore.get(player).energy();

		AbilityRouter.activate(player, LION_HEART);

		helper.runAfterDelay(8, () -> {
			helper.assertTrue(HeroDataStore.get(player).energy() >= energyAtActivate - 0.5f,
					"no drain during the windup, energy=" + HeroDataStore.get(player).energy());
		});
		helper.runAfterDelay(26, () -> {
			float energy = HeroDataStore.get(player).energy();
			helper.assertTrue(energy < energyAtActivate - 60f,
					"the 10/t drain runs past the fire tick, energy=" + energy);

			Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
			zombie.moveTo(player.getX() + 2.5, player.getY(), player.getZ(), 0f, 0f);
			zombie.setNoAi(true);
			helper.getLevel().addFreshEntity(zombie);

			AbilityRouter.deactivate(player, LION_HEART);
			helper.assertFalse(LionHeartController.isBlocking(player), "shield down");
			helper.assertTrue(zombie.getDeltaMovement().horizontalDistance() > 1.0,
					"deactivation throws the shockwave push, v=" + zombie.getDeltaMovement());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}
}
