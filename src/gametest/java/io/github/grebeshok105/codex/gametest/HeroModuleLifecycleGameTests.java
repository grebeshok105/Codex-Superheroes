package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.RaidenSwordDrawAbility;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSword;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.effect.KratosRageController;
import io.github.grebeshok105.codex.effect.RaidenState;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardState;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSwordDrawCeremonyController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardTimeSlowController;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.hero.AbilityScopedModifiers;
import io.github.grebeshok105.codex.hero.KratosHero;
import io.github.grebeshok105.codex.hero.RaidenHero;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardHero;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardAttachments;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardItems;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardModifiers;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;

import java.util.List;

/**
 * Characterization tests for the lifecycle hooks migrated into hero modules in D2b-2 —
 * these clear/onPlayerGone/resetAll bodies have side effects beyond map drops (items,
 * {@code EntityControlLock}, attribute modifiers), so they stay explicit
 * {@code ctx.lifecycle()} hooks and this file pins their behavior.
 */
public final class HeroModuleLifecycleGameTests implements FabricGameTest {
	/** Mirrors {@code ReinhardTimeSlowController#FREEZE_MODIFIER_ID} (private). */
	private static final ResourceLocation TIME_SLOW_FREEZE = ModId.of("time_slow_freeze");

