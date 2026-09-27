package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.effect.RamCompanionController;
import io.github.grebeshok105.codex.effect.RemDemonismController;
import io.github.grebeshok105.codex.entity.RamEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Characterization for the I4b module move, written against the pre-move layout:
 * Rem's ability slot order, the Oni-demonism meter (builds from damage taken and
 * dealt plus a passive trickle, gates activation at full, drains out and
 * auto-deactivates, survives a lethal hit once as a permanent session), the Ram
 * companion summon/dismiss/grief lifecycle, and the rem_oni_horn transform item.
 * Ability ids are literals because the constants move from {@code AbilityIds} to
 * the module ({@code RemAbilities}); items resolve through the registry because
 * the rows move from {@code ModItems} to {@code RemItems}.
 */
public final class RemGameTests implements FabricGameTest {
	private static final ResourceLocation REM = ModId.of("rem");
	private static final ResourceLocation HEALING_MAGIC = ModId.of("rem_healing_magic");
	private static final ResourceLocation ICE_BURST = ModId.of("rem_ice_burst");
	private static final ResourceLocation ONI_RAGE = ModId.of("rem_oni_rage");
	private static final ResourceLocation MORNING_STAR = ModId.of("rem_morning_star");
	private static final ResourceLocation MACE_CRATER = ModId.of("rem_mace_crater");
	private static final ResourceLocation ONI_KICK = ModId.of("rem_oni_kick");
	private static final ResourceLocation HUMA_ICE_SPIKES = ModId.of("rem_huma_ice_spikes");

