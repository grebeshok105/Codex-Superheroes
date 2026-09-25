package com.example.superheroes.gametest;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.attachment.SungShadowArmy;
import com.example.superheroes.effect.SungJinwooController;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.entity.ShadowSoldierEntity;
import com.example.superheroes.hero.SungJinwooHero;
import com.example.superheroes.transform.HeroTransformService;
import com.example.superheroes.util.SafeTeleport;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Audit B18 (Sung's shadow army survives restarts and releases on untransform)
 * and B22 (ability teleports clamp at the first obstructed position).
 */
public final class ShadowsTeleportsGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void shadowsDisbandOnUntransform(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		helper.assertTrue(HeroTransformService.transform(player, SungJinwooHero.ID),
				"transform into Sung Jin-Woo");

		helper.runAfterDelay(5, () -> {
			helper.assertTrue(SungJinwooController.aliveCount(player) == SungJinwooController.MAX_SHADOWS,
					"the initial shadow army is summoned");
			helper.assertTrue(player.getAttached(ModAttachments.SUNG_SHADOW_ARMY).summoned(),
					"the summon is recorded in the persistent attachment");

			HeroTransformService.forceUntransform(player);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(SungJinwooController.aliveCount(player) == 0,
						"untransform releases every shadow");
				SungShadowArmy army = player.getAttached(ModAttachments.SUNG_SHADOW_ARMY);
				helper.assertTrue(army == null || army.shadowIds().isEmpty(),
						"the persistent army record is cleared");
				AABB area = helper.getBounds().inflate(64.0);
				helper.assertTrue(
						helper.getLevel().getEntitiesOfClass(ShadowSoldierEntity.class, area).isEmpty(),
						"no shadow entities remain in the level");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void orphanShadowDiscardsItself(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		// Not Sung — a shadow saved under this owner must not keep existing.
		ShadowSoldierEntity shadow = helper.spawn(ModEntities.SHADOW_SOLDIER, 1, 1, 1);
		shadow.setOwnerId(owner.getUUID());

		helper.runAfterDelay(3, () -> {
			helper.assertFalse(shadow.isAlive(), "a shadow without an army entry removes itself");
			TestPlayers.leave(owner);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void teleportClampsAtWall(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 start = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(start.x, start.y, start.z);

		// Stone wall at x=4 spanning z=0..2, y=1..3 — the blink line crosses it.
		for (int y = 1; y <= 3; y++) {
			for (int z = 0; z <= 2; z++) {
				helper.setBlock(new BlockPos(4, y, z), Blocks.STONE);
			}
		}

		Vec3 dest = helper.absoluteVec(new Vec3(7.5, 1.0, 1.5));
		Vec3 safe = SafeTeleport.clamp(helper.getLevel(), player, dest);

		double wallX = helper.absolutePos(new BlockPos(4, 1, 1)).getX();
		helper.assertTrue(safe.x < wallX, "the teleport clamps to the last free spot before the wall");
		helper.assertTrue(safe.x > start.x + 1.0, "the teleport still moves forward up to the wall");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void teleportReachesClearDestination(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 start = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(start.x, start.y, start.z);

		Vec3 dest = helper.absoluteVec(new Vec3(6.5, 1.0, 1.5));
		Vec3 safe = SafeTeleport.clamp(helper.getLevel(), player, dest);
		helper.assertTrue(safe.equals(dest) || safe.distanceTo(dest) < 0.01,
				"an unobstructed blink reaches the exact destination");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
