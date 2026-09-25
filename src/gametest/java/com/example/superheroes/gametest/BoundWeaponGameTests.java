package com.example.superheroes.gametest;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.item.bound.BoundWeapons;
import com.example.superheroes.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Audit B1 (no-drop recursion crash) and B7 (bound weapon duplication). */
public final class BoundWeaponGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void issuingIntoFullInventoryFailsCleanly(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		TestPlayers.fillInventory(owner);

		helper.assertFalse(BoundWeapons.ensureHeld(owner, ModItems.MUSOU_NO_HITOTACHI),
				"a full inventory must refuse the weapon instead of dropping it");
		helper.assertTrue(TestPlayers.count(owner, ModItems.MUSOU_NO_HITOTACHI) == 0, "no weapon was issued");
		assertNoItemEntities(helper, owner);
		TestPlayers.leave(owner);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void droppingWithFullInventoryDoesNotRecurse(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		TestPlayers.fillInventory(owner);
		Inventory inventory = owner.getInventory();
		inventory.items.set(inventory.selected, ItemStack.EMPTY);
		helper.assertTrue(BoundWeapons.ensureHeld(owner, ModItems.ROYAL_ICICLE), "the free main hand takes the sword");

		ItemStack sword = inventory.removeItemNoUpdate(inventory.selected);
		inventory.items.set(inventory.selected, new ItemStack(Items.DIRT));
		// Before the fix this re-entered Player.drop through placeItemBackInInventory until StackOverflowError.
		ItemEntity dropped = owner.drop(sword, false);

		helper.assertTrue(dropped == null, "a bound weapon never becomes an item entity");
		helper.assertTrue(sword.isEmpty(), "with no room left the weapon is deleted, not dropped");
		assertNoItemEntities(helper, owner);
		TestPlayers.leave(owner);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void droppedWeaponReturnsToOwner(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		helper.assertTrue(BoundWeapons.ensureHeld(owner, ModItems.REM_MORNING_STAR), "issued into the main hand");

		owner.drop(true);

		helper.assertTrue(TestPlayers.count(owner, ModItems.REM_MORNING_STAR) == 1, "the mace went back to the owner");
		assertNoItemEntities(helper, owner);
		TestPlayers.leave(owner);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void stashedCopyGoesStaleWhenReissued(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		Inventory inventory = owner.getInventory();
		BoundWeapons.ensureHeld(owner, ModItems.REM_MORNING_STAR);
		// Simulates moving the mace into a chest: the owner no longer carries it.
		ItemStack stashed = inventory.removeItemNoUpdate(inventory.selected);

		BoundWeapons.ensureHeld(owner, ModItems.REM_MORNING_STAR);
		inventory.add(stashed);
		helper.assertTrue(TestPlayers.count(owner, ModItems.REM_MORNING_STAR) == 2, "stash pulled back out");
		inventory.tick();

		helper.assertTrue(TestPlayers.count(owner, ModItems.REM_MORNING_STAR) == 1,
				"only the current issue survives an inventory tick");
		helper.assertTrue(BoundWeapons.isValidFor(inventory.getItem(inventory.selected), owner),
				"the surviving copy is the reissued one");
		TestPlayers.leave(owner);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void copyHeldByAnotherPlayerVanishes(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		ServerPlayer stranger = TestPlayers.join(helper);
		BoundWeapons.ensureHeld(owner, ModItems.ROYAL_ICICLE);
		stranger.getInventory().add(owner.getMainHandItem().copy());

		stranger.getInventory().tick();

		helper.assertTrue(TestPlayers.count(stranger, ModItems.ROYAL_ICICLE) == 0,
				"a bound weapon is useless to anyone but its owner");
		helper.assertTrue(TestPlayers.count(owner, ModItems.ROYAL_ICICLE) == 1, "the owner keeps theirs");
		TestPlayers.leave(stranger);
		TestPlayers.leave(owner);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void unissuedCopyVanishes(GameTestHelper helper) {
		ServerPlayer holder = TestPlayers.join(helper);
		holder.getInventory().add(new ItemStack(ModItems.MUSOU_NO_HITOTACHI));

		holder.getInventory().tick();

		helper.assertTrue(TestPlayers.count(holder, ModItems.MUSOU_NO_HITOTACHI) == 0,
				"a copy without a current issue (old save, /give) is not a free weapon");
		TestPlayers.leave(holder);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void raidenSwordDrawRespectsFullInventory(GameTestHelper helper) {
		ServerPlayer raiden = TestPlayers.join(helper);
		helper.assertTrue(HeroTransformService.transform(raiden, RaidenHero.ID), "transformed into Raiden");
		TestPlayers.fillInventory(raiden);

		AbilityRouter.activate(raiden, AbilityIds.RAIDEN_SWORD_DRAW);
		helper.assertFalse(raiden.getAttachedOrCreate(ModAttachments.HERO_DATA).isActive(AbilityIds.RAIDEN_SWORD_DRAW),
				"no room for Yamato means the draw does not start");
		helper.assertTrue(TestPlayers.count(raiden, ModItems.MUSOU_NO_HITOTACHI) == 0, "no sword was issued");

		raiden.getInventory().items.set(raiden.getInventory().selected, ItemStack.EMPTY);
		AbilityRouter.activate(raiden, AbilityIds.RAIDEN_SWORD_DRAW);
		helper.assertTrue(raiden.getAttachedOrCreate(ModAttachments.HERO_DATA).isActive(AbilityIds.RAIDEN_SWORD_DRAW),
				"with a free hand the draw starts");
		helper.assertTrue(TestPlayers.count(raiden, ModItems.MUSOU_NO_HITOTACHI) == 1, "exactly one Yamato");

		AbilityRouter.activate(raiden, AbilityIds.RAIDEN_SWORD_DRAW);
		helper.assertTrue(TestPlayers.count(raiden, ModItems.MUSOU_NO_HITOTACHI) == 0, "sheathing removes Yamato");
		TestPlayers.leave(raiden);
		helper.succeed();
	}

	private static void assertNoItemEntities(GameTestHelper helper, ServerPlayer player) {
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0))
				.isEmpty(), "nothing was dropped into the world");
	}
}