	@GameTest(template = EMPTY_STRUCTURE)
	public void remOwnsItsAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero rem = Heroes.get(REM);
		helper.assertTrue(rem != null, "rem registered");
		helper.assertTrue(rem.getAbilities().equals(List.of(
				HEALING_MAGIC, ICE_BURST, ONI_RAGE, MORNING_STAR, MACE_CRATER, ONI_KICK, HUMA_ICE_SPIKES)),
				"slot order " + rem.getAbilities());
		for (ResourceLocation id : rem.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void demonismBuildsFromDamageTakenAndDealt(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		TestPlayers.clearSpawnInvulnerability(player);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);

		helper.assertTrue(RemDemonismController.getCharge(player) == 0f, "demonism starts empty");
		player.hurt(player.damageSources().mobAttack(zombie), 10f);
		float afterTaken = RemDemonismController.getCharge(player);
		helper.assertTrue(afterTaken > 0f && afterTaken < 10f,
				"charge grows from damage taken (0.55/dmg), got " + afterTaken);

		// Dealt side: a big non-lethal hit (400 on a 500hp warden) must cap at MAX_DEMONISM.
		Warden warden = helper.spawn(EntityType.WARDEN, 4, 1, 1);
		helper.assertTrue(warden.hurt(helper.getLevel().damageSources().playerAttack(player), 400f),
				"setup: the dealt hit landed");
		helper.assertTrue(RemDemonismController.getCharge(player) == RemDemonismController.MAX_DEMONISM,
				"charge caps at MAX_DEMONISM, got " + RemDemonismController.getCharge(player));

		warden.kill();
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void demonismTricklesUpPassivelyWhileInactive(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		helper.assertTrue(RemDemonismController.getCharge(player) == 0f, "demonism starts empty");

		// Inactive demonism banks 0.45/s: over ~2 seconds some charge must appear
		// without a single hit taken or dealt.
		helper.runAfterDelay(45, () -> {
			float charge = RemDemonismController.getCharge(player);
			helper.assertTrue(charge > 0f && charge < 10f,
					"passive trickle banked charge, got " + charge);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oniRageRefusesBelowFullCharge(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		TestPlayers.clearSpawnInvulnerability(player);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		player.hurt(player.damageSources().mobAttack(zombie), 10f);
		helper.assertTrue(RemDemonismController.getCharge(player) > 0f, "setup: some charge banked");

		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertFalse(RemDemonismController.isActive(player), "charge not full — no activation");
		helper.assertFalse(HeroDataStore.get(player).isActive(ONI_RAGE),
				"no active flag below full charge");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oniRageActivationAppliesBuffsAndMace(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);

		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "demonism session active");
		helper.assertTrue(HeroDataStore.get(player).isActive(ONI_RAGE), "toggle flag set");
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST), "damage boost applied");
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "resistance applied");
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "speed applied");
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "fire resistance applied");
		helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "activation absorption applied");
		helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 6.5,
				"demon modifiers stack on passives, atk=" + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		helper.assertTrue(TestPlayers.count(player, morningStar()) == 1, "bound mace issued");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 620)
	public void oniRageDrainsOutAndAutoDeactivates(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "setup: demonism running");

		// 500-tick drain (100 / 0.2 per tick): after ~520 ticks the meter is empty,
		// the session is dropped and the toggle's active flag is cleared.
		helper.runAfterDelay(520, () -> {
			helper.assertFalse(RemDemonismController.isActive(player), "demonism session expired");
			helper.assertTrue(RemDemonismController.getCharge(player) == 0f, "meter drained to 0");
			helper.assertFalse(HeroDataStore.get(player).isActive(ONI_RAGE),
					"drain auto-deactivates the toggle");
			helper.assertTrue(TestPlayers.count(player, morningStar()) == 0,
					"drain revokes the bound mace");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oniRageToggleOffStripsSessionAndMace(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "setup: demonism running");

		// Toggling again runs the deactivate path: meter zeroed, mace revoked,
		// matching-type effects stripped, 160t exit cooldown armed.
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertFalse(RemDemonismController.isActive(player), "demonism stopped manually");
		helper.assertTrue(RemDemonismController.getCharge(player) == 0f, "meter zeroed on stop");
		helper.assertFalse(HeroDataStore.get(player).isActive(ONI_RAGE), "toggle flag cleared");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ONI_RAGE), "exit cooldown armed");
		helper.assertFalse(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "resistance stripped");
		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "speed stripped");
		helper.assertTrue(TestPlayers.count(player, morningStar()) == 0, "mace revoked");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void demonismStateDiesWithLogout(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "setup: demonism running");

		TestPlayers.leave(player);
		helper.assertFalse(RemDemonismController.isActive(player), "logout cleared ACTIVE entry");
		helper.assertTrue(RemDemonismController.getCharge(player) == 0f, "logout cleared CHARGE entry");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untransformDropsDemonismSession(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "setup: demonism running");

		HeroTransformService.forceUntransform(player);
		helper.assertFalse(RemDemonismController.isActive(player), "untransform dropped the session");
		helper.assertTrue(RemDemonismController.getCharge(player) == 0f, "meter zeroed");
		helper.assertFalse(HeroDataStore.get(player).isActive(ONI_RAGE), "toggle flag cleared");
		helper.assertTrue(TestPlayers.count(player, morningStar()) == 0, "mace revoked");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void lethalHitTriggersPermanentDemonism(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		TestPlayers.clearSpawnInvulnerability(player);

		player.kill();
		helper.assertTrue(player.isAlive(), "ALLOW_DEATH save denied the kill");
		helper.assertTrue(RemDemonismController.isActive(player), "death-save activates demonism");
		helper.assertTrue(RemDemonismController.isPermanent(player), "death-save marks it permanent");
		helper.assertTrue(RemDemonismController.getCharge(player) == RemDemonismController.MAX_DEMONISM,
				"meter pinned full");
		helper.assertTrue(HeroDataStore.get(player).isActive(ONI_RAGE),
				"toggle flag forced on by the save");
		helper.assertTrue(TestPlayers.count(player, morningStar()) == 1, "death-save issues the mace");
		helper.assertTrue(player.getHealth() == player.getMaxHealth(), "save heals to full");

		// Permanent demonism resists the manual stop: the toggle re-asserts itself
		// (the 160t exit cooldown still arms — pinned as-is).
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "permanent demonism resists stop");
		helper.assertTrue(RemDemonismController.isPermanent(player), "still permanent");
		helper.assertTrue(HeroDataStore.get(player).isActive(ONI_RAGE), "flag re-asserted");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ONI_RAGE),
				"exit cooldown arms even on the blocked stop");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void ramSummonsWithDemonismAndDismissesWithIt(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);
		helper.assertTrue(RemDemonismController.isActive(player), "setup: demonism running");

		// The companion controller only runs its %20 gate — allow the window plus
		// the section upgrade a fresh spawn needs before area scans see it.
		helper.runAfterDelay(45, () -> {
			List<RamEntity> rams = ownedRams(helper.getLevel(), player);
			helper.assertTrue(rams.size() == 1, "one ram summoned for the owner, got " + rams.size());
			helper.assertFalse(rams.get(0).shouldBeSaved(), "ram is a session summon — never saved");

			AbilityRouter.activate(player, ONI_RAGE);
			helper.assertFalse(RemDemonismController.isActive(player), "setup: demonism stopped");

			helper.runAfterDelay(30, () -> {
				helper.assertTrue(ownedRams(helper.getLevel(), player).isEmpty(),
						"ram dismissed once demonism ended");
				// Entity section-crossing flicker rule: absence must hold two ticks.
				helper.runAfterDelay(2, () -> {
					helper.assertTrue(ownedRams(helper.getLevel(), player).isEmpty(),
							"ram still gone a tick later");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void ramDeathGriefsOwnerAndBlocksResummon(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);
		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);

		helper.runAfterDelay(45, () -> {
			List<RamEntity> rams = ownedRams(helper.getLevel(), player);
			helper.assertTrue(rams.size() == 1, "setup: ram present");
			rams.get(0).kill();
			helper.assertFalse(rams.get(0).isAlive(), "ram died");
			helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "grief applies weakness");
			helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "grief applies slowness");

			// A fallen ram is never resummoned within the same demonism session.
			helper.runAfterDelay(40, () -> {
				helper.assertTrue(ownedRams(helper.getLevel(), player).isEmpty(),
						"no resummon while the session runs");
				helper.runAfterDelay(2, () -> {
					helper.assertTrue(ownedRams(helper.getLevel(), player).isEmpty(),
							"still none a tick later");
					RamCompanionController.clear(player.getUUID());
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void maceCraterRequiresActiveDemonism(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, REM);

		AbilityRouter.activate(player, MACE_CRATER);
		helper.assertFalse(RemDemonismController.isCraterWinding(player), "no windup without demonism");
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, MACE_CRATER), "denial arms no cooldown");

		fillDemonism(helper, player);
		AbilityRouter.activate(player, ONI_RAGE);
		AbilityRouter.activate(player, MACE_CRATER);
		helper.assertTrue(RemDemonismController.isCraterWinding(player), "windup armed inside demonism");

		// Hero clear drops the windup entry before the 30t impact can land.
		HeroTransformService.forceUntransform(player);
		helper.assertFalse(RemDemonismController.isCraterWinding(player), "clear drops the windup");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void oniHornTransformsAndShiftUseUntransforms(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		Item horn = BuiltInRegistries.ITEM.get(ModId.of("rem_oni_horn"));
		helper.assertTrue(horn != Items.AIR && horn instanceof TransformationItem,
				"rem_oni_horn registered as a transformation item");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(horn));
		horn.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(REM.equals(HeroDataStore.get(player).heroId()), "horn transforms into rem");

		// Transform cooldown is 20t — the shift-click untransform waits it out.
		helper.runAfterDelay(25, () -> {
			player.setShiftKeyDown(true);
			horn.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			player.setShiftKeyDown(false);
			helper.assertFalse(HeroDataStore.get(player).hasHero(), "shift-use untransforms");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/**
	 * Banks a full meter: one non-lethal 400-damage player-attack hit on a warden
	 * (500hp) adds dealt*0.85 = 340, capped at MAX_DEMONISM. The warden is killed
	 * afterwards so it cannot wander into the rest of the test.
	 */
	private static void fillDemonism(GameTestHelper helper, ServerPlayer player) {
		Warden warden = helper.spawn(EntityType.WARDEN, 4, 1, 1);
		helper.assertTrue(warden.hurt(helper.getLevel().damageSources().playerAttack(player), 400f),
				"setup: the dealt hit landed");
		warden.kill();
		helper.assertTrue(RemDemonismController.getCharge(player) == RemDemonismController.MAX_DEMONISM,
				"setup: demonism meter full");
	}

	private static Item morningStar() {
		return BuiltInRegistries.ITEM.get(ModId.of("rem_morning_star"));
	}

	private static List<RamEntity> ownedRams(ServerLevel level, ServerPlayer owner) {
		return level.getEntitiesOfClass(RamEntity.class, owner.getBoundingBox().inflate(64.0),
				ram -> ram.isAlive() && owner.getUUID().equals(ram.getOwnerId()));
	}
}
