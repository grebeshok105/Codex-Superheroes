package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.mechanic.targeting.TargetFilter;
import io.github.grebeshok105.codex.mechanic.targeting.Targeting;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;

/**
 * Stage M1: {@link TargetFilter} flags and the {@link Targeting#living} area scan.
 * Zombie, spectator observer and owner — each flag cuts exactly its own candidate.
 */
public final class MechanicTargetingGameTests implements FabricGameTest {

	/** Every flag off: the filter keeps everything, so migrated call sites keep their old predicate. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void defaultFilterKeepsEveryone(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper, "m1DefaultOwner");
		ServerPlayer spectator = TestPlayers.join(helper, "m1DefaultSpectator");
		spectator.setGameMode(GameType.SPECTATOR);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);

		TargetFilter filter = TargetFilter.of(owner);
		helper.assertTrue(filter.test(owner), "default keeps the owner");
		helper.assertTrue(filter.test(zombie), "default keeps a mob");
		helper.assertTrue(filter.test(spectator), "default keeps a spectator");

		TestPlayers.leave(owner);
		TestPlayers.leave(spectator);
		helper.succeed();
	}

	/** withoutOwner / withoutSpectators / alive each drop exactly their own candidate. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void eachFlagCutsExactlyItsOwn(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper, "m1FlagOwner");
		ServerPlayer spectator = TestPlayers.join(helper, "m1FlagSpectator");
		spectator.setGameMode(GameType.SPECTATOR);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);

		TargetFilter noOwner = TargetFilter.of(owner).withoutOwner();
		helper.assertFalse(noOwner.test(owner), "withoutOwner drops the owner");
		helper.assertTrue(noOwner.test(zombie) && noOwner.test(spectator),
				"withoutOwner keeps mob and spectator");

		TargetFilter noSpectators = TargetFilter.of(owner).withoutSpectators();
		helper.assertFalse(noSpectators.test(spectator), "withoutSpectators drops the spectator");
		helper.assertTrue(noSpectators.test(zombie) && noSpectators.test(owner),
				"withoutSpectators keeps mob and owner");

		zombie.kill();
		TargetFilter aliveOnly = TargetFilter.of(owner).alive();
		helper.assertFalse(zombie.isAlive(), "killed zombie is dead");
		helper.assertFalse(aliveOnly.test(zombie), "alive drops the dead mob");
		helper.assertTrue(aliveOnly.test(owner) && aliveOnly.test(spectator),
				"alive keeps living candidates");

		TestPlayers.leave(owner);
		TestPlayers.leave(spectator);
		helper.succeed();
	}

	/** respectPvp drops players the attacker cannot harm (pvp=false) while mobs stay targettable. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void respectPvpCutsUnharmablePlayers(GameTestHelper helper) {
		ServerPlayer attacker = TestPlayers.join(helper, "m1PvpAttacker");
		ServerPlayer victim = TestPlayers.join(helper, "m1PvpVictim");
		attacker.setGameMode(GameType.SURVIVAL);
		victim.setGameMode(GameType.SURVIVAL);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		ServerLevel level = helper.getLevel();
		boolean oldPvp = level.getServer().isPvpAllowed();
		level.getServer().setPvpAllowed(false);

		TargetFilter pvp = new TargetFilter(attacker, false, false, false, true, false, null);
		try {
			helper.assertFalse(pvp.test(victim), "pvp=false makes the victim untargettable");
			helper.assertTrue(pvp.test(zombie), "pvp=false still allows mobs");
			helper.assertTrue(new TargetFilter(attacker, false, false, false, false, false, null)
							.test(victim),
					"without respectPvp the victim stays targettable");
		} finally {
			level.getServer().setPvpAllowed(oldPvp);
			TestPlayers.leave(attacker);
			TestPlayers.leave(victim);
		}
		helper.succeed();
	}

	/** excludeAllies drops scoreboard teammates of the owner (the owner is allied to itself too). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void excludeAlliesCutsTeammates(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper, "m1AllyOwner");
		ServerPlayer teammate = TestPlayers.join(helper, "m1AllyMate");
		ServerPlayer outsider = TestPlayers.join(helper, "m1AllyFoe");
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		ServerLevel level = helper.getLevel();

		PlayerTeam allies = level.getScoreboard().addPlayerTeam("m1_allies");
		level.getScoreboard().addPlayerToTeam(owner.getScoreboardName(), allies);
		level.getScoreboard().addPlayerToTeam(teammate.getScoreboardName(), allies);

		TargetFilter noAllies = new TargetFilter(owner, false, false, false, false, true, null);
		helper.assertFalse(noAllies.test(teammate), "excludeAllies drops a teammate");
		helper.assertFalse(noAllies.test(owner), "owner is allied to itself on the team");
		helper.assertTrue(noAllies.test(outsider), "excludeAllies keeps a non-team player");
		helper.assertTrue(noAllies.test(zombie), "excludeAllies keeps a teamless mob");

		TestPlayers.leave(owner);
		TestPlayers.leave(teammate);
		TestPlayers.leave(outsider);
		helper.succeed();
	}

	/** and(more) appends the predicate; a second and() composes onto the first. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void andComposesExtraPredicate(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper, "m1AndOwner");
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		Zombie other = helper.spawn(EntityType.ZOMBIE, 3, 1, 1);

		TargetFilter noFirstZombie = TargetFilter.of(owner).and(e -> e != zombie);
		helper.assertFalse(noFirstZombie.test(zombie), "and() drops the rejected mob");
		helper.assertTrue(noFirstZombie.test(other) && noFirstZombie.test(owner),
				"and() keeps the rest");

		TargetFilter chained = noFirstZombie.and(e -> false);
		helper.assertFalse(chained.test(other), "second and() composes with extra.and(more)");

		TestPlayers.leave(owner);
		helper.succeed();
	}

	/** Targeting.living applies the filter to the real box scan. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void livingScanAppliesFilter(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper, "m1ScanOwner");
		ServerPlayer spectator = TestPlayers.join(helper, "m1ScanSpectator");
		spectator.setGameMode(GameType.SPECTATOR);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		Vec3 center = Vec3.atBottomCenterOf(
				helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)));
		owner.teleportTo(center.x + 1.0, center.y, center.z);
		spectator.teleportTo(center.x - 1.0, center.y, center.z);
		AABB box = new AABB(center, center).inflate(6.0);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			ServerLevel level = helper.getLevel();
			List<LivingEntity> all = Targeting.living(level, box, TargetFilter.of(owner));
			helper.assertTrue(all.contains(zombie) && all.contains(owner) && all.contains(spectator),
					"default filter keeps everyone in the box");

			List<LivingEntity> targets = Targeting.living(level, box,
					TargetFilter.of(owner).withoutOwner().alive().withoutSpectators());
			helper.assertTrue(targets.contains(zombie), "rules keep the hostile mob");
			helper.assertFalse(targets.contains(owner) || targets.contains(spectator),
					"rules cut the owner and the spectator");

			TestPlayers.leave(owner);
			TestPlayers.leave(spectator);
			helper.succeed();
		});
	}
}
