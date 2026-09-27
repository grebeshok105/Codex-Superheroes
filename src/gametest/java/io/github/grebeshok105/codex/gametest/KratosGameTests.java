package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.effect.KratosRageController;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Characterization for the I2b module move, written against the pre-move layout:
 * Kratos's ability slot order, the Spartan Rage meter (builds from damage taken and
 * dealt, gates activation at full, drains out and auto-deactivates), session cleanup
 * on leave/untransform, and the four combat abilities' observable effects.
 * Ability ids are literals because the constants move from {@code AbilityIds} to the
 * module (KratosAbilities).
 */
public final class KratosGameTests implements FabricGameTest {
	private static final ResourceLocation SPARTAN_RAGE = ModId.of("kratos_spartan_rage");
	private static final ResourceLocation BLADE_STORM = ModId.of("kratos_blade_storm");
	private static final ResourceLocation CHAIN_WHIRL = ModId.of("kratos_chain_whirl");
	private static final ResourceLocation LEVIATHAN_THROW = ModId.of("kratos_leviathan_throw");
	private static final ResourceLocation GOD_SLAYER = ModId.of("kratos_god_slayer");

	@GameTest(template = EMPTY_STRUCTURE)
	public void kratosOwnsItsAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero kratos = Heroes.get(ModId.of("kratos"));
		helper.assertTrue(kratos != null, "kratos registered");
		helper.assertTrue(kratos.getAbilities().equals(List.of(
				SPARTAN_RAGE, BLADE_STORM, CHAIN_WHIRL, LEVIATHAN_THROW, GOD_SLAYER)),
				"slot order " + kratos.getAbilities());
		for (ResourceLocation id : kratos.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rageBuildsFromDamageTakenAndDealt(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		TestPlayers.clearSpawnInvulnerability(player);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);

		helper.assertTrue(KratosRageController.getRage(player) == 0f, "rage starts empty");
		player.hurt(player.damageSources().mobAttack(zombie), 10f);
		float afterTaken = KratosRageController.getRage(player);
		helper.assertTrue(afterTaken > 0f && afterTaken < 10f,
				"rage grows from damage taken (0.3/dmg), got " + afterTaken);

		// Dealt side: a big non-lethal hit (400 on a 500hp warden) must cap at MAX_RAGE.
		Warden warden = helper.spawn(EntityType.WARDEN, 4, 1, 1);
		helper.assertTrue(warden.hurt(helper.getLevel().damageSources().playerAttack(player), 400f),
				"setup: the dealt hit landed");
		helper.assertTrue(KratosRageController.getRage(player) == KratosRageController.MAX_RAGE,
				"rage caps at MAX_RAGE, got " + KratosRageController.getRage(player));

		warden.kill();
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spartanRageRefusesBelowFullRage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		TestPlayers.clearSpawnInvulnerability(player);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		player.hurt(player.damageSources().mobAttack(zombie), 10f);
		helper.assertTrue(KratosRageController.getRage(player) > 0f, "setup: some rage banked");

		AbilityRouter.activate(player, SPARTAN_RAGE);
		helper.assertFalse(KratosRageController.isActive(player), "rage not full — no activation");
		helper.assertFalse(HeroDataStore.get(player).isActive(SPARTAN_RAGE),
				"no active flag below full rage");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spartanRageActivationAppliesBuffsAndModifiers(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		fillRage(helper, player);

		AbilityRouter.activate(player, SPARTAN_RAGE);
		helper.assertTrue(KratosRageController.isActive(player), "rage session active");
		helper.assertTrue(HeroDataStore.get(player).isActive(SPARTAN_RAGE), "toggle flag set");
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST), "damage boost applied");
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "resistance applied");
		helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "absorption applied");
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "fire resistance applied");
		helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 20f,
				"rage modifiers applied, atk=" + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		helper.assertTrue(player.getHealth() == player.getMaxHealth(), "activation heals to full");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void spartanRageDrainsOutAndAutoDeactivates(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		fillRage(helper, player);
		AbilityRouter.activate(player, SPARTAN_RAGE);
		helper.assertTrue(KratosRageController.isActive(player), "setup: rage running");

		// 240-tick drain: after ~250 ticks the controller must have emptied the meter,
		// dropped the session and run the ability's deactivate path.
		helper.runAfterDelay(250, () -> {
			helper.assertFalse(KratosRageController.isActive(player), "rage session expired");
			helper.assertTrue(KratosRageController.getRage(player) == 0f, "rage drained to 0");
			helper.assertFalse(HeroDataStore.get(player).isActive(SPARTAN_RAGE),
					"drain auto-deactivates the toggle");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rageStateDiesWithLogout(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		fillRage(helper, player);
		AbilityRouter.activate(player, SPARTAN_RAGE);
		helper.assertTrue(KratosRageController.isActive(player), "setup: rage running");

		TestPlayers.leave(player);
		helper.assertFalse(KratosRageController.isActive(player), "logout cleared ACTIVE entry");
		helper.assertTrue(KratosRageController.getRage(player) == 0f, "logout cleared RAGE entry");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void untransformDropsActiveRage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		fillRage(helper, player);
		AbilityRouter.activate(player, SPARTAN_RAGE);
		helper.assertTrue(KratosRageController.isActive(player), "setup: rage running");

		HeroTransformService.forceUntransform(player);
		// The per-tick sweep drops non-kratos ACTIVE entries and zeroes the meter.
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(KratosRageController.isActive(player), "untransform dropped the session");
			helper.assertTrue(KratosRageController.getRage(player) == 0f, "rage meter zeroed");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void leviathanThrowKillsPrimaryAndSplashesNeighbors(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		Zombie primary = helper.spawn(EntityType.ZOMBIE, 4, 1, 4);
		Zombie neighbor = helper.spawn(EntityType.ZOMBIE, 4, 1, 6);

		// Face +X at the pair: player 8 blocks behind the primary.
		player.teleportTo(primary.getX() - 8.0, primary.getY(), primary.getZ());
		player.setYRot(-90.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(-90.0F);
		player.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, neighbor, () -> {
			// Pin positions after the visibility wait — zombies wander during it.
			Vec3 p = player.position();
			primary.teleportTo(p.x + 8.0, p.y, p.z);
			neighbor.teleportTo(p.x + 8.0, p.y, p.z + 2.0);

			AbilityRouter.activate(player, LEVIATHAN_THROW);
			helper.assertFalse(primary.isAlive(), "36 damage kills a 20hp zombie");
			helper.assertTrue(neighbor.getHealth() < neighbor.getMaxHealth(),
					"splash damage hit the neighbor (16)");
			helper.assertTrue(neighbor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"splash applies slowness 80t");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, LEVIATHAN_THROW),
					"240t cooldown armed");
			helper.assertTrue(HeroDataStore.get(player).energy() <= 100f,
					"150 energy spent, energy=" + HeroDataStore.get(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bladeStormHitsHarderAtCenterAndIgnites(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		Zombie near = helper.spawn(EntityType.ZOMBIE, 4, 1, 4);
		Zombie edge = helper.spawn(EntityType.ZOMBIE, 4, 1, 4);

		TestPlayers.awaitVisible(helper, edge, () -> {
			Vec3 center = player.position();
			near.teleportTo(center.x + 1.0, center.y, center.z);
			edge.teleportTo(center.x + 5.5, center.y, center.z);

			AbilityRouter.activate(player, BLADE_STORM);
			helper.assertFalse(near.isAlive(), "~23 damage at 1 block kills a 20hp zombie");
			helper.assertTrue(edge.isAlive(), "~11 damage at 5.5 blocks leaves the edge zombie alive");
			helper.assertTrue(edge.isOnFire(), "hit ignites for 4s");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, BLADE_STORM),
					"120t cooldown armed");
			helper.assertTrue(HeroDataStore.get(player).energy() <= 150f,
					"100 energy spent, energy=" + HeroDataStore.get(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void chainWhirlTicksDamageWhileToggled(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 4, 1, 4);
		player.teleportTo(zombie.getX() - 2.0, zombie.getY(), zombie.getZ());

		AbilityRouter.activate(player, CHAIN_WHIRL);
		helper.assertTrue(HeroDataStore.get(player).isActive(CHAIN_WHIRL), "whirl toggled on");
		helper.assertTrue(HeroDataStore.get(player).energy() <= 220f,
				"30 energy spent on activation");

		// AoE ticks every 5th tick (9 dmg, ignite, weakness within r=4).
		TestPlayers.awaitVisible(helper, zombie, () -> {
			zombie.teleportTo(player.getX() + 2.0, player.getY(), player.getZ());
			helper.runAfterDelay(12, () -> {
				helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < zombie.getMaxHealth(),
						"whirl ticks damaged the zombie");
				if (zombie.isAlive()) {
					helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS),
							"whirl tick applies weakness 60t");
				}
				// Toggle semantics: a second activation switches it back off.
				AbilityRouter.activate(player, CHAIN_WHIRL);
				helper.assertFalse(HeroDataStore.get(player).isActive(CHAIN_WHIRL),
						"re-activation toggles off");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void godSlayerFailsWithoutTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		Vec3 before = player.position();
		float energyBefore = HeroDataStore.get(player).energy();

		// No hostile in the 16-block scan: tryActivate returns false, router refunds
		// the cost and leaves no cooldown or active flag.
		AbilityRouter.activate(player, GOD_SLAYER);
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, GOD_SLAYER), "no cooldown on fail");
		helper.assertFalse(HeroDataStore.get(player).isActive(GOD_SLAYER), "no active flag on fail");
		helper.assertTrue(player.position().equals(before), "player did not teleport");
		helper.assertTrue(HeroDataStore.get(player).energy() == energyBefore,
				"200 energy refunded on fail");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void godSlayerTeleportsAndKillsNearestHostile(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kratos"));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 4, 1, 4);
		Vec3 before = player.position();

		TestPlayers.awaitVisible(helper, zombie, () -> {
			zombie.teleportTo(before.x + 4.0, before.y, before.z);

			AbilityRouter.activate(player, GOD_SLAYER);
			helper.assertFalse(zombie.isAlive(), "5x12 damage kills a 20hp zombie");
			helper.assertTrue(player.position().distanceTo(before) > 1.0,
					"teleported behind the target");
			helper.assertTrue(player.position().distanceTo(zombie.position()) < 4.0,
					"landed next to the target");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, GOD_SLAYER),
					"900t cooldown armed");
			helper.assertTrue(HeroDataStore.get(player).energy() <= 50f,
					"200 energy spent, energy=" + HeroDataStore.get(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * Banks a full rage meter: one non-lethal 400-damage player-attack hit on a warden
	 * (500hp) adds dealt*0.75 = 300, capped at MAX_RAGE. The warden is killed
	 * afterwards so it cannot wander into the rest of the test.
	 */
	private static void fillRage(GameTestHelper helper, ServerPlayer player) {
		Warden warden = helper.spawn(EntityType.WARDEN, 4, 1, 1);
		helper.assertTrue(warden.hurt(helper.getLevel().damageSources().playerAttack(player), 400f),
				"setup: the dealt hit landed");
		warden.kill();
		helper.assertTrue(KratosRageController.getRage(player) == KratosRageController.MAX_RAGE,
				"setup: rage meter full");
	}
}
