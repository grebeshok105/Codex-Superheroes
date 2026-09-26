package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.mechanic.motion.Motion;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

/**
 * Plan 5 / M1: {@link Motion} is the single seam for server-side velocity
 * changes — it sets the delta, marks the entity for the vanilla tracker and,
 * for players, pushes the motion packet immediately.
 */
public final class MotionGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void setMarksAndAppliesVelocity(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		Motion.set(zombie, new Vec3(1.0, 0.5, -0.25), Motion.Sync.MARK);
		helper.assertTrue(zombie.hurtMarked, "hurtMarked set for the tracker");
		helper.assertValueEqual(zombie.getDeltaMovement(), new Vec3(1.0, 0.5, -0.25), "delta movement");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void addAccumulatesOnCurrentVelocity(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		Motion.set(zombie, new Vec3(1.0, 0.0, 0.0), Motion.Sync.MARK);
		Motion.add(zombie, new Vec3(0.0, 2.0, 0.5), Motion.Sync.MARK);
		helper.assertValueEqual(zombie.getDeltaMovement(), new Vec3(1.0, 2.0, 0.5), "delta movement");
		helper.assertTrue(zombie.hurtMarked, "hurtMarked set");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sendToPlayerVariantIsHarmlessForNonPlayers(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		Motion.set(zombie, new Vec3(0.0, 1.0, 0.0), Motion.Sync.MARK_AND_SEND_TO_PLAYER);
		helper.assertTrue(zombie.hurtMarked, "hurtMarked set");
		helper.assertValueEqual(zombie.getDeltaMovement(), new Vec3(0.0, 1.0, 0.0), "delta movement");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sendToPlayerPushesMotionToThePlayerItself(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		Motion.set(player, new Vec3(0.25, 0.75, -1.0), Motion.Sync.MARK_AND_SEND_TO_PLAYER);
		helper.assertTrue(player.hurtMarked, "hurtMarked set");
		helper.assertValueEqual(player.getDeltaMovement(), new Vec3(0.25, 0.75, -1.0), "delta movement");
		helper.succeed();
	}
}
