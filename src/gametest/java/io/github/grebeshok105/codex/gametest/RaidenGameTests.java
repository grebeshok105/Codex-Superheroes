package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.raiden.RaidenAttachments;
import io.github.grebeshok105.codex.hero.raiden.RaidenItems;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenSwordDrawAbility;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.hero.raiden.item.MusouNoHitotachiItem;
import io.github.grebeshok105.codex.hero.raiden.runtime.HeavensStrikeController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenModifiers;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenMusouIsshinController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Stage I4c characterization: Raiden's observable server behavior on the pre-move
 * layout — the heavens-strike queue behind Plunging Strike (caster lock, freeze
 * bundle, 80-tick windup, crater + damage detonation, cancel/leave drops), the
 * Musou Isshin pending slash (hostile freeze during windup, Yamato/hero drop
 * conditions), the Musou Shinsetsu burst-window counters (expire + final slash),
 * the Transcendence aura zap cadence, sword-draw issue/revoke and the transient
 * RaidenState attachment. Ability and item ids are literals because the constants
 * move into the hero module with it.
 *
 * <p>The pending queues tick off {@code level.getGameTime()} through the real
 * dispatcher, so windup waits go through {@code runAfterDelay}/{@code awaitTrue}
 * instead of synchronous tick loops. Husks stand in for zombies on the long
 * waits so daylight can't interfere with the damage assertions.
 */
public final class RaidenGameTests implements FabricGameTest {
	private static final ResourceLocation RAIDEN = ModId.of("raiden_shogun");
	private static final ResourceLocation SWORD_DRAW = ModId.of("raiden_sword_draw");
	private static final ResourceLocation EYE_OF_JUDGMENT = ModId.of("raiden_eye_of_judgment");
	private static final ResourceLocation MUSOU_SHINSETSU = ModId.of("raiden_musou_shinsetsu");
	private static final ResourceLocation MUSOU_ISSHIN = ModId.of("raiden_musou_isshin");
	private static final ResourceLocation PLUNGING_STRIKE = ModId.of("raiden_plunging_strike");
	private static final ResourceLocation TRANSCENDENCE = ModId.of("raiden_transcendence");

	private static void grantEnergy(ServerPlayer player) {
		HeroDataStore.update(player, d -> d.withResources(1500f, 0f));
	}

	private static RaidenState raidenState(ServerPlayer player) {
		return player.getAttachedOrCreate(RaidenAttachments.STATE);
	}

	private static boolean isActive(ServerPlayer player, ResourceLocation abilityId) {
		return HeroDataStore.get(player).isActive(abilityId);
	}

	private static boolean hurtBy(ServerPlayer player, LivingEntity victim) {
		return victim.getLastDamageSource() != null
				&& victim.getLastDamageSource().getEntity() == player;
	}

	/**
	 * Puts the player inside this test's own structure so nearest-hostile scans
	 * cannot pick up leftovers another test left near the shared world spawn.
	 */
	private static void teleportIntoStructure(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = helper.absolutePos(BlockPos.containing(1, 1, 1));
		player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	private static <T extends Mob> T spawnNoAi(ServerLevel level, EntityType<T> type, Vec3 pos) {
		T entity = type.create(level);
		if (entity == null) {
			throw new IllegalStateException("could not create " + type);
		}
		entity.setNoAi(true);
		entity.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
		level.addFreshEntity(entity);
		return entity;
	}

	/** Polls once per game tick until {@code cond} holds or {@code tries} run out, then runs {@code body}. */
	private static void awaitTrue(GameTestHelper helper, BooleanSupplier cond, int tries, Runnable body) {
		if (tries <= 0 || cond.getAsBoolean()) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitTrue(helper, cond, tries - 1, body));
	}

