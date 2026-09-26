package com.example.superheroes.gametest;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.RaidenSwordDrawAbility;
import com.example.superheroes.ability.ReinhardSwordDrawAbility;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.KratosRageController;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.effect.ReinhardSwordDrawCeremonyController;
import com.example.superheroes.effect.ReinhardTimeSlowController;
import com.example.superheroes.hero.AbilityScopedModifiers;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.transform.HeroTransformService;
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
		ReinhardState armed = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE)
				.withPhase(3)
				.withAccumulatedDamage(100f)
				.withPhoenixUsed(true)
				.withInSecondComing(true)
				.withSwordDrawn(true)
				.withAdaptedDamageTypes(List.of("minecraft:generic"));
		player.setAttached(ModAttachments.REINHARD_STATE, armed);
		AbilityScopedModifiers.buildReinhardPhaseSet(3).apply(player);
		AbilityScopedModifiers.REINHARD_DRAW.apply(player);
		AbilityScopedModifiers.REINHARD_SECOND_COMING.apply(player);
		helper.assertTrue(ReinhardSwordDrawAbility.giveSword(player), "Royal Icicle given");

		HeroTransformService.forceUntransform(player);

		ReinhardState after = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		helper.assertTrue(after.phase() == 1 && after.accumulatedDamage() == 0f
						&& !after.phoenixUsed() && !after.inSecondComing() && !after.swordDrawn()
						&& after.adaptedDamageTypes().isEmpty() && after.recentDamageTypes().isEmpty(),
				"hero clear resets the Reinhard attachment");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(AbilityScopedModifiers.REINHARD_PHASE_DAMAGE) == null,
				"phase modifiers stripped");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(AbilityScopedModifiers.REINHARD_DRAW_DAMAGE) == null,
				"draw modifiers stripped");
		helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
						.getModifier(AbilityScopedModifiers.REINHARD_SECOND_COMING_DAMAGE) == null,
				"second-coming modifiers stripped");
		helper.assertTrue(TestPlayers.count(player, ModItems.ROYAL_ICICLE) == 0,
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
		helper.assertTrue(ReinhardSwordDrawCeremonyController.startCeremony(player),
				"ceremony started");
		helper.assertTrue(zombie.isNoAi(), "ceremony froze the mob (NoAI lock)");
		helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
				"ceremony applied the slowness effect");

		TestPlayers.leave(player);

		helper.assertFalse(zombie.isNoAi(), "leaving cancels the ceremony and releases NoAI");
		helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
				"leaving strips the freeze effects");
		helper.assertFalse(ReinhardSwordDrawCeremonyController.isInCeremony(player),
				"ceremony tracking dropped");
		helper.succeed();
	}

	/** Same hook on death — Reinhard dies for real only once the phoenix revive is spent. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardCeremonyDeathThawsFrozenMobs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		player.setAttached(ModAttachments.REINHARD_STATE,
				player.getAttachedOrCreate(ModAttachments.REINHARD_STATE).withPhoenixUsed(true));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		player.teleportTo(zombie.getX() + 2.0, zombie.getY(), zombie.getZ());
		helper.assertTrue(ReinhardSwordDrawCeremonyController.startCeremony(player),
				"ceremony started");
		helper.assertTrue(zombie.isNoAi(), "ceremony froze the mob (NoAI lock)");

		player.kill();

		helper.assertFalse(player.isAlive(), "phoenix spent — the hit kills for real");
		helper.assertFalse(zombie.isNoAi(), "dying cancels the ceremony and releases NoAI");
		helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
				"dying strips the freeze effects");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/** {@code ReinhardTimeSlowController.onPlayerGone}: the owner leaving releases every
	 * entity control lock of the active slow; a frozen victim leaving sheds the freeze
	 * attribute modifiers. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reinhardTimeSlowLeaveReleasesLocksAndModifiers(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		TestHeroes.transform(owner, ReinhardHero.ID);
		ServerPlayer victim = TestPlayers.join(helper, "frozen-victim");
		Zombie frozen = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		owner.teleportTo(frozen.getX() - 3.0, frozen.getY(), frozen.getZ());
		victim.teleportTo(frozen.getX() + 3.0, frozen.getY(), frozen.getZ());
		ReinhardTimeSlowController.triggerAbilitySlow(owner);

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(frozen.isNoAi(), "time slow holds a NoAI lock on the mob");
			helper.assertTrue(victim.getAttribute(Attributes.MOVEMENT_SPEED)
							.getModifier(TIME_SLOW_FREEZE) != null,
					"frozen players get the zero-speed modifier");

			TestPlayers.leave(victim);
			helper.assertTrue(victim.getAttribute(Attributes.MOVEMENT_SPEED)
							.getModifier(TIME_SLOW_FREEZE) == null,
					"victim leaving strips the freeze modifiers");

			TestPlayers.leave(owner);
			helper.assertFalse(frozen.isNoAi(), "owner leaving releases the entity locks");
			helper.assertFalse(ReinhardTimeSlowController.isActive(owner),
					"owner's slow dropped");
			helper.succeed();
		});
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

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(frozen.isNoAi(), "time slow holds a NoAI lock on the mob");
			ReinhardTimeSlowController.resetAll(helper.getLevel().getServer());
			helper.assertFalse(frozen.isNoAi(), "resetAll releases the entity locks");
			helper.assertFalse(ReinhardTimeSlowController.isActive(owner),
					"resetAll drops the active slow");
			TestPlayers.leave(owner);
			helper.succeed();
		});
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
