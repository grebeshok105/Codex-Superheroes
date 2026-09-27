package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.hero.goku.ability.GokuKamehamehaAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSpiritBombAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoOodamaRasenganAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoRasenganAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoRasenshurikenAbility;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.lifecycle.HeroTickDispatcher;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.hero.goku.runtime.GokuKiStackController;
import io.github.grebeshok105.codex.hero.naruto.runtime.KawarimiController;
import io.github.grebeshok105.codex.hero.naruto.entity.KageBunshinEntity;
import io.github.grebeshok105.codex.hero.goku.GokuAbilities;
import io.github.grebeshok105.codex.hero.goku.GokuHero;
import io.github.grebeshok105.codex.hero.naruto.NarutoAbilities;
import io.github.grebeshok105.codex.hero.naruto.NarutoHero;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

/**
 * Stage I2a characterization: the current (pre-move) behavior of the five charge
 * abilities — «charge N ticks → release», session lifecycle edges (death, leave,
 * untransform, re-activation denial) — plus the sage-mode / super-saiyan-aura
 * conditional ticks and the Goku ki-stack / Naruto kawarimi controllers.
 *
 * <p>Charge ticks are driven by calling {@code serverTick(player)} directly so the
 * whole sequence stays atomic inside one game tick; real dispatcher ticks only run
 * while a test waits for a fresh spawn to become entity-visible.
 */
public final class GokuNarutoChargeGameTests implements FabricGameTest {

	private static void grantEnergy(ServerPlayer player) {
		HeroDataStore.update(player, d -> d.withResources(1000f, 0f));
	}

	private static void faceForward(ServerPlayer player) {
		// yaw 0 / pitch 0 — view vector = +Z.
		player.setYRot(0f);
		player.setXRot(0f);
	}

	private static Zombie spawnZombieAhead(ServerPlayer player, double blocksAhead) {
		ServerLevel level = player.serverLevel();
		Zombie zombie = EntityType.ZOMBIE.create(level);
		if (zombie == null) {
			throw new IllegalStateException("could not create zombie");
		}
		zombie.setNoAi(true);
		Vec3 pos = player.position().add(player.getLookAngle().scale(blocksAhead));
		zombie.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
		level.addFreshEntity(zombie);
		return zombie;
	}

	// ---------- charge timings ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void kamehamehaBeamFiresAfterFortyChargeTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		Zombie target = spawnZombieAhead(player, 6.0);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, GokuAbilities.GOKU_KAMEHAMEHA);
			Ability kamehameha = AbilityRegistry.get(GokuAbilities.GOKU_KAMEHAMEHA);
			helper.assertTrue(!kamehameha.canActivate(player),
					"a charge session opens on activation");

