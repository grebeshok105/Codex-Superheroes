package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.effect.RegulusMadnessController;
import io.github.grebeshok105.codex.physics.RushTerrainBreaker;
import io.github.grebeshok105.codex.mechanic.world.WorldDestructionPolicy;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Audit B10: every ability-driven block change routes through
 * {@link WorldDestructionPolicy}, which honours unbreakable blocks, the
 * {@code superheroes:ability_immune} tag, spawn protection +
 * {@link PlayerBlockBreakEvents} for players, and mobGriefing for mobs.
 */
public final class DestructionPolicyGameTests implements FabricGameTest {
	private static final AtomicBoolean PROTECTION_DENIES = new AtomicBoolean(false);
	private static final AtomicInteger CANCELED_FIRED = new AtomicInteger();
	private static final AtomicInteger AFTER_FIRED = new AtomicInteger();

	static {
		PlayerBlockBreakEvents.BEFORE.register(
				(level, player, pos, state, blockEntity) -> !PROTECTION_DENIES.get());
		PlayerBlockBreakEvents.CANCELED.register(
				(level, player, pos, state, blockEntity) -> CANCELED_FIRED.incrementAndGet());
		PlayerBlockBreakEvents.AFTER.register(
				(level, player, pos, state, blockEntity) -> AFTER_FIRED.incrementAndGet());
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void craterSkipsUnbreakableAndImmuneBlocks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		BlockPos impact = new BlockPos(4, 8, 4);
		helper.setBlock(impact, Blocks.STONE);
		helper.setBlock(impact.west(1), Blocks.BEDROCK);
		helper.setBlock(impact.east(1), Blocks.SPAWNER);

		RegulusMadnessController.carveCrater(level, helper.absolutePos(impact), player);

		helper.assertBlockNotPresent(Blocks.STONE, impact);
		helper.assertBlockPresent(Blocks.BEDROCK, impact.west(1));
		helper.assertBlockPresent(Blocks.SPAWNER, impact.east(1));

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void playerBreakHonoursProtectionEvents(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, Blocks.STONE);
		BlockPos abs = helper.absolutePos(pos);

		PROTECTION_DENIES.set(true);
		int canceledBefore = CANCELED_FIRED.get();
		try {
			helper.assertTrue(!WorldDestructionPolicy.tryBreak(level, abs, true, player),
					"a cancelled PlayerBlockBreakEvents.BEFORE veto must deny the break");
		} finally {
			PROTECTION_DENIES.set(false);
		}
		helper.assertBlockPresent(Blocks.STONE, pos);
		helper.assertTrue(CANCELED_FIRED.get() > canceledBefore,
				"CANCELED must fire when BEFORE vetoes the break");

		int afterBefore = AFTER_FIRED.get();
		helper.assertTrue(WorldDestructionPolicy.tryBreak(level, abs, true, player),
				"an allowed player break succeeds");
		helper.assertBlockNotPresent(Blocks.STONE, pos);
		helper.assertTrue(AFTER_FIRED.get() > afterBefore,
				"AFTER must fire once the block is actually broken");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void mobCauseHonoursMobGriefing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 4);
		BlockPos pos = new BlockPos(2, 2, 3);
		helper.setBlock(pos, Blocks.STONE);
		BlockPos abs = helper.absolutePos(pos);

		GameRules.BooleanValue rule = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
		rule.set(false, level.getServer());
		try {
			helper.assertTrue(!WorldDestructionPolicy.tryBreak(level, abs, true, zombie),
					"mob-driven breaks are denied while mobGriefing is off");
			helper.assertTrue(!WorldDestructionPolicy.tryBreak(level, abs, true, null),
					"an un-attributed break counts as mob-driven for mobGriefing");
			helper.assertBlockPresent(Blocks.STONE, pos);
		} finally {
			rule.set(true, level.getServer());
		}

