package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.effect.RegulusGreedController;
import io.github.grebeshok105.codex.effect.RegulusMadnessController;
import io.github.grebeshok105.codex.effect.RegulusMadnessState;
import io.github.grebeshok105.codex.effect.RegulusTotemController;
import io.github.grebeshok105.codex.hero.RegulusHero;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.mechanic.ability.SharedAbilityIds;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * I5b characterization pins for Regulus — current behavior of the madness pipeline
 * (Evangelion reading window → madness + bonus life + attribute/effect schedule),
 * the totem ward, the counter sequence (lift/arrive/slam, flight strip, energy lock)
 * and greed/cage/roar/lion-heart mechanics, so the move to {@code hero/regulus} can
 * be judged byte-equivalent. Ability ids stay literal {@code ModId.of(...)} — the
 * {@code AbilityIds} constants relocate during the wave.
 */
public class RegulusGameTests implements FabricGameTest {

	private static final ResourceLocation LION_HEART = ModId.of("lion_heart");
	private static final ResourceLocation MANIA_OF_GREED = ModId.of("mania_of_greed");
	private static final ResourceLocation GREEDS_EMBRACE = ModId.of("greeds_embrace");
	private static final ResourceLocation LION_ROAR = ModId.of("lion_roar");
	private static final ResourceLocation COUNTER_STRIKE = ModId.of("counter_strike");

