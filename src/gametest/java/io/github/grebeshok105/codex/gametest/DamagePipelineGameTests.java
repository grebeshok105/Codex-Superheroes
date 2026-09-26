package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.damage.ModDamageTypes;
import io.github.grebeshok105.codex.effect.KawarimiController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardTimeSlowController;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.hero.NarutoHero;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Audit B9/B11/B20: accounting runs on landed damage, death-saves live in
 * ALLOW_DEATH, mod ability damage bypasses i-frames, and Reinhard's time slow
 * does not change the server tick rate.
 */
public final class DamagePipelineGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void modAbilityDamageBypassesCooldown(GameTestHelper helper) {
		ServerPlayer attacker = TestPlayers.join(helper);
		ServerLevel level = helper.getLevel();
		Zombie laserTarget = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		Zombie control = helper.spawn(EntityType.ZOMBIE, 3, 1, 1);

		float hp = laserTarget.getHealth();
		laserTarget.hurt(ModDamageTypes.eyeLaser(level, attacker), 5f);
		laserTarget.hurt(ModDamageTypes.eyeLaser(level, attacker), 5f);
		helper.assertTrue(hp - laserTarget.getHealth() >= 9.9f,
				"eye_laser is in bypasses_cooldown: both same-tick hits apply fully");

		float controlHp = control.getHealth();
		control.hurt(level.damageSources().mobAttack(control), 5f);
		control.hurt(level.damageSources().mobAttack(control), 5f);
		helper.assertTrue(controlHp - control.getHealth() <= 5.01f,
				"vanilla mob_attack still respects i-frames");

		TestPlayers.leave(attacker);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void timeSlowFreezesRadiusNotTickRate(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		Zombie frozen = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		owner.teleportTo(frozen.getX() - 3.0, frozen.getY(), frozen.getZ());

		ReinhardTimeSlowController.triggerAbilitySlow(owner);
		// See HeroModuleLifecycleGameTests: wait for the spawn to be entity-visible first.
		TestPlayers.awaitVisible(helper, frozen, () -> helper.runAfterDelay(2, () -> {
			helper.assertTrue(helper.getLevel().getServer().tickRateManager().tickrate() == 20.0f,
					"time slow must not touch the server tick rate (audit B11)");
			helper.assertTrue(TestPlayers.lockOwners(frozen, ControlLockKind.NO_AI)
							.contains(owner.getUUID()),
					"entities inside the freeze radius get a NoAI control lock");
			helper.assertTrue(ReinhardTimeSlowController.isActive(owner),
					"the slow is still active for its owner");

			ReinhardTimeSlowController.onPlayerGone(owner);
			helper.assertFalse(TestPlayers.lockOwners(frozen, ControlLockKind.NO_AI)
							.contains(owner.getUUID()),
					"ending the slow releases the owner's lock ref");
			TestPlayers.leave(owner);
			helper.succeed();
		}));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void kawarimiSavesFromLethalHit(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);
		Zombie attacker = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		// Mock players join with private spawnInvulnerableTime=60 that blocks any
		// hurt() not in #bypasses_invulnerability; tick() only decrements by 1.
		try {
			java.lang.reflect.Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			f.setAccessible(true);
			f.setInt(player, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		player.invulnerableTime = 0;

		float maxHp = player.getMaxHealth();
		player.hurt(helper.getLevel().damageSources().mobAttack(attacker), maxHp + 100f);
		helper.assertTrue(player.isAlive() && player.getHealth() > 0f,
				"kawarimi substitution prevents the death via ALLOW_DEATH");
		helper.assertTrue(KawarimiController.getCooldownRemainingTicks(player) > 0,
				"the substitution goes on cooldown");

		TestPlayers.leave(player);
		helper.succeed();
	}
}
