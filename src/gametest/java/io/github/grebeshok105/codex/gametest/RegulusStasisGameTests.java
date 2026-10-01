package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.ability.GreedsEmbraceAbility;
import io.github.grebeshok105.codex.hero.regulus.runtime.GreedStasisController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Stasis dome (Greed's Embrace rework): acquire-anchor capture, mob control
 * locks, player positional pin, damage queue with the 35% release cap, and
 * single-dome-per-caster. Concurrent tests share one level, so every test
 * isolates itself on a forced-chunk pad far from the shared structure.
 */
public class RegulusStasisGameTests {
	private static final String EMPTY_STRUCTURE = "fabric-gametest-api-v1:empty";

	/** Full cast path: activate -> acquire -> fire tick opens the dome. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void greedsEmbraceCastOpensDomeOnFireTick(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 0, 8);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x + 3.0, spot.y, spot.z);
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			aimAt(caster, zombie.position());
			AbilityRouter.activate(caster, GreedsEmbraceAbility.ID);
			helper.assertTrue(RegulusCastState.isCasting(caster, GreedsEmbraceAbility.ID),
					"activate starts the embrace cast");
			helper.runAfterDelay(10, () -> {
				helper.assertFalse(GreedStasisController.inStasis(zombie),
						"dome waits for the fire tick");
				helper.assertFalse(AbilityCooldowns.isOnCooldown(caster, GreedsEmbraceAbility.ID),
						"no cooldown before fire");
				helper.assertTrue(energy(caster) >= 999.5f,
						"no charge before fire: " + energy(caster));
			});
			helper.runAfterDelay(22, () -> {
				helper.assertTrue(GreedStasisController.inStasis(zombie), "dome opened on the fire tick");
				helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
						.contains(caster.getUUID()), "NO_AI lock owned by caster");
				helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_GRAVITY)
						.contains(caster.getUUID()), "NO_GRAVITY lock owned by caster");
				helper.assertTrue(AbilityCooldowns.isOnCooldown(caster, GreedsEmbraceAbility.ID),
						"cooldown armed on fire");
				helper.assertTrue(energy(caster) < 700f,
						"fire tick charged 400 (not the old 700): " + energy(caster));
				helper.assertTrue(RegulusCastState.isCasting(caster, GreedsEmbraceAbility.ID),
						"cast session lasts until castUntil (44t)");
			});
			helper.runAfterDelay(50, () -> helper.assertFalse(
					RegulusCastState.isCasting(caster, GreedsEmbraceAbility.ID),
					"cast session removed at castUntil"));
			// Dome opened at ~T+18 and lives 80 ticks.
			helper.runAfterDelay(105, () -> {
				helper.assertFalse(GreedStasisController.inStasis(zombie), "dome expires after 80 ticks");
				helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty(),
						"locks released on expiry");
				helper.succeed();
			});
		}));
	}

	/** The anchor is the acquire-end aim (13t): re-aiming before fire must not move the dome. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void stasisAnchorCapturedAtAcquireEnd(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 1, 20);
		Vec3 spotB = spot.add(16.0, 0.0, 0.0);
		forceArea(helper.getLevel(), spotB);
		Zombie a = spawnAt(helper, EntityType.HUSK, spot.x, spot.y, spot.z + 2.0);
		Zombie b = spawnAt(helper, EntityType.HUSK, spotB.x, spotB.y, spotB.z);
		TestPlayers.awaitVisible(helper, b, () -> helper.runAfterDelay(2, () -> {
			aimAt(caster, a.position());
			AbilityRouter.activate(caster, GreedsEmbraceAbility.ID);
			helper.runAfterDelay(15, () -> aimAt(caster, b.position()));
			helper.runAfterDelay(24, () -> {
				helper.assertTrue(GreedStasisController.inStasis(a),
						"dome anchors on the acquire-end aim, not the fire-tick aim");
				helper.assertFalse(GreedStasisController.inStasis(b),
						"re-aim after acquire does not move the dome");
				helper.succeed();
			});
		}));
	}

	/** Required pin: allied players and the caster's tamed pets are never caught. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void stasisSkipsAllies(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		ServerPlayer ally = TestPlayers.join(helper, "stasis-ally");
		TestPlayers.clearSpawnInvulnerability(ally);
		Vec3 spot = isolate(helper, caster, 2, 12);
		PlayerTeam team = helper.getLevel().getScoreboard().addPlayerTeam("stasis_allies");
		team.setAllowFriendlyFire(false);
		helper.getLevel().getScoreboard().addPlayerToTeam(caster.getScoreboardName(), team);
		helper.getLevel().getScoreboard().addPlayerToTeam(ally.getScoreboardName(), team);
		ally.teleportTo(spot.x + 3.0, spot.y, spot.z);
		Wolf wolf = spawnAt(helper, EntityType.WOLF, spot.x - 3.0, spot.y, spot.z);
		wolf.tame(caster);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x, spot.y, spot.z + 3.0);
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			// Capture honors the shared server pvp flag; set it in the same tick as
			// tryOpen so concurrent tests cannot flip it between write and read.
			helper.getLevel().getServer().setPvpAllowed(true);
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(5, () -> {
				helper.assertTrue(GreedStasisController.inStasis(zombie), "hostile mob is caught");
				helper.assertFalse(GreedStasisController.inStasis(ally), "allied player is skipped");
				helper.assertFalse(GreedStasisController.inStasis(wolf),
						"tamed pet of the caster is skipped");
				helper.assertFalse(GreedStasisController.inStasis(caster), "caster is never caught");
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
				helper.succeed();
			});
		}));
	}

	/** Required pin: a held player is teleport-locked back to the captured position. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void stasisActuallyPinsPlayer(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		ServerPlayer victim = TestPlayers.join(helper, "stasis-victim");
		TestPlayers.clearSpawnInvulnerability(victim);
		Vec3 spot = isolate(helper, caster, 3, 12);
		victim.teleportTo(spot.x + 2.0, spot.y, spot.z);
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.runAfterDelay(3, () -> {
			helper.getLevel().getServer().setPvpAllowed(true);
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(GreedStasisController.inStasis(victim), "player is caught");
				Vec3 lockPos = victim.position();
				victim.teleportTo(lockPos.x + 2.5, lockPos.y, lockPos.z);
				victim.setDeltaMovement(new Vec3(1.0, 0.0, 0.0));
				helper.runAfterDelay(3, () -> {
					helper.assertTrue(victim.position().distanceToSqr(lockPos) < 0.25,
							"pin pulls the victim back to the lock position: " + victim.position());
					helper.getLevel().getServer().setPvpAllowed(oldPvp);
					helper.succeed();
				});
			});
		});
	}

	/** Required pin: queued damage on release is capped at 35% of the victim's max health. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void stasisCapsAtThirtyFivePercent(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 4, 10);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x + 2.0, spot.y, spot.z);
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(GreedStasisController.inStasis(zombie), "victim held");
				float hp = zombie.getHealth();
				helper.assertFalse(zombie.hurt(helper.getLevel().damageSources().generic(), 60f),
						"stasis denies direct damage");
				zombie.hurt(helper.getLevel().damageSources().generic(), 60f);
				helper.assertTrue(Math.abs(zombie.getHealth() - hp) < 0.001f,
						"queued damage does not land while held: " + zombie.getHealth());
			});
			helper.runAfterDelay(90, () -> {
				float expected = zombie.getMaxHealth() * (1f - 0.35f);
				helper.assertTrue(zombie.isAlive(), "capped release leaves the victim alive");
				helper.assertTrue(Math.abs(zombie.getHealth() - expected) < 0.5f,
						"release applies at most 35% of maxHp: hp=" + zombie.getHealth());
				helper.assertFalse(GreedStasisController.inStasis(zombie), "dome released");
				helper.succeed();
			});
		}));
	}

	/** Required pin: release damage applies immediately and is not re-queued. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void stasisReleaseDamageIsNotRequeued(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 5, 10);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x + 2.0, spot.y, spot.z);
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(3, () -> zombie.hurt(
					helper.getLevel().damageSources().generic(), 10f));
			helper.runAfterDelay(90, () -> {
				float lost = zombie.getMaxHealth() - zombie.getHealth();
				helper.assertTrue(lost > 5f, "release applied the queued damage: lost=" + lost);
				helper.assertTrue(lost <= zombie.getMaxHealth() * 0.35f + 0.5f,
						"release stayed under the cap: lost=" + lost);
				zombie.invulnerableTime = 0;
			helper.assertTrue(zombie.hurt(helper.getLevel().damageSources().generic(), 1f),
						"damage lands normally once the dome is gone");
				helper.succeed();
			});
		}));
	}

	/** Required pin: caster leaving (or dying / hero-clear) releases the dome through the full release flow. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void stasisReleasesOnCasterGone(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 6, 10);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x + 2.0, spot.y, spot.z);
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(GreedStasisController.inStasis(zombie), "victim held");
				zombie.hurt(helper.getLevel().damageSources().generic(), 10f);
				TestPlayers.leave(caster);
			});
			helper.runAfterDelay(6, () -> {
				helper.assertFalse(GreedStasisController.inStasis(zombie), "caster leave releases the dome");
				helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty()
								&& TestPlayers.lockOwners(zombie, ControlLockKind.NO_GRAVITY).isEmpty(),
						"control locks released");
				helper.assertTrue(zombie.getMaxHealth() - zombie.getHealth() > 5f,
						"queued damage applied through the release flow: " + zombie.getHealth());
				helper.succeed();
			});
		}));
	}

	/** Required pin: recasting releases the old dome instead of stacking a second one. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void oneDomePerCaster(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 7, 20);
		Vec3 spotB = spot.add(18.0, 0.0, 0.0);
		forceArea(helper.getLevel(), spotB);
		Zombie a = spawnAt(helper, EntityType.HUSK, spot.x + 3.0, spot.y, spot.z);
		Zombie b = spawnAt(helper, EntityType.HUSK, spotB.x, spotB.y, spotB.z);
		TestPlayers.awaitVisible(helper, b, () -> helper.runAfterDelay(2, () -> {
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(4, () -> {
				helper.assertTrue(GreedStasisController.inStasis(a), "first dome holds its victim");
				GreedStasisController.tryOpen(caster, spotB);
			});
			helper.runAfterDelay(8, () -> {
				helper.assertFalse(GreedStasisController.inStasis(a), "recast releases the old dome");
				helper.assertTrue(GreedStasisController.inStasis(b), "second dome holds its victims");
				helper.assertTrue(zombieFullHp(a), "untouched victim is unhurt by the swap");
				helper.succeed();
			});
		}));
	}

	/** A greed-frozen victim inside the dome radius is never captured — the two systems
	 *  would fight over the same control locks (same lock owner). */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void stasisSkipsFrozenVictim(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 8, 10);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x + 2.0, spot.y, spot.z);
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			RegulusGreedController.startMagnet(caster, zombie);
			RegulusGreedController.releaseAndFreeze(caster);
			helper.assertTrue(RegulusGreedController.isFrozen(zombie), "precondition: mania freeze holds the victim");
			GreedStasisController.tryOpen(caster, spot);
			helper.runAfterDelay(3, () -> {
				helper.assertFalse(GreedStasisController.inStasis(zombie), "frozen victim is not dome-captured");
				helper.assertTrue(RegulusGreedController.isFrozen(zombie), "the freeze still owns its victim");
				helper.succeed();
			});
		}));
	}

	/** A dome-held victim refuses the mania freeze — releasing either side would
	 *  otherwise strip a control lock the other system still owns. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void freezeRefusedOnStasisVictim(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		TestHeroes.transform(caster, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster);
		Vec3 spot = isolate(helper, caster, 9, 10);
		Zombie zombie = spawnAt(helper, EntityType.HUSK, spot.x + 2.0, spot.y, spot.z);
		TestPlayers.awaitVisible(helper, zombie, () -> helper.runAfterDelay(2, () -> {
			GreedStasisController.tryOpen(caster, spot);
			helper.assertTrue(GreedStasisController.inStasis(zombie), "precondition: dome holds the victim");
			RegulusGreedController.startMagnet(caster, zombie);
			RegulusGreedController.releaseAndFreeze(caster);
			helper.assertFalse(RegulusGreedController.isFrozen(zombie), "stasis-held victim refuses the freeze");
			helper.assertTrue(GreedStasisController.inStasis(zombie), "the dome still owns its victim");
			helper.succeed();
		}));
	}

	private static boolean zombieFullHp(Zombie zombie) {
		return Math.abs(zombie.getHealth() - zombie.getMaxHealth()) < 0.001f;
	}

	private static float energy(ServerPlayer player) {
		return HeroDataStore.get(player).energy();
	}

	/** Points the caster's view at {@code target} (server-side rotation only). */
	private static void aimAt(ServerPlayer player, Vec3 target) {
		Vec3 d = target.subtract(player.getEyePosition());
		double len = Math.sqrt(d.x * d.x + d.y * d.y + d.z * d.z);
		if (len < 1.0E-4) {
			return;
		}
		player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
		player.setXRot((float) -Math.toDegrees(Math.asin(d.y / len)));
	}

	/**
	 * Teleports the caster to a forced chunk far from the shared structure, on a
	 * stone pad ({@code radius}-block half-width), and returns the pad center.
	 */
	private static Vec3 isolate(GameTestHelper helper, ServerPlayer player, int slot, int radius) {
		ServerLevel level = helper.getLevel();
		double x = player.getX() + 512.0 + 96.0 * slot;
		double y = player.getY() + 160.0;
		double z = player.getZ() + 512.0 + 96.0 * slot;
		forceArea(level, new Vec3(x, y, z));
		BlockPos padTop = BlockPos.containing(x, y - 1.0, z);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				level.setBlock(padTop.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		player.teleportTo(x, y, z);
		player.setDeltaMovement(Vec3.ZERO);
		return new Vec3(x, y, z);
	}

	/** Keeps a 3x3 chunk area around {@code center} loaded for the test's lifetime. */
	private static void forceArea(ServerLevel level, Vec3 center) {
		ChunkPos chunk = new ChunkPos(BlockPos.containing(center));
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setChunkForced(chunk.x + dx, chunk.z + dz, true);
			}
		}
	}

	private static <T extends Entity> T spawnAt(GameTestHelper helper, EntityType<T> type,
			double x, double y, double z) {
		// Force-load the destination chunk first: in an unloaded chunk the entity is
		// never registered for area scans, so controllers would see an empty world.
		helper.getLevel().getChunk(BlockPos.containing(x, y, z));
		T entity = type.create(helper.getLevel());
		entity.moveTo(x, y, z, 0f, 0f);
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}
}
