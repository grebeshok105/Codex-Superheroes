package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.regulus.RegulusAttachments;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Task-7 balance pins for the Regulus rework: the mania cast windup, the paid
 * freeze, the release cap, and the counter's real-attacker targeting, one-hit
 * formula and visual-only slam.
 */
public class RegulusManiaCounterGameTests implements FabricGameTest {

	private static final ResourceLocation MANIA_OF_GREED = ModId.of("mania_of_greed");
	private static final ResourceLocation COUNTER_STRIKE = ModId.of("counter_strike");

	/**
	 * Without a recorded last damager — or with one that stepped out of the
	 * 40-block reach — the counter press is denied at the hero gate and arms
	 * nothing.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void counterRequiresRealAttacker(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		helper.assertTrue(RegulusMadnessController.findCounterTarget(player) == null,
				"precondition: no recorded damager");
		helper.assertFalse(Heroes.get(RegulusHero.ID)
						.canUseAbility(player, HeroDataStore.get(player), COUNTER_STRIKE),
				"canUseAbility refuses the counter without a real attacker");
		AbilityRouter.activate(player, COUNTER_STRIKE);
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, COUNTER_STRIKE),
				"a denied counter arms no cooldown");

		Zombie zombie = spawnEntity(helper, EntityType.ZOMBIE,
				player.getX() + 2, player.getY(), player.getZ());
		zombie.setNoAi(true);
		TestPlayers.awaitVisible(helper, zombie, () -> {
			player.invulnerableTime = 0;
			helper.assertTrue(
					player.hurt(helper.getLevel().damageSources().mobAttack(zombie), 5f),
					"the zombie hit registers as last damager");
			// Beyond the 40-block reach the recorded damager no longer counts.
			zombie.teleportTo(zombie.getX() + 60, zombie.getY(), zombie.getZ());
			helper.getLevel().getChunk(zombie.blockPosition());

			helper.runAfterDelay(3, () -> {
				helper.assertTrue(RegulusMadnessController.findCounterTarget(player) == null,
						"the recorded damager is out of reach — no fallback scan");
				AbilityRouter.activate(player, COUNTER_STRIKE);
				helper.assertFalse(AbilityCooldowns.isOnCooldown(player, COUNTER_STRIKE),
						"a denied counter arms no cooldown");
				helper.assertFalse(RegulusMadnessController.isCounterInvolved(zombie),
						"no counter started on the out-of-reach damager");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * The counter hits exactly once — {@code min(45, 15 + 15% maxHp)} at the
	 * ARRIVE→SLAM contact — and nothing more lands at the final slam: a 500-HP
	 * warden keeps 455 after the whole sequence.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void counterDealsFormulaOnce(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		Warden warden = spawnEntity(helper, EntityType.WARDEN,
				player.getX() + 2.0, player.getY(), player.getZ());
		warden.setNoAi(true);

		TestPlayers.awaitVisible(helper, warden, () -> {
			player.invulnerableTime = 0;
			helper.assertTrue(
					player.hurt(helper.getLevel().damageSources().mobAttack(warden), 5f),
					"the warden hit registers as last damager");
			player.setInvulnerable(true);
			AbilityRouter.activate(player, COUNTER_STRIKE);
			helper.assertTrue(RegulusMadnessController.isCounterInvolved(warden),
					"the warden is the counter victim");

			// Catch the contact hit the tick it lands and measure that tick's
			// delta: the later fall damage is slam physics, not counter damage.
			awaitCounterHitDamage(helper, warden, warden.getHealth(), 160, dealt -> {
				// min(45, 15 + 0.15*500) * damageScale(1.0) = 45 — one hit, not
				// 30 slam-entry + 27 finalSlam + explosion.
				helper.assertTrue(Math.abs(dealt - 45f) < 0.5f,
						"the contact dealt the formula hit exactly once, dealt=" + dealt
								+ "/" + warden.getMaxHealth());
				helper.runAfterDelay(30, () -> {
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/**
	 * The slam's bang is strictly visual: a bystander parked next to the impact
	 * point takes nothing — the old {@code level.explode} (which hurt entities even
	 * under ExplosionInteraction.NONE) is gone.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void counterVisualExplosionDealsNoExtraDamage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		Warden warden = spawnEntity(helper, EntityType.WARDEN,
				player.getX() + 2.0, player.getY(), player.getZ());
		warden.setNoAi(true);
		// Inside the old 6-power explosion radius but outside the 3-block crater,
		// parked mid-air: it cannot suffocate in a wall, burn in daylight, or
		// have a stray fall read as slam damage.
		Mob bystander = spawnEntity(helper, EntityType.HUSK,
				player.getX() + 5.0, player.getY() + 4.0, player.getZ());
		bystander.setNoAi(true);
		bystander.setNoGravity(true);

		TestPlayers.awaitVisible(helper, warden,
				() -> TestPlayers.awaitVisible(helper, bystander, () -> {
					player.invulnerableTime = 0;
					helper.assertTrue(
							player.hurt(helper.getLevel().damageSources().mobAttack(warden), 5f),
							"the warden hit registers as last damager");
					player.setInvulnerable(true);
					AbilityRouter.activate(player, COUNTER_STRIKE);

					// Measure the bystander across the counter's own runtime: the
					// window opens just before the earliest possible slam and closes
					// when the counter finishes, so a stray blast from a neighbouring
					// test outside the window cannot read as counter damage.
					helper.runAfterDelay(40, () -> {
						float pre = bystander.getHealth();
						awaitCounterDone(helper, warden, 150, () -> {
							helper.assertTrue(
									Math.abs(bystander.getHealth() - pre) < 0.001f,
									"the slam is visual-only — no damage while the counter ran, dealt="
											+ (pre - bystander.getHealth())
											+ " src=" + bystander.getLastDamageSource());
							TestPlayers.leave(player);
							helper.succeed();
						});
					});
				}));
	}

	/**
	 * The single hit lands on the ARRIVE→SLAM contact — after 20 lift ticks plus
	 * 20 arrive ticks — not earlier. Before tick ~39 of the sequence the victim
	 * is still at full health.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void counterImpactMatchesAnimationEvent(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		Warden warden = spawnEntity(helper, EntityType.WARDEN,
				player.getX() + 2.0, player.getY(), player.getZ());
		warden.setNoAi(true);

		TestPlayers.awaitVisible(helper, warden, () -> {
			player.invulnerableTime = 0;
			helper.assertTrue(
					player.hurt(helper.getLevel().damageSources().mobAttack(warden), 5f),
					"the warden hit registers as last damager");
			player.setInvulnerable(true);
			AbilityRouter.activate(player, COUNTER_STRIKE);

			// The contact hit lands on the ARRIVE→SLAM transition: the counter
			// strike damage type only exists there — no earlier source can fake it.
			awaitCounterHit(helper, warden, 160, () -> {
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * The crater shrank to a radius-3 cone, 8 blocks deep: the surface cell next
	 * to the impact is carved, the ring at horizontal distance 4 survives, and
	 * nothing below depth 8 is touched.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void craterShrunk(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		player.setAttached(RegulusAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true));

		// A solid stone pad under the slam zone: the victim lifts 30 blocks up and
		// falls straight back onto it.
		BlockPos feet = BlockPos.containing(player.getX() + 2.0, player.getY(), player.getZ());
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-5, -9, -5), feet.offset(5, -1, 5))) {
			helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
		}
		Warden warden = spawnEntity(helper, EntityType.WARDEN,
				feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		warden.setNoAi(true);

		TestPlayers.awaitVisible(helper, warden, () -> {
			player.invulnerableTime = 0;
			helper.assertTrue(
					player.hurt(helper.getLevel().damageSources().mobAttack(warden), 5f),
					"the warden hit registers as last damager");
			player.setInvulnerable(true);
			AbilityRouter.activate(player, COUNTER_STRIKE);

			awaitCounterDone(helper, warden, 220, () -> {
				// Pin the crater's own cone with a controlled carve: a victim parked
				// in a non-ticking pocket slams wherever it hovers, so the radius and
				// depth must be verified on the carver, not the landing spot. The
				// impact is lifted 8 above the floor so the whole 8-deep cone (and
				// the boundary layer under it) sits inside the world.
				BlockPos impact = feet.above(8);
				// Re-pave the measurement cells in the same tick as the carve — a
				// stray blast from a neighbouring test cannot then fake a wider cone
				// or a deeper shaft.
				for (BlockPos p : BlockPos.betweenClosed(
						impact.offset(-4, -1, -4), impact.offset(4, -1, 4))) {
					helper.getLevel().setBlock(p, Blocks.STONE.defaultBlockState(), 3);
				}
				for (BlockPos p : BlockPos.betweenClosed(
						impact.below(7), impact.below(9))) {
					helper.getLevel().setBlock(p, Blocks.STONE.defaultBlockState(), 3);
				}
				RegulusMadnessController.carveCrater(helper.getLevel(), impact, player);
				helper.assertTrue(helper.getLevel().getBlockState(impact.below(1)).isAir(),
						"the crater carves the surface cell of the cone");
				// The old radius-4 cone carved exactly these four axis cells; the
				// radius-3 cone must leave every one of them standing.
				BlockPos[] rim = {
						impact.offset(4, -1, 0), impact.offset(-4, -1, 0),
						impact.offset(0, -1, 4), impact.offset(0, -1, -4),
						impact.offset(3, -1, 3), impact.offset(-3, -1, -3),
						impact.offset(3, -1, -3), impact.offset(-3, -1, 3)};
				for (BlockPos p : rim) {
					helper.assertTrue(helper.getLevel().getBlockState(p).is(Blocks.STONE),
							"the radius-3 crater stops before distance 4, cell=" + p);
				}
				helper.assertTrue(helper.getLevel().getBlockState(impact.below(8)).is(Blocks.STONE),
						"the 8-deep crater stops before the ninth layer below the surface");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * The freeze costs 150 energy: a caster who cannot pay releases the victim
	 * with no lock at all.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void maniaFreezeDeniedAtZeroEnergy(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		// A stray hit cancels the interruptible cast — keep the caster immune so
		// the only outcomes come from the authored energy math.
		player.setInvulnerable(true);
		HeroDataStore.update(player, d -> d.withResources(40f, d.mana()));

		// A husk: daylight cannot burn it and a stray blast cannot kill it before
		// the cast fires — only the authored drain may drop the channel.
		Mob zombie = spawnAhead(helper, player, EntityType.HUSK, 4.0);
		zombie.setNoAi(true);
		zombie.setInvulnerable(true);
		Vec3 home = zombie.position();

		TestPlayers.awaitVisible(helper, zombie, () -> {
			awaitGreedSees(helper, player, zombie, 40, () -> {
				player.lookAt(EntityAnchorArgument.Anchor.EYES,
						zombie.getBoundingBox().getCenter());
				AbilityRouter.activate(player, MANIA_OF_GREED);
				// A stray blast can still knock the invulnerable victim (or the
				// caster) out of the cone during the windup — re-seat both just
				// before the fire tick.
				helper.runAfterDelay(16, () -> reseatVictim(player, zombie, home));

				// The magnet must really start before the drain can collapse it —
				// a whiff would satisfy "not frozen" vacuously.
				awaitMagnet(helper, player, 60, () -> {
					helper.assertTrue(RegulusGreedController.hasMagnet(player),
							"the magnet grabbed the victim," + castDiag(helper, player, zombie));

					// The 8/tick drain collapses the channel after the fire tick; the
					// freeze charge (150) fails too — the victim just walks. Wait on
					// the toggle instead of a fixed delay: the activation lands
					// whenever the visibility poll returns.
					awaitNotActive(helper, player, 80, () -> {
						helper.assertFalse(RegulusGreedController.isFrozen(zombie),
								"no freeze without the 150-energy payment");
						TestPlayers.leave(player);
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Release damage is capped per thaw: a mob victim pays at most 60% of max
	 * health no matter how much the freeze queued. Three 8-damage hits on a
	 * 20-HP zombie release as 12, not 24.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void maniaReleaseCapRespected(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		player.setInvulnerable(true);

		// A husk: daylight cannot burn it during the 200-tick freeze, and no
		// gravity so the thawed victim stays put — the measured release damage
		// is only the capped aggregate, not a fall or a burn.
		Mob zombie = spawnAhead(helper, player, EntityType.HUSK, 4.0);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		// Immune until the freeze lands: a stray blast must not kill or knock the
		// victim out of the magnet before release. Cleared once frozen so the
		// queued hits reach the arbiter.
		zombie.setInvulnerable(true);
		BlockPos feet = zombie.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, -1, 1))) {
			helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
		}
		zombie.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0f, 0f);
		zombie.setDeltaMovement(Vec3.ZERO);
		Vec3 home = zombie.position();

		TestPlayers.awaitVisible(helper, zombie, () -> {
			awaitGreedSees(helper, player, zombie, 40, () -> {
				player.lookAt(EntityAnchorArgument.Anchor.EYES,
						zombie.getBoundingBox().getCenter());
				AbilityRouter.activate(player, MANIA_OF_GREED);
				helper.runAfterDelay(16, () -> reseatVictim(player, zombie, home));

				awaitMagnet(helper, player, 60, () -> {
					helper.assertTrue(RegulusGreedController.hasMagnet(player),
							"the magnet grabbed the victim," + castDiag(helper, player, zombie));
					AbilityRouter.deactivate(player, MANIA_OF_GREED);
					helper.assertTrue(RegulusGreedController.isFrozen(zombie),
							"the victim is frozen after a paid release");
					zombie.setInvulnerable(false);

					for (int i = 0; i < 3; i++) {
						helper.assertFalse(
								zombie.hurt(helper.getLevel().damageSources().magic(), 8f),
								"a frozen victim queues damage instead of taking it");
					}

					helper.runAfterDelay(215, () -> {
						float dealt = zombie.getMaxHealth() - zombie.getHealth();
						helper.assertTrue(Math.abs(dealt - 12f) < 0.5f,
								"the release paid at most 60% of max health, dealt=" + dealt);
						TestPlayers.leave(player);
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * The magnet does not choose a target before the authored fire tick: through
	 * the 19-tick windup there is no magnet and no cooldown; both appear only
	 * once the cast fires.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void maniaDoesNotTargetBefore19t(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		player.setInvulnerable(true);

		// Husk + invulnerable: the victim must still be in the cone at the fire
		// tick — a burn or a stray blast must not remove it mid-windup.
		Mob zombie = spawnAhead(helper, player, EntityType.HUSK, 4.0);
		zombie.setNoAi(true);
		zombie.setInvulnerable(true);
		Vec3 home = zombie.position();

		TestPlayers.awaitVisible(helper, zombie, () -> {
			awaitGreedSees(helper, player, zombie, 40, () -> {
				player.lookAt(EntityAnchorArgument.Anchor.EYES,
						zombie.getBoundingBox().getCenter());
				long t0 = helper.getLevel().getGameTime();
				AbilityRouter.activate(player, MANIA_OF_GREED);
				helper.runAfterDelay(16, () -> reseatVictim(player, zombie, home));
				helper.assertTrue(HeroDataStore.get(player).isActive(MANIA_OF_GREED),
						"the toggle is on — the cast is winding up");
				helper.assertFalse(RegulusGreedController.hasMagnet(player),
						"no magnet on the activation tick");

				helper.runAfterDelay(10, () -> {
					helper.assertFalse(RegulusGreedController.hasMagnet(player),
							"still no magnet mid-windup");
					helper.assertFalse(AbilityCooldowns.isOnCooldown(player, MANIA_OF_GREED),
							"no cooldown before the fire tick");
				});
				// The pin is the fire tick, not the grab: a whiff or a same-tick
				// victim drop also arms the authored cooldown, so wait on either
				// evidence of the cast machine firing — the magnet itself or the
				// cooldown only the fire tick sets.
				awaitCastFired(helper, player, 60, () -> {
					long fired = helper.getLevel().getGameTime() - t0;
					helper.assertTrue(fired >= 19 && fired <= 45,
							"the cast fired on the authored tick, fired=" + fired
									+ " " + castDiag(helper, player, zombie));
					helper.assertTrue(AbilityCooldowns.isOnCooldown(player, MANIA_OF_GREED),
							"the 500-tick cooldown armed on the fire tick");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Polls the same spatial query shape {@code ManiaOfGreedAbility.findTarget}
	 * runs — a fresh mob can be index-visible a tick or two before the entity
	 * section serves area scans.
	 */
	private static void awaitGreedSees(GameTestHelper helper, ServerPlayer player, LivingEntity victim,
			int tries, Runnable body) {
		Vec3 look = player.getViewVector(1.0f);
		AABB box = player.getBoundingBox().expandTowards(look.scale(100.0)).inflate(1.5);
		if (tries <= 0
				|| helper.getLevel().getEntities(player, box, e -> e == victim).contains(victim)) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitGreedSees(helper, player, victim, tries - 1, body));
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player, double distance) {
		return spawnAhead(helper, player, EntityType.ZOMBIE, distance);
	}

	private static <T extends Entity> T spawnAhead(GameTestHelper helper, ServerPlayer player,
			EntityType<T> type, double distance) {
		Vec3 ahead = player.position().add(player.getViewVector(1f).normalize().scale(distance));
		return spawnEntity(helper, type, ahead.x, player.getY(), ahead.z);
	}

	/** Polls until the mania magnet exists on the caster (or tries run out). */
	private static void awaitMagnet(GameTestHelper helper, ServerPlayer player,
			int tries, Runnable body) {
		if (RegulusGreedController.hasMagnet(player) || tries <= 0) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitMagnet(helper, player, tries - 1, body));
	}

	/**
	 * Polls until the mania cast's fire tick produces observable evidence — the
	 * magnet grab or the 500-tick cooldown it arms even on a whiff.
	 */
	private static void awaitCastFired(GameTestHelper helper, ServerPlayer player,
			int tries, Runnable body) {
		if (RegulusGreedController.hasMagnet(player)
				|| AbilityCooldowns.isOnCooldown(player, MANIA_OF_GREED) || tries <= 0) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitCastFired(helper, player, tries - 1, body));
	}

	/**
	 * Cast-machine and victim state for failure messages — discriminates a
	 * whiff, a same-tick magnet drop and a never-fired cast without rerunning.
	 */
	private static String castDiag(GameTestHelper helper, ServerPlayer player,
			LivingEntity victim) {
		return "casting=" + RegulusCastState.isCasting(player, MANIA_OF_GREED)
				+ " fired=" + RegulusCastState.hasFired(player, MANIA_OF_GREED)
				+ " cd=" + AbilityCooldowns.isOnCooldown(player, MANIA_OF_GREED)
				+ " active=" + HeroDataStore.get(player).isActive(MANIA_OF_GREED)
				+ " magnet=" + RegulusGreedController.hasMagnet(player)
				+ " victimAlive=" + victim.isAlive()
				+ " victimDist=" + String.format(java.util.Locale.ROOT, "%.1f",
						victim.distanceTo(player))
				+ " " + scanDiag(helper, player, victim);
	}

	/**
	 * Replicates {@code ManiaOfGreedAbility.findTarget} for failure messages —
	 * reports which stage (clip, box, hostile filter, cone) rejects every entity
	 * the scan can see.
	 */
	private static String scanDiag(GameTestHelper helper, ServerPlayer player,
			LivingEntity victim) {
		Vec3 eyes = player.getEyePosition(1.0f);
		Vec3 look = player.getViewVector(1.0f);
		Vec3 end = eyes.add(look.scale(100.0));
		HitResult blockHit = helper.getLevel().clip(new ClipContext(eyes, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDistSq = blockHit.getType() == HitResult.Type.MISS
				? 10000.0 : blockHit.getLocation().distanceToSqr(eyes);
		AABB box = player.getBoundingBox().expandTowards(look.scale(100.0)).inflate(1.5);
		java.util.Locale loc = java.util.Locale.ROOT;
		StringBuilder sb = new StringBuilder("scan{block=").append(blockHit.getType())
				.append("@").append(String.format(loc, "%.1f", Math.sqrt(maxDistSq)));
		for (Entity e : helper.getLevel().getEntities(player, box,
				en -> en instanceof LivingEntity)) {
			LivingEntity le = (LivingEntity) e;
			Vec3 pos = le.getBoundingBox().getCenter();
			double along = pos.subtract(eyes).dot(look);
			Vec3 closest = eyes.add(look.scale(Math.max(along, 0.0)));
			sb.append(" [").append(le.getType().toShortString())
					.append(e == victim ? "*" : "")
					.append(" d=").append(String.format(loc, "%.1f",
							Math.sqrt(pos.distanceToSqr(eyes))))
					.append(" along=").append(String.format(loc, "%.1f", along))
					.append(" off=").append(String.format(loc, "%.2f",
							Math.sqrt(closest.distanceToSqr(pos))))
					.append(" rad=").append(String.format(loc, "%.2f",
							le.getBoundingBox().getSize() * 0.6))
					.append(" host=").append(TargetFilters.hostileTo(player).test(le))
					.append("]");
		}
		return sb.append("}").toString();
	}

	/** Polls until the mania toggle drops (drain collapse, deactivate, abort). */
	private static void awaitNotActive(GameTestHelper helper, ServerPlayer player,
			int tries, Runnable body) {
		if (!HeroDataStore.get(player).isActive(MANIA_OF_GREED) || tries <= 0) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitNotActive(helper, player, tries - 1, body));
	}

	/**
	 * Polls until the counter session drops the victim — which only happens
	 * after {@code finalSlam} (or an abort). Fixed delays race with how late
	 * the activation lands, so the asserts wait on the session itself.
	 */
	private static void awaitCounterDone(GameTestHelper helper, LivingEntity victim,
			int tries, Runnable body) {
		if (!RegulusMadnessController.isCounterInvolved(victim) || tries <= 0) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitCounterDone(helper, victim, tries - 1, body));
	}

	/**
	 * Polls until the counter's single {@code counter_strike} hit lands — its
	 * damage type is only produced on the ARRIVE→SLAM transition, so catching
	 * it pins the timing by construction.
	 */
	private static void awaitCounterHit(GameTestHelper helper, LivingEntity victim,
			int tries, Runnable body) {
		DamageSource src = victim.getLastDamageSource();
		if (src != null && src.is(RegulusDamageTypes.COUNTER_STRIKE)) {
			body.run();
			return;
		}
		if (tries <= 0 || !RegulusMadnessController.isCounterInvolved(victim)) {
			helper.fail("the counter contact hit never landed, src=" + src
					+ " involved=" + RegulusMadnessController.isCounterInvolved(victim));
			return;
		}
		helper.runAfterDelay(1, () -> awaitCounterHit(helper, victim, tries - 1, body));
	}

	/**
	 * Like {@link #awaitCounterHit} but reports the health the victim lost in
	 * the hit's own tick — the authored fall damage that follows is excluded.
	 */
	private static void awaitCounterHitDamage(GameTestHelper helper, LivingEntity victim,
			float prevHealth, int tries, java.util.function.DoubleConsumer onDealt) {
		DamageSource src = victim.getLastDamageSource();
		float now = victim.getHealth();
		if (src != null && src.is(RegulusDamageTypes.COUNTER_STRIKE)) {
			onDealt.accept(prevHealth - now);
			return;
		}
		if (tries <= 0 || !RegulusMadnessController.isCounterInvolved(victim)) {
			helper.fail("the counter contact hit never landed, src=" + src
					+ " involved=" + RegulusMadnessController.isCounterInvolved(victim));
			return;
		}
		helper.runAfterDelay(1, () -> awaitCounterHitDamage(helper, victim, now, tries - 1, onDealt));
	}

	/**
	 * Puts the victim back on its authored spot and re-aims the caster — a
	 * stray blast can knock either side out of the scan cone mid-windup, so
	 * the scan inputs are restored three ticks before the fire tick.
	 */
	private static void reseatVictim(ServerPlayer player, Mob victim, Vec3 home) {
		victim.moveTo(home.x, home.y, home.z, 0f, 0f);
		victim.setDeltaMovement(Vec3.ZERO);
		player.lookAt(EntityAnchorArgument.Anchor.EYES,
				victim.getBoundingBox().getCenter());
	}

	private static <T extends Entity> T spawnEntity(GameTestHelper helper, EntityType<T> type,
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
