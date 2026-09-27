package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

import java.util.List;

/**
 * Pins for the I1b A-Train module move: written against the pre-move layout and must
 * pass identically after {@code hero/atrain/} absorbs the server side. Ability ids are
 * literals because the constants move from {@code AbilityIds} to the module.
 */
public final class ATrainGameTests implements FabricGameTest {
	private static final ResourceLocation MACH_DASH = ModId.of("a_train_mach_dash");
	private static final ResourceLocation SONIC_BOOM = ModId.of("a_train_sonic_boom");
	private static final ResourceLocation HYPERSPEED = ModId.of("a_train_hyperspeed");
	private static final ResourceLocation ADRENALINE_RUSH = ModId.of("a_train_adrenaline_rush");

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTrainOwnsExactlyItsFourAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero atrain = Heroes.get(ModId.of("a_train"));
		helper.assertTrue(atrain != null, "a_train registered");
		helper.assertTrue(atrain.getAbilities().equals(List.of(MACH_DASH, SONIC_BOOM,
				HYPERSPEED, ADRENALINE_RUSH)), "slot order " + atrain.getAbilities());
		helper.assertTrue(atrain.getImpactStyle() == ImpactStyle.SPEED,
				"a_train keeps the BF11 SPEED impact style, got " + atrain.getImpactStyle());
		for (ResourceLocation id : atrain.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void hyperspeedToggleAppliesAndStripsSpeedBuffs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("a_train"));

		AbilityRouter.activate(player, HYPERSPEED);
		helper.assertTrue(HeroDataStore.get(player).isActive(HYPERSPEED), "hyperspeed went active");
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "hyperspeed speed applied");
		helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "hyperspeed haste applied");
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "hyperspeed resistance applied");

		AbilityRouter.deactivate(player, HYPERSPEED);
		helper.assertFalse(HeroDataStore.get(player).isActive(HYPERSPEED), "hyperspeed went inactive");
		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "speed removed");
		helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "haste removed");
		helper.assertFalse(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "resistance removed");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, HYPERSPEED),
				"deactivation starts the exit cooldown");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untransformStripsHyperspeedEffects(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("a_train"));

		AbilityRouter.activate(player, HYPERSPEED);
		helper.assertTrue(HeroDataStore.get(player).isActive(HYPERSPEED), "hyperspeed went active");
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "speed applied");

		HeroTransformService.forceUntransform(player);
		helper.assertFalse(HeroDataStore.get(player).isActive(HYPERSPEED),
				"untransform clears the active flag");
		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED),
				"removePassives stripped speed");
		helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED),
				"removePassives stripped haste");
		helper.assertFalse(player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
				"removePassives stripped resistance");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void machDashLaunchesAndDamagesAhead(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("a_train"));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		// Face the victim: three blocks behind it along -X, looking +X.
		player.teleportTo(zombie.getX() - 3.0, zombie.getY(), zombie.getZ());
		player.setYRot(-90.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(-90.0F);
		player.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, MACH_DASH);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, MACH_DASH),
					"mach dash went on cooldown");
			helper.assertTrue(player.getDeltaMovement().x > 2.0,
					"dash launches the player forward, got " + player.getDeltaMovement().x);
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"sweep damages the victim");
			helper.assertTrue(zombie.hasEffect(MobEffects.CONFUSION), "sweep confusion applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "sweep slowness applied");
			helper.assertTrue(zombie.getDeltaMovement().x > 1.0,
					"sweep knocks the victim forward, got " + zombie.getDeltaMovement().x);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sonicBoomHitsConeAhead(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("a_train"));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		player.teleportTo(zombie.getX() - 3.0, zombie.getY(), zombie.getZ());
		player.setYRot(-90.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(-90.0F);
		player.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, SONIC_BOOM);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, SONIC_BOOM),
					"sonic boom went on cooldown");
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"cone damages the victim");
			helper.assertTrue(zombie.hasEffect(MobEffects.CONFUSION), "boom confusion applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "boom weakness applied");
			helper.assertTrue(zombie.getDeltaMovement().x > 1.0,
					"boom knocks the victim forward, got " + zombie.getDeltaMovement().x);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void adrenalineRushHealsAndBuffs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("a_train"));
		player.setHealth(10.0f);
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 9999, 0));
		player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 9999, 0));

		AbilityRouter.activate(player, ADRENALINE_RUSH);

		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ADRENALINE_RUSH),
				"adrenaline rush went on cooldown");
		helper.assertTrue(player.getHealth() > 19.0f, "rush heals 10, got " + player.getHealth());
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "rush regeneration applied");
		helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "rush absorption applied");
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "rush speed applied");
		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "slowness cleansed");
		helper.assertFalse(player.hasEffect(MobEffects.CONFUSION), "confusion cleansed");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