	/** {@code RaidenLifecycleController.clearOnUntransform}: untransform takes Yamato back
	 * and strips the burst attribute modifiers. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void raidenHeroClearStripsSwordAndBurst(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RaidenHero.ID);
		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato given");
		AbilityScopedModifiers.RAIDEN_BURST.apply(player);
		helper.assertTrue(TestPlayers.count(player, ModItems.MUSOU_NO_HITOTACHI) > 0,
				"Yamato sits in the inventory");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(AbilityScopedModifiers.RAIDEN_BURST_DAMAGE) != null,
				"burst damage modifier applied");

		HeroTransformService.forceUntransform(player);

		helper.assertTrue(TestPlayers.count(player, ModItems.MUSOU_NO_HITOTACHI) == 0,
				"untransform takes Yamato back");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(AbilityScopedModifiers.RAIDEN_BURST_DAMAGE) == null,
				"burst modifiers stripped");
		helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED)
						.getModifier(AbilityScopedModifiers.RAIDEN_BURST_SPEED) == null,
				"burst speed modifier stripped");
		helper.assertTrue(RaidenState.EMPTY.equals(
						player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE)),
				"RaidenState reset to EMPTY");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/** {@code ReinhardController.clearAdaptations}: untransform resets the adaptation
	 * attachment, strips phase/draw/second-coming modifiers and revokes the bound sword. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardHeroClearResetsAdaptationsAndDraw(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		ReinhardState armed = player.getAttachedOrCreate(ReinhardAttachments.STATE)
				.withPhase(3)
				.withAccumulatedDamage(100f)
				.withPhoenixUsed(true)
				.withInSecondComing(true)
				.withSwordDrawn(true)
				.withAdaptedDamageTypes(List.of("minecraft:generic"));
		player.setAttached(ReinhardAttachments.STATE, armed);
		ReinhardModifiers.buildReinhardPhaseSet(3).apply(player);
		ReinhardModifiers.REINHARD_DRAW.apply(player);
		ReinhardModifiers.REINHARD_SECOND_COMING.apply(player);
		helper.assertTrue(ReinhardSword.giveSword(player), "Royal Icicle given");

		HeroTransformService.forceUntransform(player);

		ReinhardState after = player.getAttachedOrCreate(ReinhardAttachments.STATE);
		helper.assertTrue(after.phase() == 1 && after.accumulatedDamage() == 0f
						&& !after.phoenixUsed() && !after.inSecondComing() && !after.swordDrawn()
						&& after.adaptedDamageTypes().isEmpty() && after.recentDamageTypes().isEmpty(),
				"hero clear resets the Reinhard attachment");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(ReinhardModifiers.REINHARD_PHASE_DAMAGE) == null,
				"phase modifiers stripped");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(ReinhardModifiers.REINHARD_DRAW_DAMAGE) == null,
				"draw modifiers stripped");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(ReinhardModifiers.REINHARD_SECOND_COMING_DAMAGE) == null,
				"second-coming modifiers stripped");
		helper.assertTrue(TestPlayers.count(player, ReinhardItems.ROYAL_ICICLE) == 0,
				"bound sword revoked");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/** {@code ReinhardSwordDrawCeremonyController.cancelCeremony} on leave: the ceremony's
	 * NoAI locks and freeze effects on nearby mobs are released. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardCeremonyLeaveThawsFrozenMobs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		player.teleportTo(zombie.getX() + 2.0, zombie.getY(), zombie.getZ());
		// startCeremony scans the accessible entity sections — wait for the spawn to be visible.
		TestPlayers.awaitVisible(helper, zombie, () -> {
			helper.assertTrue(ReinhardSwordDrawCeremonyController.startCeremony(player),
					"ceremony started");
			helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
					.contains(player.getUUID()), "ceremony froze the mob (NoAI lock)");
			helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"ceremony applied the slowness effect");

			TestPlayers.leave(player);

			helper.assertFalse(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
					.contains(player.getUUID()), "leaving cancels the ceremony's NoAI ref");
			helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"leaving strips the freeze effects");
			helper.assertFalse(ReinhardSwordDrawCeremonyController.isInCeremony(player),
					"ceremony tracking dropped");
			helper.succeed();
		});
	}

	/** Same hook on death — Reinhard dies for real only once the phoenix revive is spent. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardCeremonyDeathThawsFrozenMobs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		player.setAttached(ReinhardAttachments.STATE,
				player.getAttachedOrCreate(ReinhardAttachments.STATE).withPhoenixUsed(true));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		player.teleportTo(zombie.getX() + 2.0, zombie.getY(), zombie.getZ());
		// See the leave variant: the ceremony's area scan needs the spawn entity-visible.
		TestPlayers.awaitVisible(helper, zombie, () -> {
			helper.assertTrue(ReinhardSwordDrawCeremonyController.startCeremony(player),
					"ceremony started");
			helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
					.contains(player.getUUID()), "ceremony froze the mob (NoAI lock)");

			player.kill();

			helper.assertFalse(player.isAlive(), "phoenix spent — the hit kills for real");
			helper.assertFalse(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
					.contains(player.getUUID()), "dying cancels the ceremony's NoAI ref");
			helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"dying strips the freeze effects");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/** {@code ReinhardTimeSlowController.onPlayerGone}: the owner leaving releases every
	 * entity control lock of the active slow; a frozen victim leaving sheds the freeze
	 * attribute modifiers. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardTimeSlowLeaveReleasesLocksAndModifiers(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		TestHeroes.transform(owner, ReinhardHero.ID);
		ServerPlayer victim = TestPlayers.join(helper, "frozen-victim");
		// ServerPlayer.canHarmPlayer gates the player-freeze path on isPvpAllowed(); the
		// gametest server runs with pvp off, so enable it for this test only and
		// restore it — the flag is global to every concurrent test in the batch.
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.getLevel().getServer().setPvpAllowed(true);
		Zombie frozen = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		owner.teleportTo(frozen.getX() - 3.0, frozen.getY(), frozen.getZ());
		victim.teleportTo(frozen.getX() + 3.0, frozen.getY(), frozen.getZ());
		ReinhardTimeSlowController.triggerAbilitySlow(owner);

		// Freshly joined players and spawned mobs only enter the accessible entity
		// sections freezeAround() scans once their chunk's tracking upgrade lands —
		// poll for both, then settle.
		TestPlayers.awaitVisible(helper, victim, () -> TestPlayers.awaitVisible(helper, frozen, () -> helper.runAfterDelay(2, () -> {
			helper.assertTrue(TestPlayers.lockOwners(frozen, ControlLockKind.NO_AI)
					.contains(owner.getUUID()), "time slow holds a NoAI lock on the mob");
			helper.assertTrue(victim.getAttribute(Attributes.MOVEMENT_SPEED)
							.getModifier(TIME_SLOW_FREEZE) != null,
					"frozen players get the zero-speed modifier");
			helper.getLevel().getServer().setPvpAllowed(oldPvp);

			TestPlayers.leave(victim);
			helper.assertTrue(victim.getAttribute(Attributes.MOVEMENT_SPEED)
							.getModifier(TIME_SLOW_FREEZE) == null,
					"victim leaving strips the freeze modifiers");

			TestPlayers.leave(owner);
			helper.assertFalse(TestPlayers.lockOwners(frozen, ControlLockKind.NO_AI)
					.contains(owner.getUUID()), "owner leaving releases the lock ref");
			helper.assertFalse(ReinhardTimeSlowController.isActive(owner),
					"owner's slow dropped");
			helper.succeed();
		})));
	}

	/** {@code ReinhardTimeSlowController.resetAll}: world shutdown releases every lock and
	 * freeze modifier of every active slow. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardTimeSlowResetAllReleasesEverything(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		TestHeroes.transform(owner, ReinhardHero.ID);
		Zombie frozen = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		owner.teleportTo(frozen.getX() - 3.0, frozen.getY(), frozen.getZ());
		ReinhardTimeSlowController.triggerAbilitySlow(owner);

		// See the leave variant: wait for the spawn to be entity-visible first.
		TestPlayers.awaitVisible(helper, frozen, () -> helper.runAfterDelay(2, () -> {
			helper.assertTrue(TestPlayers.lockOwners(frozen, ControlLockKind.NO_AI)
					.contains(owner.getUUID()), "time slow holds a NoAI lock on the mob");
			ReinhardTimeSlowController.resetAll(helper.getLevel().getServer());
			helper.assertFalse(TestPlayers.lockOwners(frozen, ControlLockKind.NO_AI)
					.contains(owner.getUUID()), "resetAll releases the lock ref");
			helper.assertFalse(ReinhardTimeSlowController.isActive(owner),
					"resetAll drops the active slow");
			TestPlayers.leave(owner);
			helper.succeed();
		}));
	}

	/** {@code KratosRageController} tracking maps are OwnedSessionMap now: death drops the
	 * rage meter (reads the same as the old 0f sentinel). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void kratosRageDropsOnDeath(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, KratosHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		Zombie attacker = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		player.hurt(helper.getLevel().damageSources().mobAttack(attacker), 10f);
		helper.assertTrue(KratosRageController.getRage(player) > 0f,
				"taken damage builds rage");

		player.kill();

		helper.assertFalse(player.isAlive(), "the hit kills");
		helper.assertTrue(KratosRageController.getRage(player) == 0f,
				"death drops the rage meter");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