			for (int i = 0; i < 40; i++) {
				GokuKamehamehaAbility.serverTick(player);
			}
			helper.assertTrue(target.getHealth() == target.getMaxHealth(),
					"40 charge ticks only charge — no beam damage yet");
			GokuKamehamehaAbility.serverTick(player);
			helper.assertTrue(target.getHealth() < target.getMaxHealth(),
					"tick 41 is the first beam tick");
			for (int i = 0; i < 29; i++) {
				GokuKamehamehaAbility.serverTick(player);
			}
			helper.assertTrue(kamehameha.canActivate(player),
					"the session is gone after 40 charge + 30 beam ticks");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spiritBombDetonatesAfterEightyChannelTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		// Blast center = origin + look*8 (+1.2y), radius 12 — 6 blocks ahead is inside.
		Zombie target = spawnZombieAhead(player, 6.0);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, GokuAbilities.GOKU_SPIRIT_BOMB);
			Ability spiritBomb = AbilityRegistry.get(GokuAbilities.GOKU_SPIRIT_BOMB);
			helper.assertTrue(!spiritBomb.canActivate(player), "channel session opened");

			for (int i = 0; i < 79; i++) {
				GokuSpiritBombAbility.serverTick(player);
			}
			helper.assertTrue(target.isAlive(), "channeling for 79 ticks does not detonate");
			GokuSpiritBombAbility.serverTick(player);
			helper.assertTrue(target.getHealth() < target.getMaxHealth() || !target.isAlive(),
					"tick 80 detonates the spirit bomb");
			helper.assertTrue(spiritBomb.canActivate(player),
					"the session closes on detonation");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rasenganStrikesAfterThirtyChargeTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		Zombie target = spawnZombieAhead(player, 2.5);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_RASENGAN);
			Ability rasengan = AbilityRegistry.get(NarutoAbilities.NARUTO_RASENGAN);

			for (int i = 0; i < 30; i++) {
				NarutoRasenganAbility.serverTick(player);
			}
			helper.assertTrue(target.getHealth() == target.getMaxHealth(),
					"30 charge ticks — no strike yet");
			NarutoRasenganAbility.serverTick(player);
			helper.assertTrue(target.getHealth() < target.getMaxHealth() || !target.isAlive(),
					"the first window tick strikes a hostile in reach");
			helper.assertTrue(!rasengan.canActivate(player),
					"a detonated rasengan still holds the session until the window ends");
			for (int i = 0; i < 99; i++) {
				NarutoRasenganAbility.serverTick(player);
			}
			helper.assertTrue(rasengan.canActivate(player),
					"the session expires after 30 charge + 100 window ticks");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oodamaRasenganStrikesAfterFiftyChargeTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		Zombie target = spawnZombieAhead(player, 4.0);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_OODAMA_RASENGAN);

			for (int i = 0; i < 50; i++) {
				NarutoOodamaRasenganAbility.serverTick(player);
			}
			helper.assertTrue(target.getHealth() == target.getMaxHealth(),
					"50 charge ticks — no strike yet");
			NarutoOodamaRasenganAbility.serverTick(player);
			helper.assertTrue(target.getHealth() < target.getMaxHealth() || !target.isAlive(),
					"the first window tick strikes a hostile in reach");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rasenshurikenLaunchesAfterTwentyFourChargeTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		// A guaranteed victim on the projectile path (the session closes on any hit).
		spawnZombieAhead(player, 10.0);
		Ability rasenshuriken = AbilityRegistry.get(NarutoAbilities.NARUTO_RASENSHURIKEN);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_RASENSHURIKEN);

			for (int i = 0; i < 24; i++) {
				NarutoRasenshurikenAbility.serverTick(player);
			}
			helper.assertTrue(!rasenshuriken.canActivate(player),
					"the charge session is still live after the 24-tick charge");
			for (int i = 0; i < 16; i++) {
				NarutoRasenshurikenAbility.serverTick(player);
			}
			helper.assertTrue(rasenshuriken.canActivate(player),
					"the session closes once the launched projectile reaches a hostile");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---------- session lifecycle edges ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void chargeSessionDropsOnDeath(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, GokuAbilities.GOKU_KAMEHAMEHA);
			Ability kamehameha = AbilityRegistry.get(GokuAbilities.GOKU_KAMEHAMEHA);
			helper.assertTrue(!kamehameha.canActivate(player), "session active before death");

			player.kill();
			helper.assertTrue(kamehameha.canActivate(player),
					"death clears the in-progress charge (OwnedSessionMap ClearOn.DEATH)");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chargeSessionDropsOnLeave(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_RASENGAN);
			helper.assertTrue(!AbilityRegistry.get(NarutoAbilities.NARUTO_RASENGAN).canActivate(player),
					"session active before leave");

			TestPlayers.leave(player);
			ServerPlayer rejoined = TestPlayers.rejoin(helper, player);
			helper.assertTrue(AbilityRegistry.get(NarutoAbilities.NARUTO_RASENGAN).canActivate(rejoined),
					"leave clears the in-progress charge (OwnedSessionMap ClearOn.LEAVE)");

			TestPlayers.leave(rejoined);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chargeSessionSurvivesUntransform(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, GokuAbilities.GOKU_KAMEHAMEHA);
			Ability kamehameha = AbilityRegistry.get(GokuAbilities.GOKU_KAMEHAMEHA);

			helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
			helper.assertTrue(!kamehameha.canActivate(player),
					"HERO_CLEAR is not in the session's clear policy — the charge survives untransform");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void reactivationDeniedWhileSessionLives(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_RASENGAN);
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, NarutoAbilities.NARUTO_RASENGAN),
					"activation sets the cooldown immediately");

			float energyAfterFirst = HeroDataStore.get(player).energy();
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_RASENGAN);
			helper.assertTrue(HeroDataStore.get(player).energy() == energyAfterFirst,
					"re-activation while a session lives is a free no-op (canActivate/cooldown)");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void foreignHeroAbilityDoesNotStart(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			float energy = HeroDataStore.get(player).energy();
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_RASENGAN);
			helper.assertTrue(HeroDataStore.get(player).energy() == energy,
					"a Goku player cannot start Naruto's rasengan (hero ability list gate)");
			helper.assertTrue(AbilityRegistry.get(NarutoAbilities.NARUTO_RASENGAN).canActivate(player),
					"no session was created");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---------- conditional ticks: sage mode / super saiyan aura ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void sageModeTickRefreshesRegenEveryFortyTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);
		grantEnergy(player);
		AbilityRouter.activate(player, NarutoAbilities.NARUTO_SAGE_MODE);
		helper.assertTrue(HeroDataStore.get(player).isActive(NarutoAbilities.NARUTO_SAGE_MODE),
				"sage mode is a toggle — activation marks it active");
		player.removeEffect(MobEffects.REGENERATION);

		var server = helper.getLevel().getServer();
		player.tickCount = 39;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(!player.hasEffect(MobEffects.REGENERATION),
				"no regen refresh on a non-multiple-of-40 tick");
		player.tickCount = 40;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
				"the conditional tick refreshes regen on every 40th player tick");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sageModeDeactivateAppliesExitEffects(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);
		grantEnergy(player);
		AbilityRouter.activate(player, NarutoAbilities.NARUTO_SAGE_MODE);
		AbilityRouter.deactivate(player, NarutoAbilities.NARUTO_SAGE_MODE);

		helper.assertTrue(!HeroDataStore.get(player).isActive(NarutoAbilities.NARUTO_SAGE_MODE),
				"deactivation clears the active flag");
		helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS)
						&& player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
				"exiting sage mode applies weakness + slowdown");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, NarutoAbilities.NARUTO_SAGE_MODE),
				"exiting sage mode starts a 200-tick cooldown");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void superSaiyanAuraTickRefreshesEffectsEveryFortyTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, GokuHero.ID);
		grantEnergy(player);
		AbilityRouter.activate(player, GokuAbilities.GOKU_SUPER_SAIYAN_AURA);
		helper.assertTrue(HeroDataStore.get(player).isActive(GokuAbilities.GOKU_SUPER_SAIYAN_AURA),
				"the aura is a toggle");
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		player.removeEffect(MobEffects.GLOWING);

		var server = helper.getLevel().getServer();
		player.tickCount = 41;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(!player.hasEffect(MobEffects.FIRE_RESISTANCE),
				"no refresh off the 40-tick cadence");
		player.tickCount = 40;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE)
						&& player.hasEffect(MobEffects.GLOWING),
				"the conditional tick re-applies fire resistance + glowing");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void superSaiyanAuraDeactivateDropsGlowingOnly(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, GokuHero.ID);
		grantEnergy(player);
		AbilityRouter.activate(player, GokuAbilities.GOKU_SUPER_SAIYAN_AURA);
		AbilityRouter.deactivate(player, GokuAbilities.GOKU_SUPER_SAIYAN_AURA);

		helper.assertTrue(!player.hasEffect(MobEffects.GLOWING),
				"deactivation removes glowing");
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE),
				"fire resistance outlives deactivation — current behavior, characterized as-is");

		TestPlayers.leave(player);
		helper.succeed();
	}

	// ---------- goku ki stacks / naruto kawarimi ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void kiChargeStacksEveryTwentyTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, GokuHero.ID);
		grantEnergy(player);
		AbilityRouter.activate(player, GokuAbilities.GOKU_KI_CHARGE);
		var server = helper.getLevel().getServer();

		helper.assertTrue(GokuKiStackController.getStacks(player) == 0, "no stacks before ticking");
		player.tickCount = 19;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(GokuKiStackController.getStacks(player) == 0,
				"no stack on a non-multiple-of-20 tick");
		player.tickCount = 20;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(GokuKiStackController.getStacks(player) == 1,
				"one ki stack per 20 active ticks");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void kiResilienceNeedsThreeStacks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, GokuHero.ID);
		var server = helper.getLevel().getServer();

		player.tickCount = 40;
		HeroTickDispatcher.tick(server);
		helper.assertTrue(!player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
				"no resistance without stacks");
		GokuKiStackController.addStack(player);
		GokuKiStackController.addStack(player);
		GokuKiStackController.addStack(player);
		HeroTickDispatcher.tick(server);
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
				"3 ki stacks + a 40-tick boundary grant damage resistance");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void kamehamehaConsumesKiStacks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, GokuHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			GokuKiStackController.addStack(player);
			GokuKiStackController.addStack(player);
			GokuKiStackController.addStack(player);
			AbilityRouter.activate(player, GokuAbilities.GOKU_KAMEHAMEHA);
			helper.assertTrue(GokuKiStackController.getStacks(player) == 0,
					"kamehameha consumes all ki stacks for the damage multiplier");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void kawarimiCooldownBlocksSecondSubstitution(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);
		Zombie attacker = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		// Mock players join with spawnInvulnerableTime=60 that blocks any hurt()
		// not in #bypasses_invulnerability — see DamagePipelineGameTests.
		try {
			java.lang.reflect.Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			f.setAccessible(true);
			f.setInt(player, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		player.invulnerableTime = 0;

		float maxHp = player.getMaxHealth();
		player.hurt(helper.getLevel().damageSources().mobAttack(attacker), maxHp + 100f);
		helper.assertTrue(player.isAlive() && KawarimiController.getCooldownRemainingTicks(player) > 0,
				"first lethal hit triggers the substitution and starts the cooldown");

		player.invulnerableTime = 0;
		player.hurt(helper.getLevel().damageSources().mobAttack(attacker), maxHp + 100f);
		helper.assertTrue(!player.isAlive(),
				"a second lethal hit inside the 1200-tick cooldown kills");

		TestPlayers.leave(player);
		helper.succeed();
	}

	// ---------- shadow clones / instant abilities ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void shadowClonesSpawnFifteenAndRetarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		Zombie hunter = spawnZombieAhead(player, 5.0);
		hunter.setTarget(player);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_SHADOW_CLONES);
			var clones = helper.getLevel().getEntitiesOfClass(KageBunshinEntity.class,
					player.getBoundingBox().inflate(12.0));
			helper.assertTrue(clones.size() == 15,
					"shadow clones spawns 15 kage bunshin, got " + clones.size());
			helper.assertTrue(hunter.getTarget() instanceof KageBunshinEntity,
					"hostiles targeting the caster get retargeted onto a clone");
			helper.assertTrue(hunter.hasEffect(MobEffects.DARKNESS),
					"hostiles in range get darkness");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bijuudamaDamagesAtImpactPoint(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, NarutoHero.ID);
		// Impact = eye + look*32 (no block in the test structure); ±7 AoE box.
		Zombie target = spawnZombieAhead(player, 30.0);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, NarutoAbilities.NARUTO_BIJUUDAMA);
			helper.assertTrue(target.getHealth() < target.getMaxHealth() || !target.isAlive(),
					"bijuudama hits hostiles near the look-point impact");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, NarutoAbilities.NARUTO_BIJUUDAMA),
					"instant ability still lands on cooldown");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void instantTransmissionTeleportsForward(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			Vec3 origin = player.position();
			AbilityRouter.activate(player, GokuAbilities.GOKU_INSTANT_TRANSMISSION);
			// The blink lands 12 out only when no hostile is in range — shared-level
			// leftovers may instead route the activation into a strike behind them.
			helper.assertTrue(player.position().distanceTo(origin) > 1.0e-6,
					"activation repositions the player (a 12-block blink or a strike behind a hostile)");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, GokuAbilities.GOKU_INSTANT_TRANSMISSION),
					"teleport lands on cooldown");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void solarFlareBlindsHostilesInRadius(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		faceForward(player);
		TestHeroes.transform(player, GokuHero.ID);
		Zombie target = spawnZombieAhead(player, 5.0);

		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, GokuAbilities.GOKU_SOLAR_FLARE);
			helper.assertTrue(target.hasEffect(MobEffects.BLINDNESS),
					"solar flare blinds hostiles within 20 blocks");

			TestPlayers.leave(player);
			helper.succeed();
		});
	}
}
