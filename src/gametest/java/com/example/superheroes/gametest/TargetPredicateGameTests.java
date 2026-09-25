package com.example.superheroes.gametest;

import com.example.superheroes.ability.DoomsdayRoarAbility;
import com.example.superheroes.combat.TargetFilters;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Audit B19: hostile AoE selection honors vanilla PvP/team rules —
 * teammates without friendly fire and {@code pvp=false} are skipped.
 */
public final class TargetPredicateGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void aoeSkipsTeammateAndHitsEnemy(GameTestHelper helper) {
		ServerPlayer attacker = TestPlayers.join(helper, "attacker");
		ServerPlayer teammate = TestPlayers.join(helper, "teammate");
		ServerPlayer enemy = TestPlayers.join(helper, "enemy");
		for (ServerPlayer p : new ServerPlayer[] {attacker, teammate, enemy}) {
			clearSpawnInvulnerability(p);
			p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		}
		ServerLevel level = helper.getLevel();
		boolean oldPvp = level.getServer().isPvpAllowed();
		level.getServer().setPvpAllowed(true);
		Vec3 center = Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)));
		attacker.teleportTo(center.x, center.y, center.z);
		teammate.teleportTo(center.x + 2, center.y, center.z);
		enemy.teleportTo(center.x - 2, center.y, center.z);

		PlayerTeam allies = level.getScoreboard().addPlayerTeam("gametest_allies");
		allies.setAllowFriendlyFire(false);
		level.getScoreboard().addPlayerToTeam(attacker.getScoreboardName(), allies);
		level.getScoreboard().addPlayerToTeam(teammate.getScoreboardName(), allies);

		AABB box = new AABB(center, center).inflate(6.0);
		helper.assertTrue(
				level.getEntitiesOfClass(LivingEntity.class, box, TargetFilters.hostileTo(attacker))
						.contains(enemy),
				"enemy player is a hostile target");
		helper.assertTrue(
				level.getEntitiesOfClass(LivingEntity.class, box, TargetFilters.hostileTo(attacker))
						.stream().noneMatch(e -> e == teammate || e == attacker),
				"same-team player and the caster are skipped");

		float enemyHp = enemy.getHealth();
		float teammateHp = teammate.getHealth();
		new DoomsdayRoarAbility().tryActivate(attacker);
		helper.assertTrue(enemy.getHealth() < enemyHp, "enemy player took the roar");
		helper.assertTrue(teammate.getHealth() == teammateHp,
				"teammate was not damaged (canHarmPlayer rejects friendly fire)");

		level.getServer().setPvpAllowed(oldPvp);
		for (ServerPlayer p : new ServerPlayer[] {attacker, teammate, enemy}) {
			TestPlayers.leave(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pvpFalseSkipsPlayerTargets(GameTestHelper helper) {
		ServerPlayer attacker = TestPlayers.join(helper, "attacker2");
		ServerPlayer enemy = TestPlayers.join(helper, "enemy");
		ServerLevel level = helper.getLevel();
		Vec3 center = Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)));
		attacker.teleportTo(center.x, center.y, center.z);
		enemy.teleportTo(center.x + 2, center.y, center.z);
		enemy.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		attacker.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

		AABB box = new AABB(center, center).inflate(6.0);
		boolean oldPvp2 = level.getServer().isPvpAllowed();
		level.getServer().setPvpAllowed(true);
		helper.assertTrue(
				level.getEntitiesOfClass(LivingEntity.class, box, TargetFilters.hostileTo(attacker))
						.contains(enemy),
				"pvp=true keeps the enemy targettable");
		level.getServer().setPvpAllowed(false);
		try {
			helper.assertTrue(
					level.getEntitiesOfClass(LivingEntity.class, box, TargetFilters.hostileTo(attacker))
							.stream().noneMatch(e -> e == enemy),
					"pvp=false drops player targets from hostile scans");
		} finally {
			level.getServer().setPvpAllowed(oldPvp2);
		}

		TestPlayers.leave(attacker);
		TestPlayers.leave(enemy);
		helper.succeed();
	}

	private static void clearSpawnInvulnerability(ServerPlayer player) {
		try {
			java.lang.reflect.Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			f.setAccessible(true);
			f.setInt(player, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		player.invulnerableTime = 0;
	}
}
