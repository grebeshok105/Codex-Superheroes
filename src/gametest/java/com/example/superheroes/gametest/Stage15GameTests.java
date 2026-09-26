package com.example.superheroes.gametest;

import com.example.superheroes.effect.RamCompanionController;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.entity.RamEntity;
import com.example.superheroes.horde.HordeManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Audit "потенциальные" regression coverage: the horde pauses while nobody is
 * around (no spawns into unloaded chunks), and the Ram companion can never
 * duplicate through a stale tracking reference.
 */
public final class Stage15GameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void hordePausesWithoutAudience(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		// Structures sit within earshot of each other — players leaked by an earlier
		// test (e.g. one that failed mid-body) would count as audience here.
		for (ServerPlayer other : new java.util.ArrayList<>(level.getServer().getPlayerList().getPlayers())) {
			if (other != player) {
				TestPlayers.leave(other);
			}
		}
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		// Far enough that the horde has no audience (> 96 blocks).
		player.teleportTo(center.x + 200, center.y, center.z + 200);
		HordeManager.startHorde(level, center, player);

		helper.runAfterDelay(100, () -> {
			helper.assertTrue(HordeManager.liveMobCount(level) == 0,
					"with nobody around the horde must not start waves");

			// Player walks into the arena — the horde resumes on its own.
			player.teleportTo(center.x, center.y + 1, center.z);
			helper.runAfterDelay(100, () -> {
				helper.assertTrue(HordeManager.liveMobCount(level) > 0,
						"an audience near the centre resumes the horde");
				HordeManager.stopHorde(level);
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ramCompanionAdoptsAndDeduplicates(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();

		// An unregistered live ram (e.g. the tracked reference was lost on unload)
		// is adopted, not re-spawned.
		RamEntity ram = ModEntities.RAM.create(level);
		ram.setOwnerId(owner.getUUID());
		ram.moveTo(owner.getX() + 1, owner.getY(), owner.getZ(), 0f, 0f);
		level.addFreshEntity(ram);
		helper.assertTrue(ram == RamCompanionController.reconcileForTest(owner),
				"the live ram is adopted, not duplicated");

		// A second copy — a persisted RamEntity from an old save — is discarded.
		RamEntity stale = ModEntities.RAM.create(level);
		stale.setOwnerId(owner.getUUID());
		stale.moveTo(owner.getX() - 1, owner.getY(), owner.getZ(), 0f, 0f);
		level.addFreshEntity(stale);
		helper.assertTrue(ram == RamCompanionController.reconcileForTest(owner),
				"the registered ram wins");
		helper.assertFalse(stale.isAlive(), "the duplicate copy is discarded");
		helper.assertFalse(ram.shouldBeSaved(),
				"session summons must never persist to NBT — a reloaded copy would duplicate");

		ram.discard();
		RamCompanionController.clear(owner.getUUID());
		TestPlayers.leave(owner);
		helper.succeed();
	}
}