	/** The module registers exactly these six abilities, in this slot order. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void raidenHeroExposesItsSixAbilities(GameTestHelper helper) {
		Hero hero = Heroes.get(RAIDEN);
		helper.assertTrue(hero != null, "raiden_shogun hero registered");
		helper.assertTrue(hero.getAbilities().equals(List.of(
						SWORD_DRAW, EYE_OF_JUDGMENT, MUSOU_SHINSETSU,
						MUSOU_ISSHIN, PLUNGING_STRIKE, TRANSCENDENCE)),
				"raiden ability set and slot order");
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	/** Manifest Yamato toggle: activation issues the bound sword and flags
	 * {@code swordDrawn}; a second activation revokes the sword and clears it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void swordDrawIssuesAndRevokesYamato(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);

		AbilityRouter.activate(player, SWORD_DRAW);
		helper.assertTrue(isActive(player, SWORD_DRAW), "sword draw toggled on");
		helper.assertTrue(player.getMainHandItem().getItem() instanceof MusouNoHitotachiItem,
				"Yamato lands in the main hand");
		helper.assertTrue(TestPlayers.count(player, RaidenItems.MUSOU_NO_HITOTACHI) == 1,
				"exactly one Yamato issued");
		helper.assertTrue(raidenState(player).swordDrawn(), "state flags the drawn sword");

		AbilityRouter.activate(player, SWORD_DRAW);
		helper.assertTrue(!isActive(player, SWORD_DRAW), "second activation toggles it off");
		helper.assertTrue(TestPlayers.count(player, RaidenItems.MUSOU_NO_HITOTACHI) == 0,
				"deactivation revokes Yamato");
		helper.assertTrue(!raidenState(player).swordDrawn(), "state cleared");

		TestPlayers.leave(player);
		helper.succeed();
	}

	/** Same toggle with a full inventory: the bound weapon has nowhere to go and
	 * the activation fails without marking the ability or touching the state. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void swordDrawDeniedWhenInventoryFull(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		TestPlayers.fillInventory(player);

		AbilityRouter.activate(player, SWORD_DRAW);
		helper.assertTrue(!isActive(player, SWORD_DRAW), "no room -> no activation");
		helper.assertTrue(TestPlayers.count(player, RaidenItems.MUSOU_NO_HITOTACHI) == 0, "no Yamato");
		helper.assertTrue(!raidenState(player).swordDrawn(), "state untouched");

		TestPlayers.leave(player);
		helper.succeed();
	}

	/** Eye of Judgment is gated on holding Yamato; once active it carries a
	 * 500-tick timer that deactivates the toggle on its own when it expires. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void eyeOfJudgmentNeedsYamatoAndAutoExpires(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		grantEnergy(player);

		AbilityRouter.activate(player, EYE_OF_JUDGMENT);
		helper.assertTrue(!isActive(player, EYE_OF_JUDGMENT), "eye blocked without Yamato in hand");

		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato issued");
		AbilityRouter.activate(player, EYE_OF_JUDGMENT);
		long now = helper.getLevel().getGameTime();
		helper.assertTrue(isActive(player, EYE_OF_JUDGMENT), "eye toggled on with Yamato");
		helper.assertTrue(raidenState(player).eyeExpireTick() - now == 500L,
				"eye timer armed at +500 ticks");

		// Fast-forward the expiry field: the active tick must drop the toggle itself.
		player.setAttached(RaidenAttachments.STATE,
				raidenState(player).withEyeExpireTick(now + 1));
		helper.runAfterDelay(4, () -> {
			helper.assertTrue(!isActive(player, EYE_OF_JUDGMENT), "expired eye deactivates itself");
			helper.assertTrue(raidenState(player).eyeExpireTick() == 0L, "deactivation clears the timer");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/** Musou Shinsetsu arms both burst counters at +140 ticks and applies the
	 * burst modifiers; on that tick the final slash hits hostiles in radius 8
	 * and the modifiers come off. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void musouShinsetsuTimesFinalSlashAndStripsModifiers(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato issued");
		grantEnergy(player);

		helper.setBlock(new BlockPos(4, 0, 1), Blocks.STONE);
		BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 1));
		Husk husk = spawnNoAi(helper.getLevel(), EntityType.HUSK, Vec3.atBottomCenterOf(pos));

		TestPlayers.awaitVisible(helper, husk, () -> {
			long now = helper.getLevel().getGameTime();
			AbilityRouter.activate(player, MUSOU_SHINSETSU);
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, MUSOU_SHINSETSU),
					"burst sets its 25s cooldown");
			RaidenState armed = raidenState(player);
			helper.assertTrue(armed.burstExpireTick() - now == 140L
							&& armed.burstFinalSlashTick() - now == 140L,
					"burst window and final slash both armed at +140");
			helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
							.getModifier(RaidenModifiers.RAIDEN_BURST_DAMAGE) != null,
					"burst attack-damage modifier applied");
			helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED)
							.getModifier(RaidenModifiers.RAIDEN_BURST_SPEED) != null,
					"burst speed modifier applied");

			helper.runAfterDelay(142, () -> {
				helper.assertTrue(hurtBy(player, husk), "final slash hits the mob at expire");
				RaidenState done = raidenState(player);
				helper.assertTrue(done.burstExpireTick() == 0L && done.burstFinalSlashTick() == 0L,
						"burst counters cleared after the slash");
				helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE)
								.getModifier(RaidenModifiers.RAIDEN_BURST_DAMAGE) == null,
						"burst modifiers stripped on expiry");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/** The final slash's player branch: 22 damage instead of 12. The gametest
	 * server runs with pvp off, so enable it for this test only and restore it —
	 * the flag is global to every concurrent test in the batch. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void musouShinsetsuFinalSlashUsesPlayerDamageOnPlayers(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		ServerPlayer victim = TestPlayers.join(helper, "raiden-pvp-victim");
		teleportIntoStructure(helper, player);
		BlockPos vpos = helper.absolutePos(new BlockPos(4, 1, 1));
		victim.teleportTo(vpos.getX() + 0.5, vpos.getY(), vpos.getZ() + 0.5);
		TestHeroes.transform(player, RAIDEN);
		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato issued");
		grantEnergy(player);
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.getLevel().getServer().setPvpAllowed(true);
		TestPlayers.clearSpawnInvulnerability(victim);

		TestPlayers.awaitVisible(helper, victim, () -> {
			AbilityRouter.activate(player, MUSOU_SHINSETSU);
			helper.runAfterDelay(142, () -> {
				try {
					helper.assertTrue(hurtBy(player, victim),
							"the 22-damage player branch of the final slash lands");
					helper.assertTrue(!victim.isAlive(), "22 damage kills a survival victim");
				} finally {
					helper.getLevel().getServer().setPvpAllowed(oldPvp);
				}
				TestPlayers.leave(victim);
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/** Transcendence marks the aura flag {@code Long.MAX_VALUE} until manual off;
	 * the aura ticks the nearest hostile in radius 6 every 30 ticks. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
	public void transcendenceAuraZapsNearestHostile(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		grantEnergy(player);

		helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
		BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 1));
		Husk husk = spawnNoAi(helper.getLevel(), EntityType.HUSK, Vec3.atBottomCenterOf(pos));

		TestPlayers.awaitVisible(helper, husk, () -> {
			AbilityRouter.activate(player, TRANSCENDENCE);
			helper.assertTrue(isActive(player, TRANSCENDENCE), "transcendence toggled on");
			helper.assertTrue(raidenState(player).transcendenceUntilTick() == Long.MAX_VALUE,
					"aura flag rides until manual off");

			awaitTrue(helper, () -> hurtBy(player, husk), 80, () -> {
				helper.assertTrue(hurtBy(player, husk),
						"aura zap lands the periodic playerAttack hit");
				AbilityRouter.deactivate(player, TRANSCENDENCE);
				helper.assertTrue(raidenState(player).transcendenceUntilTick() == 0L,
						"manual off clears the aura flag");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/** Musou Isshin: the queued slash freezes the caster (amp 8) and every hostile
	 * in radius 50 (amp 9 bundle) for the 60-tick windup, then hits along the look
	 * lane; the freeze stops refreshing once the charge resolves. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void musouIsshinWindupFreezesThenSlashes(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		player.setYRot(0f);
		player.setXRot(0f);
		TestHeroes.transform(player, RAIDEN);
		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato issued");
		grantEnergy(player);

		// Ravager survives the 28-damage slash so the freeze purge is observable.
		helper.setBlock(new BlockPos(1, 0, 7), Blocks.STONE);
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 7));
		Ravager ravager = spawnNoAi(helper.getLevel(), EntityType.RAVAGER, Vec3.atBottomCenterOf(pos));

		TestPlayers.awaitVisible(helper, ravager, () -> {
			AbilityRouter.activate(player, MUSOU_ISSHIN);
			helper.assertTrue(RaidenMusouIsshinController.isCharging(player),
					"isshin queues a pending slash");
			helper.assertTrue(!RaidenMusouIsshinController.start(player),
					"a second start is refused while charging");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, MUSOU_ISSHIN),
					"isshin sets its 45s cooldown");
			helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null
							&& player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 8,
					"caster freeze lands at amp 8");
			helper.assertTrue(ravager.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null
							&& ravager.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 9,
					"hostiles in 50 get the amp-9 freeze");
			helper.assertTrue(ravager.hasEffect(MobEffects.WEAKNESS)
							&& ravager.hasEffect(MobEffects.DIG_SLOWDOWN),
					"freeze bundle includes weakness + dig slowdown");

			awaitTrue(helper, () -> !RaidenMusouIsshinController.isCharging(player), 90, () -> {
				helper.assertTrue(hurtBy(player, ravager), "the slash hits along the look lane");
				helper.runAfterDelay(30, () -> {
					helper.assertTrue(!ravager.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
							"freeze stops refreshing once the charge resolves");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/** The pending slash is keyed on Yamato staying in hand: emptying the main
	 * hand drops the pending entry and no slash resolves. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void musouIsshinChargeDropsWhenYamatoLeavesHands(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		player.setYRot(0f);
		player.setXRot(0f);
		TestHeroes.transform(player, RAIDEN);
		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato issued");
		grantEnergy(player);

		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 7));
		Husk husk = spawnNoAi(helper.getLevel(), EntityType.HUSK, Vec3.atBottomCenterOf(pos));

		TestPlayers.awaitVisible(helper, husk, () -> {
			AbilityRouter.activate(player, MUSOU_ISSHIN);
			helper.assertTrue(RaidenMusouIsshinController.isCharging(player), "charging");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			helper.runAfterDelay(70, () -> {
				helper.assertTrue(!RaidenMusouIsshinController.isCharging(player),
						"pending drops once Yamato leaves the hands");
				helper.assertTrue(husk.getLastDamageSource() == null,
						"no slash resolves after the drop");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/** Losing the hero drops the pending slash as well (the isRaiden gate in the
	 * queue drain), and the lifecycle clear resets the state attachment. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void musouIsshinChargeDropsOnUntransform(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		helper.assertTrue(RaidenSwordDrawAbility.giveSword(player), "Yamato issued");
		grantEnergy(player);

		AbilityRouter.activate(player, MUSOU_ISSHIN);
		helper.assertTrue(RaidenMusouIsshinController.isCharging(player), "charging");
		HeroTransformService.forceUntransform(player);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(!RaidenMusouIsshinController.isCharging(player),
					"untransform drops the charge");
			helper.assertTrue(RaidenState.EMPTY.equals(raidenState(player)),
					"hero clear resets RaidenState");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	/** Plunging Strike queues a heavens strike with Variant.RAIDEN: the caster is
	 * re-locked to the queued spot for the 80-tick windup, then the detonation
	 * hits hostiles within radius+2, carves the ground below (UNBREAKABLE blocks
	 * survive) and clears the freeze bundle. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void heavensStrikeLocksCasterAndDetonatesOnWindup(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		for (int x = 2; x <= 6; x++) {
			for (int z = 2; z <= 6; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		helper.setBlock(new BlockPos(6, 1, 6), Blocks.OBSIDIAN);
		BlockPos ppos = helper.absolutePos(new BlockPos(4, 2, 4));
		player.teleportTo(ppos.getX() + 0.5, ppos.getY(), ppos.getZ() + 0.5);
		TestHeroes.transform(player, RAIDEN);
		grantEnergy(player);

		BlockPos hpos = helper.absolutePos(new BlockPos(6, 2, 4));
		Husk husk = spawnNoAi(helper.getLevel(), EntityType.HUSK, Vec3.atBottomCenterOf(hpos));

		TestPlayers.awaitVisible(helper, husk, () -> {
			Vec3 lock = player.position();
			AbilityRouter.activate(player, PLUNGING_STRIKE);
			helper.assertTrue(HeavensStrikeController.isCharging(player),
					"plunging strike queues a heavens strike");
			helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null
							&& player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 6,
					"caster freeze lands at amp 6");
			helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS)
							&& player.hasEffect(MobEffects.DIG_SLOWDOWN),
					"freeze bundle applied");
			helper.assertTrue(!HeavensStrikeController.start(player, HeavensStrikeController.Variant.RAIDEN),
					"second start refused while pending");
			helper.assertTrue(!AbilityRegistry.get(PLUNGING_STRIKE).canActivate(player),
					"plunging strike gate denies while charging");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, PLUNGING_STRIKE),
					"18s cooldown armed");

			// The lock teleports a drifting caster back to the queued position.
			player.teleportTo(lock.x + 1.5, lock.y, lock.z);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(player.position().distanceToSqr(lock) < 0.05,
						"caster re-locked to the queued spot");

				awaitTrue(helper, () -> !HeavensStrikeController.isCharging(player), 110, () -> {
					helper.assertTrue(hurtBy(player, husk), "60-damage detonation hits the mob");
					helper.assertBlockNotPresent(Blocks.STONE, new BlockPos(4, 1, 4));
					helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(6, 1, 6));
					helper.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
							"freeze cleared on impact");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/** {@code cancel} drops the pending entry so no detonation lands — but the
	 * freeze bundle is left to decay on its own (24-tick duration). */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void heavensStrikeCancelSkipsTheDetonation(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		grantEnergy(player);

		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 1));
		Husk husk = spawnNoAi(helper.getLevel(), EntityType.HUSK, Vec3.atBottomCenterOf(pos));

		TestPlayers.awaitVisible(helper, husk, () -> {
			AbilityRouter.activate(player, PLUNGING_STRIKE);
			helper.assertTrue(HeavensStrikeController.isCharging(player), "charging");
			helper.runAfterDelay(3, () -> {
				HeavensStrikeController.cancel(player.getUUID());
				helper.assertTrue(!HeavensStrikeController.isCharging(player),
						"cancel drops the pending entry");
				helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
						"cancel does not strip the freeze — it decays naturally");

				helper.runAfterDelay(85, () -> {
					helper.assertTrue(husk.getLastDamageSource() == null,
							"no impact resolves after cancel");
					helper.assertTrue(!HeavensStrikeController.isCharging(player),
							"cancel is final — the entry is never re-armed");
					// Freeze decay itself is unverifiable here: foreign amp-6 sources
					// (RulersAuthority, RemDemonism, Reinhard ceremony) refresh SLOWDOWN
					// on any harmable player in the shared level.
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/** The pending queue resolves owners through the player list: a caster that
	 * leaves drops the entry, and relogging under the same UUID does not resume it. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void heavensStrikePendingDropsOnRelog(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		grantEnergy(player);

		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 1));
		Husk husk = spawnNoAi(helper.getLevel(), EntityType.HUSK, Vec3.atBottomCenterOf(pos));

		TestPlayers.awaitVisible(helper, husk, () -> {
			AbilityRouter.activate(player, PLUNGING_STRIKE);
			helper.assertTrue(HeavensStrikeController.isCharging(player), "charging");
			TestPlayers.leave(player);
			helper.runAfterDelay(3, () -> {
				ServerPlayer back = TestPlayers.rejoin(helper, player);
				helper.assertTrue(!HeavensStrikeController.isCharging(back),
						"pending dropped while the caster was offline");
				helper.runAfterDelay(85, () -> {
					helper.assertTrue(husk.getLastDamageSource() == null,
							"no impact resolves after relog");
					TestPlayers.leave(back);
					helper.succeed();
				});
			});
		});
	}

	/** {@code raidens_state} is deliberately non-persistent: relogging starts the
	 * attachment over from {@link RaidenState#EMPTY}. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void raidenStateResetsAcrossRelog(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		teleportIntoStructure(helper, player);
		TestHeroes.transform(player, RAIDEN);
		player.setAttached(RaidenAttachments.STATE, RaidenState.EMPTY
				.withEyeExpireTick(123L).withSwordDrawn(true).withBurstExpireTick(77L));

		TestPlayers.leave(player);
		helper.runAfterDelay(3, () -> {
			ServerPlayer back = TestPlayers.rejoin(helper, player);
			helper.assertTrue(RaidenState.EMPTY.equals(
							back.getAttachedOrCreate(RaidenAttachments.STATE)),
					"raiden_state starts empty after relog");
			TestPlayers.leave(back);
			helper.succeed();
		});
	}
}
