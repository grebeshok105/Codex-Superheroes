package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.effect.BattleBeastCurseController;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Pins for the I1c Battle Beast module move: written against the pre-move layout and must
 * pass identically after {@code hero/battlebeast/} absorbs the server side. Ability ids are
 * literals because the constants move from {@code AbilityIds} to the module.
 */
public final class BattleBeastGameTests implements FabricGameTest {
	private static final ResourceLocation BATTLE_BEAST = ModId.of("battle_beast");
	private static final ResourceLocation PREDATOR_LEAP = ModId.of("battle_beast_predator_leap");
	private static final ResourceLocation AXE_CLEAVE = ModId.of("battle_beast_axe_cleave");
	private static final ResourceLocation WAR_ROAR = ModId.of("battle_beast_war_roar");
	private static final ResourceLocation BLOODLUST = ModId.of("battle_beast_bloodlust");

	@GameTest(template = EMPTY_STRUCTURE)
	public void battleBeastOwnsExactlyItsFourAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero hero = Heroes.get(BATTLE_BEAST);
		helper.assertTrue(hero != null, "battle_beast registered");
		helper.assertTrue(hero.getAbilities().equals(List.of(PREDATOR_LEAP, AXE_CLEAVE,
				WAR_ROAR, BLOODLUST)), "slot order " + hero.getAbilities());
		helper.assertTrue(hero.getImpactStyle() == ImpactStyle.BRUTAL,
				"battle_beast keeps the BRUTAL impact style, got " + hero.getImpactStyle());
		helper.assertTrue(hero.getThreatClass() == JarvisThreatClass.S,
				"battle_beast keeps the S threat class, got " + hero.getThreatClass());
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void medallionIsRegisteredAsTransformationItem(GameTestHelper helper) {
		Item item = BuiltInRegistries.ITEM.get(ModId.of("battle_beast_medallion"));
		helper.assertTrue(item instanceof TransformationItem,
				"battle_beast_medallion registered as TransformationItem, got " + item);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void curseStageAddsModifiersAndScalesDamage(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbcurse");
		TestHeroes.transform(bb, BATTLE_BEAST);
		double baseArmor = bb.getAttributeValue(Attributes.ARMOR);
		helper.assertTrue(baseArmor > 0.0, "base passives applied, armor " + baseArmor);

		BattleBeastCurseController.setStage(bb, 5);

		double cursedArmor = bb.getAttributeValue(Attributes.ARMOR);
		helper.assertTrue(cursedArmor > baseArmor,
				"stage 5 curse adds armor: " + baseArmor + " -> " + cursedArmor);
		helper.assertTrue(Math.abs(BattleBeastCurseController.damageMultiplier(bb) - 2.88f) < 0.01f,
				"stage 5 damage multiplier is 2.88, got " + BattleBeastCurseController.damageMultiplier(bb));
		helper.assertTrue(Math.abs(BattleBeastCurseController.scaleDamage(bb, 100f) - 288f) < 0.5f,
				"stage 5 scales 100 -> 288, got " + BattleBeastCurseController.scaleDamage(bb, 100f));
		helper.assertTrue(bb.hasEffect(MobEffects.DAMAGE_BOOST), "stage >= 5 applies strength");
		TestPlayers.leave(bb);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void curseTickRecomputesStageFromStartTick(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbtick");
		TestHeroes.transform(bb, BATTLE_BEAST);
		BattleBeastCurseController.setStage(bb, 4);
		double cursedArmor = bb.getAttributeValue(Attributes.ARMOR);

		BattleBeastCurseController.tickPlayer(helper.getLevel().getServer(), bb, HeroDataStore.get(bb));

		helper.assertTrue(bb.getAttributeValue(Attributes.ARMOR) == cursedArmor,
				"tick recomputes the same stage, armor stays " + cursedArmor);
		helper.assertTrue(Math.abs(BattleBeastCurseController.damageMultiplier(bb) - 2.36f) < 0.01f,
				"stage 4 damage multiplier is 2.36, got " + BattleBeastCurseController.damageMultiplier(bb));
		TestPlayers.leave(bb);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void forceUntransformStripsCurseAndPassives(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbstrip");
		TestHeroes.transform(bb, BATTLE_BEAST);
		BattleBeastCurseController.setStage(bb, 5);
		helper.assertTrue(bb.getAttributeValue(Attributes.ARMOR) > 20.0, "curse armor applied");

		HeroTransformService.forceUntransform(bb);

		helper.assertTrue(bb.getAttributeValue(Attributes.ARMOR) == 0.0,
				"untransform strips passives + curse armor, got " + bb.getAttributeValue(Attributes.ARMOR));
		helper.assertFalse(bb.hasEffect(MobEffects.DAMAGE_RESISTANCE),
				"untransform strips the damage resistance passive");
		helper.assertTrue(BattleBeastCurseController.damageMultiplier(bb) == 1.0f,
				"untransform drops the stored curse stage");
		TestPlayers.leave(bb);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void relogClearsCurseStageKeepsBasePassives(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbrelog");
		TestHeroes.transform(bb, BATTLE_BEAST);
		BattleBeastCurseController.setStage(bb, 6);
		double cursedArmor = bb.getAttributeValue(Attributes.ARMOR);
		helper.assertTrue(BattleBeastCurseController.damageMultiplier(bb) > 1.0f,
				"curse stage 6 stored before relog");

		TestPlayers.leave(bb);
		ServerPlayer rejoined = TestPlayers.rejoin(helper, bb);

		helper.assertTrue(BATTLE_BEAST.equals(HeroDataStore.get(rejoined).heroId()),
				"rejoin keeps the battle_beast hero");
		double armorAfterRelog = rejoined.getAttributeValue(Attributes.ARMOR);
		helper.assertTrue(armorAfterRelog > 0.0 && armorAfterRelog < cursedArmor,
				"relog keeps base passives but strips the curse: " + armorAfterRelog);
		helper.assertTrue(BattleBeastCurseController.damageMultiplier(rejoined) == 1.0f,
				"relog clears the curse stage state");
		TestPlayers.leave(rejoined);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void battleBeastStageCommandAppliesCurse(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbstage");
		TestHeroes.transform(bb, BATTLE_BEAST);
		double baseArmor = bb.getAttributeValue(Attributes.ARMOR);
		MinecraftServer server = helper.getLevel().getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				"superheroes battle_beast stage bbstage 4");
		helper.assertTrue(bb.getAttributeValue(Attributes.ARMOR) > baseArmor,
				"/superheroes battle_beast stage applies the curse");
		double stage4Armor = bb.getAttributeValue(Attributes.ARMOR);

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				"superheroes battle_beast tier bbstage 2");
		helper.assertTrue(bb.getAttributeValue(Attributes.ARMOR) < stage4Armor,
				"tier alias re-sets the stage, armor " + bb.getAttributeValue(Attributes.ARMOR));
		helper.assertTrue(Math.abs(BattleBeastCurseController.damageMultiplier(bb) - 1.68f) < 0.01f,
				"tier 2 damage multiplier is 1.68, got " + BattleBeastCurseController.damageMultiplier(bb));
		TestPlayers.leave(bb);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void battleBeastStageCommandRejectsNonBattleBeast(GameTestHelper helper) {
		ServerPlayer other = TestPlayers.join(helper, "notbb");
		MinecraftServer server = helper.getLevel().getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				"superheroes battle_beast stage notbb 4");

		helper.assertTrue(other.getAttributeValue(Attributes.ARMOR) == 0.0,
				"no curse on a hero-less target");
		helper.assertTrue(BattleBeastCurseController.damageMultiplier(other) == 1.0f,
				"rejected target stores no curse stage");
		TestPlayers.leave(other);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void predatorLeapLaunchesAndDamagesAhead(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbleap");
		TestHeroes.transform(bb, BATTLE_BEAST);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		bb.teleportTo(zombie.getX() - 3.0, zombie.getY(), zombie.getZ());
		bb.setYRot(-90.0F);
		bb.setXRot(0.0F);
		bb.setYHeadRot(-90.0F);
		bb.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(bb, PREDATOR_LEAP);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(bb, PREDATOR_LEAP),
					"predator leap went on cooldown");
			helper.assertTrue(bb.getDeltaMovement().x > 1.0,
					"leap launches the player forward, got " + bb.getDeltaMovement().x);
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"leap damages the victim");
			helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"leap slows the victim");
			TestPlayers.leave(bb);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void axeCleaveDamagesConeAhead(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbcleave");
		TestHeroes.transform(bb, BATTLE_BEAST);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		bb.teleportTo(zombie.getX() - 3.0, zombie.getY(), zombie.getZ());
		bb.setYRot(-90.0F);
		bb.setXRot(0.0F);
		bb.setYHeadRot(-90.0F);
		bb.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(bb, AXE_CLEAVE);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(bb, AXE_CLEAVE),
					"axe cleave went on cooldown");
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"cleave damages the victim");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "cleave weakness applied");
			TestPlayers.leave(bb);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void warRoarDebuffsRadiusAroundPlayer(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbroar");
		TestHeroes.transform(bb, BATTLE_BEAST);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		bb.teleportTo(zombie.getX() - 3.0, zombie.getY(), zombie.getZ());

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(bb, WAR_ROAR);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(bb, WAR_ROAR),
					"war roar went on cooldown");
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"roar damages victims in radius");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "roar weakness applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "roar slowness applied");
			TestPlayers.leave(bb);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bloodlustHealsAndBuffs(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "bbblood");
		TestHeroes.transform(bb, BATTLE_BEAST);
		bb.setHealth(10.0f);

		AbilityRouter.activate(bb, BLOODLUST);

		helper.assertTrue(AbilityCooldowns.isOnCooldown(bb, BLOODLUST),
				"bloodlust went on cooldown");
		helper.assertTrue(bb.getHealth() > 19.0f, "bloodlust heals 12, got " + bb.getHealth());
		helper.assertTrue(bb.hasEffect(MobEffects.DAMAGE_BOOST), "bloodlust strength applied");
		helper.assertTrue(bb.hasEffect(MobEffects.REGENERATION), "bloodlust regeneration applied");
		helper.assertTrue(bb.getEffect(MobEffects.DAMAGE_RESISTANCE) != null
						&& bb.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 1,
				"bloodlust raises resistance above the amp-0 passive");
		TestPlayers.leave(bb);
		helper.succeed();
	}
}