	@GameTest(template = EMPTY_STRUCTURE)
	public void regulusOwnsAbilitySlotsInOrder(GameTestHelper helper) {
		Hero hero = Heroes.get(ModId.of("regulus"));
		helper.assertTrue(hero != null, "regulus hero is registered");
		helper.assertValueEqual(hero.getAbilities(),
				List.of(LION_HEART, MANIA_OF_GREED, GREEDS_EMBRACE, LION_ROAR, COUNTER_STRIKE),
				"regulus ability slots in order");
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " is registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void regulusSuitTransformsAndShiftUntransforms(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.REGULUS_SUIT));

		InteractionResultHolder<ItemStack> transform = ModItems.REGULUS_SUIT.use(
				helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(transform.getResult().consumesAction(), "suit use transforms");
		helper.assertValueEqual(HeroDataStore.get(player).heroId(), RegulusHero.ID,
				"hero after suit use");

		// The transformation item stamps a 20-tick cooldown on both directions.
		helper.runAfterDelay(25, () -> {
			player.setShiftKeyDown(true);
			try {
				InteractionResultHolder<ItemStack> undo = ModItems.REGULUS_SUIT.use(
						helper.getLevel(), player, InteractionHand.MAIN_HAND);
				helper.assertTrue(undo.getResult().consumesAction(), "shift-use untransforms");
			} finally {
				player.setShiftKeyDown(false);
			}
			helper.assertFalse(HeroDataStore.get(player).hasHero(), "hero cleared after shift-use");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void transformAppliesRegulusPassivesAndFullEnergy(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);

		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 1000f) < 0.001f,
				"fresh transform starts at full energy (1000)");
		// Vanilla clamps Attributes.ARMOR to [0,30] — pin the modifier, not the clamped value.
		assertModifierAmount(helper, player, Attributes.ARMOR,
				ModId.of("modifiers/regulus/armor"), 70.0, "regulus passive armor");
		helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 30.0) < 0.001,
				"the resolved armor value clamps at vanilla's 30 cap");
		helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 1.0) < 0.001,
				"regulus passive knockback resistance 1.0");
		assertInfinite(helper, player, MobEffects.REGENERATION, 0, "regeneration");
		assertInfinite(helper, player, MobEffects.MOVEMENT_SPEED, 1, "speed");
		assertInfinite(helper, player, MobEffects.DAMAGE_BOOST, 1, "strength");
		assertInfinite(helper, player, MobEffects.JUMP, 1, "jump boost");
		assertInfinite(helper, player, MobEffects.FIRE_RESISTANCE, 0, "fire resistance");

		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * The Evangelion item opens a 200-tick reading window that denies all damage; when it
	 * lapses the player turns mad: bonus life granted, +10 armor / +20% max health / +0.4
	 * attack damage, a full heal and the 60-tick ambient buff schedule (amp 2 speed /
	 * strength / jump, amp 0 regen / resistance).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 260)
	public void evangelionReadingBlocksDamageThenTurnsMad(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EVANGELION));

		InteractionResultHolder<ItemStack> use = ModItems.EVANGELION.use(
				helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(use.getResult().consumesAction(), "evangelion use starts reading");
		RegulusMadnessState state = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
		helper.assertTrue(state.isReading(helper.getLevel().getGameTime()),
				"reading window is open");
		helper.assertTrue(state.readingUntilTick() >= helper.getLevel().getGameTime() + 190,
				"the window spans ~200 ticks");
		helper.assertFalse(player.hurt(helper.getLevel().damageSources().generic(), 1f),
				"the reading window denies all damage");
		helper.assertFalse(ModItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
						.getResult().consumesAction(),
				"a second use fails while already reading");

		helper.runAfterDelay(205, () -> {
			RegulusMadnessState mad = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
			helper.assertTrue(mad.madness(), "madness turns on when the window lapses");
			helper.assertFalse(mad.isReading(helper.getLevel().getGameTime()),
					"the window is closed");
			helper.assertValueEqual(player.getAttachedOrCreate(ModAttachments.REGULUS_BONUS_LIFE),
					Boolean.TRUE, "madness grants the bonus life");
			assertModifierAmount(helper, player, Attributes.ARMOR,
					ModId.of("modifiers/regulus/madness_armor"), 10.0,
					"madness armor +10 on top of the passive 70 (resolved value stays clamped at 30)");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 30.0) < 0.001,
					"the resolved armor value clamps at vanilla's 30 cap");
			assertModifierAmount(helper, player, Attributes.MAX_HEALTH,
					ModId.of("modifiers/regulus/madness_max_health"), 0.20,
					"madness max-health x1.2 modifier");
			helper.assertTrue(Math.abs(player.getMaxHealth() - 24.0f) < 0.01f,
					"madness scales max health x1.2");
			helper.assertTrue(Math.abs(player.getHealth() - player.getMaxHealth()) < 0.01f,
					"madness heals to full");
			assertModifierAmount(helper, player, Attributes.ATTACK_DAMAGE,
					ModId.of("modifiers/regulus/madness_damage"), 0.40,
					"madness attack-damage +0.4 modifier");
			assertMadnessEffect(helper, player, MobEffects.MOVEMENT_SPEED, 2);
			assertMadnessEffect(helper, player, MobEffects.DAMAGE_BOOST, 2);
			assertMadnessEffect(helper, player, MobEffects.JUMP, 2);
			assertMadnessEffect(helper, player, MobEffects.DAMAGE_RESISTANCE, 0);
			// The amp-0 madness regen merges into the infinite amp-0 passive instance —
			// the passive's longer duration wins, so only the passive is observable.
			MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(regen != null && regen.isInfiniteDuration(),
					"the passive regen survives into madness");
			helper.assertFalse(ModItems.EVANGELION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
							.getResult().consumesAction(),
					"evangelion refuses while mad");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void counterStrikeDeniedWithoutMadness(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		Zombie attacker = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		helper.assertTrue(player.hurt(helper.getLevel().damageSources().mobAttack(attacker), 5f),
				"the attacker is on record");

		AbilityRouter.activate(player, COUNTER_STRIKE);
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, COUNTER_STRIKE),
				"counter strike never arms its cooldown without madness");
		helper.assertFalse(RegulusMadnessController.isCounterInvolved(attacker),
				"no counter started");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * The counter sequence: the last damager is NO_AI/NO_GRAVITY-locked and lifted
	 * ~30 blocks over 20 ticks, the Regulus player is teleported in (20 ticks), then the
	 * victim is slammed back down — carving a crater, dealing counter damage and locking
	 * the player's energy for 15 s.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 170)
	public void counterStrikeLiftsSlamsAndLocks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setAttached(ModAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		// Stay on the structure: the counter resolves its victim through the entity
		// section manager, which drops entities once a transient chunk load ends, and
		// findTarget prefers the recorded last damager over the 120-block sweep anyway.
		Warden warden = spawnEntity(helper, EntityType.WARDEN,
				player.getX() + 2.0, player.getY(), player.getZ());

		TestPlayers.awaitVisible(helper, warden, () -> {
			player.invulnerableTime = 0;
			helper.assertTrue(
					player.hurt(helper.getLevel().damageSources().mobAttack(warden), 5f),
					"the warden hit registers as last damager");
			double startY = warden.getY();
			AbilityRouter.activate(player, COUNTER_STRIKE);
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, COUNTER_STRIKE),
					"counter strike arms its 30 s cooldown");
			helper.assertTrue(RegulusMadnessController.isCounterInvolved(warden),
					"the warden is the counter victim");

			helper.runAfterDelay(12, () -> {
				helper.assertTrue(
						TestPlayers.lockOwners(warden, ControlLockKind.NO_AI).contains(player.getUUID()),
						"LIFT holds the NO_AI lock");
				helper.assertTrue(warden.getY() > startY + 6.0,
						"LIFT raises the attacker ~1.5 blocks per tick");
			});
			helper.runAfterDelay(90, () -> {
				helper.assertTrue(warden.getHealth() < warden.getMaxHealth(),
						"the slam dealt counter damage");
				helper.assertTrue(EnergyLocks.isLocked(player),
						"the final slam locks Regulus energy for 15 s");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * Madness-driven flight deny: {@code triggerCounter} deactivates the victim's flight
	 * ability and clears the vanilla flight flags, so a countered flyer cannot fly away.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void counterStripsAttackersFlight(GameTestHelper helper) {
		ServerPlayer regulus = TestPlayers.join(helper);
		ServerPlayer flyer = TestPlayers.join(helper, "regulus-counter-flyer");
		TestHeroes.transform(regulus, RegulusHero.ID);
		TestHeroes.transform(flyer, ModId.of("omniman"));
		regulus.setAttached(ModAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		AbilityRouter.activate(flyer, SharedAbilityIds.FLIGHT);
		helper.assertTrue(HeroDataStore.get(flyer).isActive(SharedAbilityIds.FLIGHT)
						&& flyer.getAbilities().flying,
				"precondition: the flyer is airborne under the flight ability");

		RegulusMadnessController.triggerCounter(regulus, flyer);
		helper.assertFalse(HeroDataStore.get(flyer).isActive(SharedAbilityIds.FLIGHT),
				"the counter deactivates the flight ability");
		helper.assertFalse(flyer.getAbilities().flying, "vanilla flying flag cleared");
		helper.assertFalse(flyer.getAbilities().mayfly, "vanilla mayfly flag cleared");

		RegulusMadnessController.clearMadness(regulus); // aborts the counter before LIFT runs
		TestPlayers.leave(regulus);
		TestPlayers.leave(flyer);
		helper.succeed();
	}

	/**
	 * The ward absorbs exactly one death (full heal + 900/100/800-tick regen, absorption,
	 * fire resistance); the second death consumes the madness bonus life at half health;
	 * the third death is real.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void totemWardSavesThenConsumesBonusLifeThenDies(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);

		player.kill();
		helper.assertTrue(player.isAlive(), "the ward refuses the first death");
		helper.assertTrue(Math.abs(player.getHealth() - player.getMaxHealth()) < 0.01f,
				"the ward restores full health");
		helper.assertTrue(RegulusTotemController.wasUsed(player.getUUID()),
				"the ward is marked used");
		assertFiniteEffect(helper, player, MobEffects.REGENERATION, 1, 900, "ward regen");
		assertFiniteEffect(helper, player, MobEffects.ABSORPTION, 1, 100, "ward absorption");
		assertFiniteEffect(helper, player, MobEffects.FIRE_RESISTANCE, 0, 800, "ward fire resistance");

		player.setAttached(ModAttachments.REGULUS_BONUS_LIFE, Boolean.TRUE);
		player.invulnerableTime = 0;
		player.kill();
		helper.assertTrue(player.isAlive(), "the bonus life refuses the second death");
		helper.assertTrue(Math.abs(player.getHealth() - player.getMaxHealth() * 0.5f) < 0.01f,
				"the bonus life restores half health");
		helper.assertValueEqual(player.getAttachedOrCreate(ModAttachments.REGULUS_BONUS_LIFE),
				Boolean.FALSE, "the bonus life is consumed");
		assertFiniteEffect(helper, player, MobEffects.REGENERATION, 1, 600, "bonus regen");
		assertFiniteEffect(helper, player, MobEffects.ABSORPTION, 1, 200, "bonus absorption");

		player.invulnerableTime = 0;
		player.kill();
		helper.assertFalse(player.isAlive(), "the third death is real — ward and bonus are spent");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * Mania of Greed: a toggle that magnet-pulls the aimed victim while rooting the
	 * caster (amp-250 slowdown). Toggling off freezes the victim for 200 ticks — NO_AI,
	 * pinned to its lock position, damage queued instead of applied — and buffs the
	 * caster; the queued damage lands when the freeze ends.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void maniaOfGreedMagnetsFreezesAndQueuesDamage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);

		double isoX = player.getX() + 3000.0;
		double isoZ = player.getZ() + 3000.0;
		helper.getLevel().getChunk(BlockPos.containing(isoX, player.getY(), isoZ));
		placeFloor(helper, isoX, player.getY(), isoZ);
		player.teleportTo(isoX, player.getY(), isoZ);
		Vec3 aheadPos = player.position().add(player.getViewVector(1f).normalize().scale(4.0));
		placeFloor(helper, aheadPos.x, player.getY(), aheadPos.z);
		Zombie zombie = spawnEntity(helper, EntityType.ZOMBIE, aheadPos.x, player.getY(), aheadPos.z);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			// findTarget's angular hitbox is bbSize*0.6 around the eye ray — aim the ray
			// at the victim's center or the vertical offset alone rejects it.
			player.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getBoundingBox().getCenter());
			double dist0 = zombie.distanceTo(player);
			AbilityRouter.activate(player, MANIA_OF_GREED);
			helper.assertTrue(HeroDataStore.get(player).isActive(MANIA_OF_GREED),
					"the magnet engages on the aimed target");

			helper.runAfterDelay(6, () -> {
				helper.assertTrue(zombie.distanceTo(player) < dist0,
						"the magnet pulls the victim in");
				MobEffectInstance slow = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
				helper.assertTrue(slow != null && slow.getAmplifier() == 250
								&& slow.getDuration() <= 200 && slow.isAmbient(),
						"the caster carries the amp-250 slowdown while magnetized");

				AbilityRouter.deactivate(player, MANIA_OF_GREED);
				helper.assertFalse(HeroDataStore.get(player).isActive(MANIA_OF_GREED),
						"toggling off releases the magnet");
				helper.assertTrue(AbilityCooldowns.isOnCooldown(player, MANIA_OF_GREED),
						"release arms the 25 s cooldown");
				helper.assertTrue(RegulusGreedController.isFrozen(zombie),
						"the victim is frozen");
				helper.assertTrue(
						TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).contains(player.getUUID()),
						"freeze holds the NO_AI lock");
				MobEffectInstance resist = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
				helper.assertTrue(resist != null && resist.getAmplifier() == 4
								&& !resist.isInfiniteDuration() && resist.getDuration() <= 200,
						"the caster gains the 200-tick amp-4 resistance");

				float hp = zombie.getHealth();
				helper.assertFalse(
						zombie.hurt(helper.getLevel().damageSources().generic(), 5f),
						"a frozen victim queues damage instead of taking it");
				helper.assertTrue(Math.abs(zombie.getHealth() - hp) < 0.001f,
						"queued damage is not applied while frozen");

				helper.runAfterDelay(215, () -> {
					helper.assertFalse(RegulusGreedController.isFrozen(zombie),
							"the freeze expires after 200 ticks");
					helper.assertTrue(
							TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty(),
							"the freeze releases the NO_AI lock");
					helper.assertTrue(zombie.getHealth() < hp,
							"the queued damage lands on release");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Greed's Embrace: 35 damage + weakness amp 1 + a straight-up launch on everything
	 * near the aim point; the cage then applies {@code fallDistance * 0.2 * maxHealth}
	 * on landing — lethal even to a 500 hp warden after a 4-block launch.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void greedsEmbraceLaunchesAndCageScalesFallDamage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);

		double isoX = player.getX() - 6000.0;
		double isoZ = player.getZ() + 6000.0;
		helper.getLevel().getChunk(BlockPos.containing(isoX, player.getY(), isoZ));
		placeFloor(helper, isoX, player.getY(), isoZ);
		placeFloor(helper, isoX + 3.0, player.getY(), isoZ);
		player.teleportTo(isoX, player.getY(), isoZ);
		Warden warden = spawnEntity(helper, EntityType.WARDEN, isoX + 3.0, player.getY(), isoZ);

		TestPlayers.awaitVisible(helper, warden, () -> {
			player.setXRot(90f); // aim straight down — the anchor is the block underfoot
			AbilityRouter.activate(player, GREEDS_EMBRACE);
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, GREEDS_EMBRACE),
					"greed's embrace arms its 60 s cooldown");
			helper.assertTrue(HeroDataStore.get(player).energy() < 400f,
					"the 700-energy activation is charged");
			helper.assertTrue(warden.getHealth() < warden.getMaxHealth(),
					"the gather deals its 35 damage");
			MobEffectInstance weakness = warden.getEffect(MobEffects.WEAKNESS);
			helper.assertTrue(weakness != null && weakness.getAmplifier() == 1,
					"the gather applies amp-1 weakness");
			helper.assertTrue(warden.getDeltaMovement().y > 0.0,
					"the gather launches the victim upward");

			helper.runAfterDelay(70, () -> {
				helper.assertFalse(warden.isAlive(),
						"the cage's scaled fall damage kills the launched warden");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * Lion's Heart is a toggle: amp-4 infinite resistance on activation, a 4-block
	 * shockwave push, 10 energy per tick drained against the +2 regen while active, and
	 * the resistance stripped on deactivate.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void lionHeartTogglesResistancePushesAndDrains(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		// Player-relative spawn — the mock player does not stand at the structure origin.
		Zombie zombie = spawnAhead(helper, player, 2.5);
		zombie.setNoAi(true); // a pathing zombie re-closes the gap and cancels the knockback assert
		float energy0 = HeroDataStore.get(player).energy();

		AbilityRouter.activate(player, LION_HEART);
		helper.assertTrue(HeroDataStore.get(player).isActive(LION_HEART),
				"lion's heart toggles on");
		MobEffectInstance resist = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
		helper.assertTrue(resist != null && resist.getAmplifier() == 4 && resist.isInfiniteDuration(),
				"activation grants infinite amp-4 resistance");

		double dist0 = zombie.distanceTo(player);
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(zombie.distanceTo(player) > dist0 + 0.2,
					"the activation shockwave pushes nearby mobs away");
			helper.assertTrue(HeroDataStore.get(player).energy() < energy0,
					"the toggle drains 10 energy per tick while active");

			AbilityRouter.deactivate(player, LION_HEART);
			helper.assertFalse(HeroDataStore.get(player).isActive(LION_HEART),
					"lion's heart toggles off");
			helper.assertFalse(player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
					"deactivate strips the resistance");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * Lion's Roar is a 45° half-angle cone at 18 blocks for 14 damage + knockback and
	 * costs 150 energy — targets behind the caster are untouched.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lionRoarHitsConeNotBehind(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		Zombie ahead = spawnAhead(helper, player, 3.0);
		ahead.setNoAi(true);
		Vec3 back = player.position().subtract(player.getViewVector(1f).normalize().scale(3.0));
		Zombie behind = spawnEntity(helper, EntityType.ZOMBIE, back.x, player.getY(), back.z);
		behind.setNoAi(true);

		TestPlayers.awaitVisible(helper, ahead, () -> TestPlayers.awaitVisible(helper, behind, () -> {
			AbilityRouter.activate(player, LION_ROAR);
			helper.assertTrue(ahead.getHealth() < ahead.getMaxHealth(),
					"the roar hits in front of the caster");
			helper.assertTrue(Math.abs(behind.getHealth() - behind.getMaxHealth()) < 0.001f,
					"the roar misses behind the caster");
			helper.assertTrue(HeroDataStore.get(player).energy() <= 850.5f,
					"the roar charges 150 energy");
			TestPlayers.leave(player);
			helper.succeed();
		}));
	}

	private static void assertInfinite(GameTestHelper helper, ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier,
			String name) {
		MobEffectInstance instance = player.getEffect(effect);
		helper.assertTrue(instance != null && instance.isInfiniteDuration()
						&& instance.getAmplifier() == amplifier,
				"regulus passive " + name + " is infinite amp-" + amplifier);
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
			ResourceLocation modifierId, double amount, String name) {
		AttributeInstance instance = player.getAttribute(attribute);
		AttributeModifier modifier = instance == null ? null : instance.getModifier(modifierId);
		helper.assertTrue(modifier != null && Math.abs(modifier.amount() - amount) < 0.001,
				name + " modifier " + modifierId + " is " + amount);
	}

	private static void assertFiniteEffect(GameTestHelper helper, ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier,
			int maxDuration, String name) {
		MobEffectInstance instance = player.getEffect(effect);
		helper.assertTrue(instance != null && !instance.isInfiniteDuration()
						&& instance.getDuration() <= maxDuration && instance.getAmplifier() == amplifier,
				name + " is a finite amp-" + amplifier + " effect within " + maxDuration + " ticks");
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player, double distance) {
		Vec3 ahead = player.position().add(player.getViewVector(1f).normalize().scale(distance));
		return spawnEntity(helper, EntityType.ZOMBIE, ahead.x, player.getY(), ahead.z);
	}

	private static <T extends Entity> T spawnEntity(GameTestHelper helper, EntityType<T> type,
			double x, double y, double z) {
		// Force-load the destination chunk first: in an unloaded chunk the entity is
		// never registered for area scans, so controllers would see an empty world.
		helper.getLevel().getChunk(BlockPos.containing(x, y, z));
		T entity = type.create(helper.getLevel());
		entity.moveTo(x, y, z, 0f, 0f);
		helper.getLevel().addFreshEntity(entity);
		return entity;
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
}
