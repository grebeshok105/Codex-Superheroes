package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.hero.regulus.RegulusAttachments;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.RegulusItems;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
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
		return regulusReader(helper, false);
	}

	private static ServerPlayer regulusReader(GameTestHelper helper, boolean vulnerable) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		// Keep spawn invulnerability when the test doesn't need real damage — the
		// shared gametest world lets other tests' entities wander in and hit the reader,
		// which would trip the >=4hp interrupt nondeterministically.
		if (vulnerable) {
			TestPlayers.clearSpawnInvulnerability(player);
		}
		// getItemInHand reads items[selected] and returns EMPTY for a non-hotbar
		// selected — pin it before equipping the book.
		player.getInventory().selected = 0;
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RegulusItems.EVANGELION));
		// Gametest world is void outside structures — a drifting/falling reader
		// trips the >0.5b movement abort. Pin it on a stone pad.
		BlockPos feet = BlockPos.containing(player.position());
		placeFloor(helper, player.getX(), feet.getY(), player.getZ());
		player.moveTo(player.getX(), feet.getY(), player.getZ());
		player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		return player;
	}

	/** The three early-return gates inside {@code use()}, asserted explicitly so a
	 * silent gate rejection can't masquerade as a dropped channel. */
	private static void assertUseGatesOpen(GameTestHelper helper, ServerPlayer player) {
		var data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		helper.assertTrue(data.hasHero() && ModId.of("regulus").equals(data.heroId()),
				"use() gate: player is regulus");
		helper.assertFalse(player.getCooldowns().isOnCooldown(RegulusItems.EVANGELION),
				"use() gate: evangelion not on cooldown");
		RegulusMadnessState state = madness(player);
		helper.assertFalse(state.madness() || state.isReading(helper.getLevel().getGameTime()),
				"use() gate: not mad and not already reading (state=" + state
						+ ", now=" + helper.getLevel().getGameTime() + ")");
	}

	private static RegulusMadnessState madness(ServerPlayer player) {
		return player.getAttachedOrCreate(RegulusAttachments.REGULUS_MADNESS);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void evangelionEarlyReleaseAborts(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper);
		long start = helper.getLevel().getGameTime();

		assertUseGatesOpen(helper, player);
		helper.assertValueEqual(player.getItemInHand(InteractionHand.MAIN_HAND).getItem(),
				RegulusItems.EVANGELION, "evangelion is in the main hand");
		var res = RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(res.getResult(), net.minecraft.world.InteractionResult.CONSUME,
				"use() consumed");
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
			// Complete the channel the way vanilla's completeUsingItem does —
			// finishUsingItem then stopUsingItem — at a fixed +2t instead of
			// racing the mock's own use-channel ticking against the 60t deadline.
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
			helper.assertTrue(Math.abs(player.getMaxHealth() - 24.0f) < 0.01f,
					"madness scales max health x1.2");
			helper.assertTrue(Math.abs(player.getHealth() - player.getMaxHealth()) < 0.01f,
					"madness heals to full");
			assertModifierAmount(helper, player, Attributes.ARMOR,
					ModId.of("modifiers/regulus/madness_armor"), 10.0);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 30.0) < 0.001,
					"the resolved armor value clamps at vanilla's 30 cap");
			assertModifierAmount(helper, player, Attributes.ATTACK_DAMAGE,
					ModId.of("modifiers/regulus/madness_damage"), 0.40);
			assertMadnessEffect(helper, player, MobEffects.MOVEMENT_SPEED, 2);
			assertMadnessEffect(helper, player, MobEffects.DAMAGE_BOOST, 2);
			assertMadnessEffect(helper, player, MobEffects.JUMP, 2);
			assertMadnessEffect(helper, player, MobEffects.DAMAGE_RESISTANCE, 0);
			// The amp-0 madness regen loses the merge to the infinite amp-0
			// passive — only the passive instance is observable.
			MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(regen != null && regen.isInfiniteDuration(),
					"the passive regen survives into madness");
			helper.assertFalse(RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
							.getResult().consumesAction(),
					"evangelion refuses while mad");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void ritualInterruptedByDamage(GameTestHelper helper) {
		ServerPlayer player = regulusReader(helper, true);
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
		// Invoke the same per-tick call vanilla's updatingUsingItem makes right
		// after the teleport rather than waiting for the next real tick.
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
		ServerPlayer player = regulusReader(helper, true);
		long now = helper.getLevel().getGameTime();
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				madness(player).withMadness(true).withMadnessUntil(now + 900));
		// Take the passive regen out — its heal would pollute the health deltas
		// the tithes are counted by. The blood-price type bypasses armor,
		// resistance and enchantments, so every 20-tick hit lands raw.
		player.removeEffect(MobEffects.REGENERATION);
		float baseline = player.getMaxHealth();
		player.setHealth(baseline);

		helper.runAfterDelay(25, () -> {
			float lost = baseline - player.getHealth();
			helper.assertTrue(lost > 0.55f && lost < 0.65f,
					"exactly one 0.6hp tithe after 20 ticks (lost=" + lost + ")");
		});
		helper.runAfterDelay(85, () -> {
			float lost = baseline - player.getHealth();
			helper.assertTrue(lost > 2.35f && lost < 2.45f,
					"four 0.6hp tithes after 80 ticks (lost=" + lost + ")");
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
		assertUseGatesOpen(helper, player);
		RegulusItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		helper.assertTrue(player.isUsingItem(), "use() starts the channel");
		long start = helper.getLevel().getGameTime();
		helper.runAfterDelay(50, () -> {
			// Order matters: madness first distinguishes a vanilla channel completion
			// (useItemRemaining decayed to 0) from an abort (deadline cleared, no
			// madness) and from an external channel stop (deadline still armed).
			helper.assertFalse(madness(player).madness(), "madness stays off before the 60t mark");
			helper.assertValueEqual(madness(player).ritualUntilTick(), start + 60,
					"ritual deadline still armed at tick 50");
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

	/** A 3x3 stone pad at feet level — the gametest world is void outside structures. */
	private static void placeFloor(GameTestHelper helper, double x, double y, double z) {
		BlockPos center = BlockPos.containing(x, y - 1.0, z);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.getLevel().setBlock(center.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	/** The madness-owned signature: 60-tick, ambient, icon-only, fixed amplifier. */
	private static void assertMadnessEffect(GameTestHelper helper, ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		MobEffectInstance instance = player.getEffect(effect);
		helper.assertTrue(instance != null && !instance.isInfiniteDuration()
						&& instance.getDuration() <= 60 && instance.getAmplifier() == amplifier
						&& instance.isAmbient() && !instance.isVisible(),
				"madness applies " + effect.unwrapKey().map(k -> k.location().toString()).orElse("?")
						+ " as a 60-tick ambient amp-" + amplifier + " instance");
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