		helper.assertTrue(WorldDestructionPolicy.tryBreak(level, abs, true, zombie),
				"mob-driven breaks work when mobGriefing is on");
		helper.assertBlockNotPresent(Blocks.STONE, pos);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakDropsShulkerBoxWithContents(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(6, 2, 6);
		helper.setBlock(pos, Blocks.SHULKER_BOX);
		BlockPos abs = helper.absolutePos(pos);
		ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) level.getBlockEntity(abs);
		helper.assertTrue(box != null, "shulker box block entity exists");
		box.setItem(0, new ItemStack(Items.DIAMOND, 3));

		helper.assertTrue(WorldDestructionPolicy.tryBreak(level, abs, true, player),
				"policy break succeeds");
		helper.assertBlockNotPresent(Blocks.SHULKER_BOX, pos);

		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
				new AABB(abs).inflate(3.0), e -> e.getItem().is(Items.SHULKER_BOX));
		helper.assertTrue(drops.size() == 1, "the shulker box drops as an item instead of vanishing");
		ItemContainerContents contents = drops.get(0).getItem()
				.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		boolean keptDiamonds = false;
		for (ItemStack stack : contents.nonEmptyItems()) {
			keptDiamonds |= stack.is(Items.DIAMOND) && stack.getCount() == 3;
		}
		helper.assertTrue(keptDiamonds,
				"the dropped shulker box keeps its contents (audit B10 regression)");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void carveSuppressesDropsButRespectsRules(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(8, 2, 6);
		helper.setBlock(pos, Blocks.SHULKER_BOX);
		BlockPos abs = helper.absolutePos(pos);
		ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) level.getBlockEntity(abs);
		box.setItem(0, new ItemStack(Items.DIAMOND, 3));

		PROTECTION_DENIES.set(true);
		try {
			helper.assertTrue(!WorldDestructionPolicy.tryCarve(level, abs, player),
					"a claim veto denies the silent carve too");
		} finally {
			PROTECTION_DENIES.set(false);
		}
		helper.assertBlockPresent(Blocks.SHULKER_BOX, pos);

		helper.assertTrue(WorldDestructionPolicy.tryCarve(level, abs, player),
				"carve removes the block quietly");
		helper.assertBlockNotPresent(Blocks.SHULKER_BOX, pos);
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
				new AABB(abs).inflate(3.0), e -> e.getItem().is(Items.SHULKER_BOX));
		helper.assertTrue(drops.isEmpty(),
				"carve intentionally suppresses the block drop");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rushContactDropsBrokenBlocks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(x, 1, z, Blocks.DIRT);
			}
		}
		BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
		int destroyed = RushTerrainBreaker.breakContact(level, player,
				Vec3.atCenterOf(abs.above(2)), new Vec3(0, -1, 0), 2.0, 20);
		helper.assertTrue(destroyed > 0, "the rush breaks dirt in the contact sphere");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class,
				new AABB(abs).inflate(4.0), e -> e.getItem().is(Items.DIRT)).size() > 0,
				"broken blocks drop as items (audit B10 regression)");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void terrainPlacementHonoursMobGriefing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(5, 5, 5);
		helper.setBlock(pos.below(), Blocks.STONE);
		BlockPos abs = helper.absolutePos(pos);

		GameRules.BooleanValue rule = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
		rule.set(false, level.getServer());
		try {
			helper.assertTrue(!WorldDestructionPolicy.tryPlace(level, abs,
					Blocks.FIRE.defaultBlockState(), null),
					"environment-caused placement is denied while mobGriefing is off");
			helper.assertBlockPresent(Blocks.AIR, pos);
		} finally {
			rule.set(true, level.getServer());
		}
		helper.assertTrue(WorldDestructionPolicy.tryPlace(level, abs,
				Blocks.FIRE.defaultBlockState(), null),
				"placement succeeds when mobGriefing is on");
		helper.assertBlockPresent(Blocks.FIRE, pos);
		helper.succeed();
	}
}
