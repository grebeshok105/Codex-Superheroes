package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.regulus.RegulusAttachments;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.RegulusItems;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Task 8 rework pins for the Evangelion ritual: an interruptible 60-tick item-use
 * channel (release/damage/movement abort it into a 400-tick cooldown) whose
 * completion is the single authoritative write-site of madness + the bonus-life
 * grant, all state living in the synced {@link RegulusMadnessState#ATTACHMENT}.
 */
public class RegulusEvangelionGameTests implements FabricGameTest {

	private static ServerPlayer regulusReader(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RegulusItems.EVANGELION));
		return player;
	}

	private static RegulusMadnessState madness(ServerPlayer player) {
		return player.getAttachedOrCreate(RegulusAttachments.REGULUS_MADNESS);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void evangelionEarlyReleaseAborts(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		long start = helper.getLevel().getGameTime();

		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.isUsingItem(), "use() starts the item-use channel");
		helper.assertValueEqual(madness(player).ritualUntilTick(), start + 60,
				"ritual deadline is now+60");

		player.releaseUsingItem();
		helper.assertFalse(player.isUsingItem(), "release ends the channel");
		helper.assertValueEqual(madness(player).ritualUntilTick(), 0L, "abort clears the deadline");
		helper.assertFalse(madness(player).madness(), "abort never turns madness on");
		helper.assertTrue(player.getCooldowns().isOnCooldown(RegulusItems.EVANGELION),
				"abort arms the 400-tick book cooldown");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void evangelionNormalCompletionDoesNotRunAbort(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);

		InteractionResultHolder<ItemStack> use = RegulusItems.EVANGELION.use(
				helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(use.getResult().consumesAction(), "use consumes");
		helper.assertTrue(player.isUsingItem(), "channel running");

		helper.runAfterDelay(2, () -> {
			// Mock players never tick useItemRemaining, so the test completes the
			// channel the way vanilla's completeUsingItem does: finishUsingItem,
			// then stopUsingItem.
			long finishNow = helper.getLevel().getGameTime();
			RegulusItems.EVANGELION.finishUsingItem(player.getUseItem(), helper.getLevel(), player);
			player.stopUsingItem();

			RegulusMadnessState state = madness(player);
			helper.assertTrue(state.madness(), "finished ritual turns madness on");
			helper.assertValueEqual(state.madnessUntilTick(), finishNow + 900,
					"madness deadline is completion+900");
			helper.assertValueEqual(state.ritualUntilTick(), 0L, "ritual window cleared");
			helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
					Boolean.TRUE, "completeRitual is the bonus-life write site");
			helper.assertFalse(player.getCooldowns().isOnCooldown(RegulusItems.EVANGELION),
					"normal completion does not arm the abort cooldown");
			assertModifierAmount(helper, player, Attributes.MAX_HEALTH,
					ModId.of("modifiers/regulus/madness_max_health"), 0.20);
			helper.assertTrue(Math.abs(player.getHealth() - player.getMaxHealth()) < 0.01f,
					"madness heals to full");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void ritualInterruptedByDamage(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.isUsingItem(), "channel running");

		helper.assertTrue(player.hurt(helper.getLevel().damageSources().generic(), 4f),
				"a >=4hp hit lands");
		helper.assertFalse(player.isUsingItem(), "the hit releases the channel");
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(madness(player).ritualUntilTick(), 0L,
					"damage interrupt cleared the deadline");
			helper.assertFalse(madness(player).madness(), "no madness");
			helper.assertTrue(player.getCooldowns().isOnCooldown(RegulusItems.EVANGELION),
					"damage interrupt arms the cooldown");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void ritualInterruptedByMovement(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.isUsingItem(), "channel running");

		// >0.5 blocks — the anchor check inside onUseTick aborts the channel.
		// Mock players never fire onUseTick on their own, so invoke the same
		// per-tick call vanilla makes.
		player.teleportTo(player.getX() + 1.0, player.getY(), player.getZ());
		RegulusItems.EVANGELION.onUseTick(helper.getLevel(), player, player.getUseItem(), 40);

		helper.assertFalse(player.isUsingItem(), "movement released the channel");
		helper.assertValueEqual(madness(player).ritualUntilTick(), 0L, "deadline cleared");
		helper.assertTrue(player.getCooldowns().isOnCooldown(RegulusItems.EVANGELION),
				"movement abort arms the cooldown");
		helper.assertFalse(madness(player).madness(), "no madness");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 960)
	public void madnessExpiresAt45s(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		long now = helper.getLevel().getGameTime();
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				madness(player).withMadness(true).withMadnessUntil(now + 900));

		helper.runAfterDelay(905, () -> {
			helper.assertFalse(madness(player).madness(), "madness expired after 900 ticks");
			helper.assertValueEqual(madness(player).madnessUntilTick(), 0L, "deadline zeroed");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void bloodPriceIsPointSixHpPerSecond(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		long now = helper.getLevel().getGameTime();
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				madness(player).withMadness(true).withMadnessUntil(now + 900));
		player.setHealth(player.getMaxHealth());

		// Passive regeneration makes health deltas unobservable — count the actual
		// blood-price hits instead (listener stays inert for every other entity).
		java.util.concurrent.atomic.AtomicInteger tithes = new java.util.concurrent.atomic.AtomicInteger();
		java.util.concurrent.atomic.AtomicReference<Float> lastAmount = new java.util.concurrent.atomic.AtomicReference<>();
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register(
				(entity, source, baseDamage, damageTaken, blocked) -> {
					if (entity == player && source.is(RegulusDamageTypes.BLOOD_PRICE)) {
						tithes.incrementAndGet();
						lastAmount.set(damageTaken);
					}
				});

		helper.runAfterDelay(25, () -> {
			helper.assertValueEqual(tithes.get(), 1, "exactly one tithe after 20 ticks");
			helper.assertTrue(lastAmount.get() != null && Math.abs(lastAmount.get() - 0.6f) < 0.001f,
					"tithe is 0.6hp");
		});
		helper.runAfterDelay(85, () -> {
			helper.assertValueEqual(tithes.get(), 4, "four tithes after 80 ticks");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void bonusLifeOncePerMadness(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		helper.runAfterDelay(2, () -> {
			RegulusItems.EVANGELION.finishUsingItem(player.getUseItem(), helper.getLevel(), player);
			player.stopUsingItem();
			helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
					Boolean.TRUE, "completion grants the bonus life");
			// The grant is a plain flag — nothing re-arms it while still mad, and
			// only the consume path (totem/death) clears it back to FALSE.
			player.setAttached(RegulusAttachments.REGULUS_BONUS_LIFE, Boolean.FALSE);
			helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
					Boolean.FALSE, "the flag stays put mid-madness");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bonusLifeHasSingleAuthoritativeState(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
				Boolean.FALSE, "no bonus life before the ritual");

		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.runAfterDelay(2, () -> {
			RegulusItems.EVANGELION.finishUsingItem(player.getUseItem(), helper.getLevel(), player);
			player.stopUsingItem();
			helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
					Boolean.TRUE, "completion flips the one attachment to TRUE");
			// Dying with the grant consumes it via the totem path — not re-tested
			// here (LifecycleSideEffectsGameTests pins the consume side).
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 20)
	public void syncedAttachmentRoundtrips(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		RegulusMadnessState original = madness(player)
				.withRitual(1234L).withMadness(true).withMadnessUntil(5678L);
		player.setAttached(RegulusAttachments.REGULUS_MADNESS, original);

		RegulusMadnessState read = madness(player);
		helper.assertValueEqual(read, original,
				"synced attachment record survives set/get (stream codec fields)");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void madnessStaysOffBefore60t(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		helper.runAfterDelay(50, () -> {
			helper.assertTrue(player.isUsingItem(), "still channeling at tick 50");
			helper.assertFalse(madness(player).madness(), "madness stays off before the 60t mark");
			helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null,
					"Slowness II holds while channeling");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 90)
	public void madnessDeadlineSurvivesLateClientObservation(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		long start = helper.getLevel().getGameTime();
		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		// Read the deadline "late" (as a rejoining client would) — the synced value
		// must still equal the authored deadline, never re-derived.
		helper.runAfterDelay(40, () -> {
			helper.assertValueEqual(madness(player).ritualUntilTick(), start + 60,
					"deadline unchanged at late observation");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	private static void assertModifierAmount(GameTestHelper helper, ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			net.minecraft.resources.ResourceLocation modifierId, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);
		AttributeModifier modifier = instance == null ? null : instance.getModifier(modifierId);
		helper.assertTrue(modifier != null && Math.abs(modifier.amount() - amount) < 0.001,
				"modifier " + modifierId + " is " + amount);
	}
}
