package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusTotemController;
import io.github.grebeshok105.codex.hero.regulus.RegulusAttachments;
import io.github.grebeshok105.codex.hero.regulus.RegulusItems;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * I5b characterization pins for Regulus — current behavior of the madness pipeline
 * (Evangelion reading window → madness + bonus life + attribute/effect schedule),
 * the totem ward, the counter sequence (lift/arrive/slam, flight strip, energy lock)
 * and greed/roar/lion-heart mechanics, so the move to {@code hero/regulus} can
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
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RegulusItems.REGULUS_SUIT));

		InteractionResultHolder<ItemStack> transform = RegulusItems.REGULUS_SUIT.use(
				helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(transform.getResult().consumesAction(), "suit use transforms");
		helper.assertValueEqual(HeroDataStore.get(player).heroId(), RegulusHero.ID,
				"hero after suit use");

		// The transformation item stamps a 20-tick cooldown on both directions.
		helper.runAfterDelay(25, () -> {
			player.setShiftKeyDown(true);
			try {
				InteractionResultHolder<ItemStack> undo = RegulusItems.REGULUS_SUIT.use(
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
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
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
		regulus.setAttached(RegulusAttachments.REGULUS_MADNESS,
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
		// the revive now strips harmful effects only — the hero's infinite fire-resistance
		// passive survives, so no finite 800-tick instance is observable
		helper.assertTrue(player.getEffect(MobEffects.FIRE_RESISTANCE) != null,
				"ward revive keeps the fire-resistance passive");

		player.setAttached(RegulusAttachments.REGULUS_BONUS_LIFE, Boolean.TRUE);
		player.invulnerableTime = 0;
		player.kill();
		helper.assertTrue(player.isAlive(), "the bonus life refuses the second death");
		helper.assertTrue(Math.abs(player.getHealth() - player.getMaxHealth() * 0.5f) < 0.01f,
				"the bonus life restores half health");
		helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
				Boolean.FALSE, "the bonus life is consumed");
		assertMinEffect(helper, player, MobEffects.REGENERATION, 1, 600, "bonus regen");
		assertMinEffect(helper, player, MobEffects.ABSORPTION, 1, 200, "bonus absorption");

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

		// The victim must keep ticking — entities parked in a transient far chunk are
		// registered but never ticked, so the magnet's pull never displaces them. And it
		// must stand level with the caster: if it lands in a lower pocket the normalized
		// pull dir goes steep and the horizontal impulse shrinks below the assert floor.
		Zombie zombie = spawnAhead(helper, player, 4.0);
		zombie.setNoAi(true);
		BlockPos feet = zombie.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, -1, 1))) {
			helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
		}
		zombie.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0f, 0f);
		zombie.setDeltaMovement(Vec3.ZERO);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			awaitGreedSees(helper, player, zombie, 40, () -> {
			// findTarget's angular hitbox is bbSize*0.6 around the eye ray — aim the ray
			// at the victim's center or the vertical offset alone rejects it.
			player.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getBoundingBox().getCenter());
			Vec3 anchor = zombie.position();
			AbilityRouter.activate(player, MANIA_OF_GREED);
			helper.assertTrue(HeroDataStore.get(player).isActive(MANIA_OF_GREED),
					"the magnet engages on the aimed target");

			awaitGreedPull(helper, player, zombie, anchor, 15, () -> {
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
		});
	}

	/**
	 * {@link TestPlayers#awaitVisible} only waits for the entity to enter the UUID index,
	 * but {@code ManiaOfGreedAbility.findTarget} reads the spatial section via
	 * {@code getEntities(box)} — a fresh mob can be index-visible a tick or two before the
	 * section sees it. Poll the same query shape the ability runs before activating.
	 */
	private static void awaitGreedSees(GameTestHelper helper, ServerPlayer player, LivingEntity victim,
			int tries, Runnable body) {
		Vec3 look = player.getViewVector(1.0f);
		AABB box = player.getBoundingBox().expandTowards(look.scale(100.0)).inflate(1.5);
		if (tries <= 0
				|| helper.getLevel().getEntities(player, box, e -> e == victim).contains(victim)) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitGreedSees(helper, player, victim, tries - 1, body));
	}

	/**
	 * The magnet's per-tick impulse lands in the player phase; whether the victim's section
	 * actually moves it varies with chunk boundaries and tick order, and a freshly spawned
	 * victim can still be airborne when first sampled (its normalized dir then shrinks the
	 * horizontal component). Accept either observable — real displacement toward the caster,
	 * or a toward-caster deltaMovement holding the last impulse (a non-ticking victim's delta
	 * never decays; a ticking one reads ≥ ~0.33 right after an impulse tick) — and retry
	 * while neither holds. Both staying false means the magnet never impulsed.
	 */
	private static void awaitGreedPull(GameTestHelper helper, ServerPlayer player, LivingEntity victim,
			Vec3 anchor, int tries, Runnable body) {
		Vec3 pull = victim.getDeltaMovement();
		Vec3 toCaster = player.position().subtract(victim.position()).normalize();
		Vec3 moved = victim.position().subtract(anchor);
		double pulled = moved.horizontalDistance();
		boolean dragged = pulled > 0.3 && (pulled < 1.0e-4
				|| toCaster.dot(moved.normalize()) > 0);
		boolean impulsed = pull.horizontalDistance() > 0.25 && pull.dot(toCaster) > 0;
		if (dragged || impulsed) {
			body.run();
			return;
		}
		helper.assertTrue(tries > 0, "the magnet pulls the victim toward the caster (pulled="
				+ pulled + " delta=" + pull + " active="
				+ HeroDataStore.get(player).isActive(MANIA_OF_GREED)
				+ " frozen=" + RegulusGreedController.isFrozen(victim)
				+ " alive=" + victim.isAlive() + " vtc=" + victim.tickCount + ")");
		helper.runAfterDelay(1, () -> awaitGreedPull(helper, player, victim, anchor, tries - 1, body));
	}

	private static void assertInfinite(GameTestHelper helper, ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier,
			String name) {
		MobEffectInstance instance = player.getEffect(effect);
		helper.assertTrue(instance != null && instance.isInfiniteDuration()
						&& instance.getAmplifier() == amplifier,
				"regulus passive " + name + " is infinite amp-" + amplifier);
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

	private static void assertMinEffect(GameTestHelper helper, ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier,
			int minDuration, String name) {
		MobEffectInstance instance = player.getEffect(effect);
		helper.assertTrue(instance != null && instance.getDuration() >= minDuration
						&& instance.getAmplifier() == amplifier,
				name + " is an amp-" + amplifier + " effect lasting at least " + minDuration + " ticks");
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

}
