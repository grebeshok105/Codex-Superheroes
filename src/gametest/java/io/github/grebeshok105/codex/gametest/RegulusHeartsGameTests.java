package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.hero.regulus.RegulusAttachments;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusHearts;
import io.github.grebeshok105.codex.hero.rem.runtime.RemEntities;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Regulus "little king" hearts (rework Task 3): peaceful/neutral vanilla entities within
 * 20 blocks become heart bearers (transient {@code regulus_heart_owner} mark, cap 12),
 * bearer death burns the heart and backlashes the owner for 10% max health through
 * {@code regulus_heart_backlash} + Weakness I, bearer unload loses the heart quietly,
 * and the owner view (bearer ids + lion-heart flag + overheat) syncs only on change.
 */
public class RegulusHeartsGameTests implements FabricGameTest {

	private static final int CAP = 12;

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void heartsMarkOnlyVanillaMobs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 0);
		Pig pigA = spawnEntity(helper, EntityType.PIG, player.getX() + 2.0, player.getY(), player.getZ());
		Pig pigB = spawnEntity(helper, EntityType.PIG, player.getX() - 2.0, player.getY(), player.getZ());

		awaitHearts(helper, player, 2, 80, () -> {
			helper.assertValueEqual(pigA.getAttached(RegulusAttachments.REGULUS_HEART_OWNER),
					player.getUUID(), "the first pig carries this owner's heart mark");
			helper.assertValueEqual(pigB.getAttached(RegulusAttachments.REGULUS_HEART_OWNER),
					player.getUUID(), "the second pig carries this owner's heart mark");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void hostileMobCannotCarryHeart(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 1);
		Zombie zombie = spawnEntity(helper, EntityType.ZOMBIE, player.getX() + 2.0, player.getY(), player.getZ());
		zombie.setNoAi(true);

		// Two full scan windows pass with nothing marked — hostile mobs are never bearers.
		helper.runAfterDelay(45, () -> {
			helper.assertValueEqual(RegulusHearts.count(player), 0, "a zombie is never a heart bearer");
			helper.assertTrue(zombie.getAttached(RegulusAttachments.REGULUS_HEART_OWNER) == null,
					"the zombie carries no heart mark");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
	public void heartsCapAtTwelve(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 2);
		Pig[] pigs = new Pig[CAP + 1];
		for (int i = 0; i < pigs.length; i++) {
			pigs[i] = spawnEntity(helper, EntityType.PIG,
					player.getX() + 1.0 + (i % 4), player.getY(), player.getZ() + 1.0 + (i / 4));
		}

		awaitHearts(helper, player, CAP, 120, () -> {
			int marked = 0;
			for (Pig pig : pigs) {
				if (pig.getAttached(RegulusAttachments.REGULUS_HEART_OWNER) != null) {
					marked++;
				}
			}
			helper.assertValueEqual(marked, CAP, "exactly " + CAP + " bearers marked — the cap holds");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void heartBacklashOnBearerDeath(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 3);
		TestPlayers.clearSpawnInvulnerability(player);
		Pig pig = spawnEntity(helper, EntityType.PIG, player.getX() + 2.0, player.getY(), player.getZ());

		awaitHearts(helper, player, 1, 80, () -> {
			float before = player.getHealth();
			float expectedLoss = player.getMaxHealth() * 0.10f;
			pig.kill();
			helper.assertValueEqual(RegulusHearts.count(player), 0,
					"the heart burns when its bearer dies");
			helper.assertTrue(Math.abs((before - player.getHealth()) - expectedLoss) < 0.001f,
					"the owner takes 10% max-health backlash (expected " + expectedLoss
							+ ", lost " + (before - player.getHealth()) + ")");
			helper.assertTrue(player.getHealth() > 0f, "the backlash alone never kills");
			var weakness = player.getEffect(MobEffects.WEAKNESS);
			helper.assertTrue(weakness != null && weakness.getAmplifier() == 0
							&& weakness.getDuration() <= 60,
					"the backlash applies Weakness I for 60 ticks");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void heartLostQuietlyOnUnload(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 4);
		TestPlayers.clearSpawnInvulnerability(player);
		Pig pig = spawnEntity(helper, EntityType.PIG, player.getX() + 2.0, player.getY(), player.getZ());

		awaitHearts(helper, player, 1, 80, () -> {
			float health = player.getHealth();
			pig.discard();
			awaitTrue(helper, () -> RegulusHearts.count(player) == 0, 60, () -> {
				helper.assertTrue(Math.abs(player.getHealth() - health) < 0.001f,
						"an unloaded bearer loses the heart quietly — no backlash");
				helper.assertFalse(player.hasEffect(MobEffects.WEAKNESS),
						"an unloaded bearer applies no weakness");
				releaseIsolation(helper, player);
			TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void heartRejectsCustomEntity(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 5);
		var ram = spawnEntity(helper, RemEntities.RAM, player.getX() + 2.0, player.getY(), player.getZ());
		ram.setNoAi(true);

		helper.runAfterDelay(45, () -> {
			helper.assertValueEqual(RegulusHearts.count(player), 0,
					"a mod-owned entity type is never a heart bearer (namespace gate)");
			helper.assertTrue(ram.getAttached(RegulusAttachments.REGULUS_HEART_OWNER) == null,
					"the custom entity carries no heart mark");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void armorStandCannotCarryHeart(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 6);
		ArmorStand stand = spawnEntity(helper, EntityType.ARMOR_STAND,
				player.getX() + 2.0, player.getY(), player.getZ());

		helper.runAfterDelay(45, () -> {
			helper.assertValueEqual(RegulusHearts.count(player), 0,
					"an armor stand is heartless (entity tag)");
			helper.assertTrue(stand.getAttached(RegulusAttachments.REGULUS_HEART_OWNER) == null,
					"the armor stand carries no heart mark");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void heartEnergyRegenScales(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 7);
		for (int i = 0; i < 4; i++) {
			spawnEntity(helper, EntityType.PIG, player.getX() + 1.0 + i, player.getY(), player.getZ() + 1.0);
		}

		awaitHearts(helper, player, 4, 100, () -> {
			HeroDataStore.update(player, d -> d.withEnergy(500f));
			float before = HeroDataStore.get(player).energy();
			ResourceController.tick(player);
			float gained = HeroDataStore.get(player).energy() - before;
			helper.assertTrue(Math.abs(gained - (2.0f + 0.15f * 4)) < 0.001f,
					"4 hearts add +0.6 energy per tick on top of the base 2.0 (gained " + gained + ")");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void heartRegenRespectsEnergyLock(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 8);
		for (int i = 0; i < 4; i++) {
			spawnEntity(helper, EntityType.PIG, player.getX() + 1.0 + i, player.getY(), player.getZ() + 1.0);
		}

		awaitHearts(helper, player, 4, 100, () -> {
			EnergyLocks.lockTicks(player, 100);
			HeroDataStore.update(player, d -> d.withEnergy(500f));
			float before = HeroDataStore.get(player).energy();
			ResourceController.tick(player);
			helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - before) < 0.001f,
					"an energy lock suppresses base regen AND the heart bonus");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void heartDamageScalesMelee(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 9);
		for (int i = 0; i < 5; i++) {
			spawnEntity(helper, EntityType.PIG, player.getX() + 1.0 + i, player.getY(), player.getZ() + 1.0);
		}

		awaitHearts(helper, player, 5, 100, () -> {
			AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
			helper.assertTrue(instance != null, "attack-damage attribute exists");
			AttributeModifier modifier = instance.getModifier(ModId.of("modifiers/regulus/hearts_damage"));
			helper.assertTrue(modifier != null
							&& modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
							&& Math.abs(modifier.amount() - 0.10) < 0.001,
					"5 hearts apply a +10% ATTACK_DAMAGE multiply-total modifier");
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void heartOwnerCleanupRemovesBearerMarks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 10);
		Pig pig = spawnEntity(helper, EntityType.PIG, player.getX() + 2.0, player.getY(), player.getZ());

		awaitHearts(helper, player, 1, 80, () -> {
			releaseIsolation(helper, player);
			TestPlayers.leave(player);
			helper.assertTrue(pig.getAttached(RegulusAttachments.REGULUS_HEART_OWNER) == null,
					"leaving strips the heart mark from still-loaded bearers");
			helper.assertValueEqual(RegulusHearts.count(player), 0, "the owner's set is cleared");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void overheatSyncChangesWithoutHeartSetChange(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 11);

		// The first sync lands on the next 20-tick boundary even with an empty heart set.
		awaitTrue(helper, () -> RegulusHearts.syncedOverheatTicks(player) >= 0, 60, () -> {
			helper.assertValueEqual(RegulusHearts.count(player), 0, "precondition: no hearts");
			RegulusHearts.setOverheatTicks(player, 7);
			awaitTrue(helper, () -> RegulusHearts.syncedOverheatTicks(player) == 7, 60, () -> {
				helper.assertValueEqual(RegulusHearts.count(player), 0,
						"the heart set never changed — the dirty rule fired on overheat alone");
				releaseIsolation(helper, player);
			TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 170)
	public void heartsDamageScaleDropsOnHeroClear(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		isolate(player, 12);
		Pig pig = spawnEntity(helper, EntityType.PIG, player.getX() + 2.0, player.getY(), player.getZ());

		awaitHearts(helper, player, 1, 100, () -> {
			// Transform carries a 20-tick cooldown — the untransform must wait it out.
			helper.runAfterDelay(25, () -> {
				AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
				helper.assertTrue(instance != null
								&& instance.getModifier(RegulusHearts.HEARTS_DAMAGE_MODIFIER_ID) != null,
						"precondition: the hearts modifier is applied while hearts are held");
				helper.assertTrue(HeroTransformService.untransform(player),
						"untransform succeeds once the transform cooldown has passed");
				helper.assertTrue(instance.getModifier(RegulusHearts.HEARTS_DAMAGE_MODIFIER_ID) == null,
						"hero clear drops the hearts damage modifier instead of recomputing it");
				helper.assertTrue(pig.getAttached(RegulusAttachments.REGULUS_HEART_OWNER) == null,
						"hero clear strips the mark from still-loaded bearers");
				helper.assertValueEqual(RegulusHearts.count(player), 0, "hero clear empties the owner set");
				releaseIsolation(helper, player);
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	private static void awaitHearts(GameTestHelper helper, ServerPlayer player, int expected,
			int tries, Runnable body) {
		awaitTrue(helper, () -> RegulusHearts.count(player) >= expected, tries, body);
	}

	private static void awaitTrue(GameTestHelper helper, BooleanSupplier cond, int triesLeft,
			Runnable body) {
		helper.runAfterDelay(1, () -> {
			if (cond.getAsBoolean() || triesLeft <= 1) {
				body.run();
				return;
			}
			awaitTrue(helper, cond, triesLeft - 1, body);
		});
	}

	/**
	 * Park the owner far east of the test grid and 160 blocks up: hearts are claimed by
	 * ANY regulus player inside the 20-block aura, and sibling gametests share the
	 * world — without isolation a foreign owner can steal a spawned bearer on the same
	 * global scan tick. The altitude is load-bearing, not cosmetic: the gametest world
	 * is default terrain, and at +40 a wandering animal or river fish can sit inside
	 * the aura and eat a cap slot (cap/negative tests assert exact counts). Midair is
	 * guaranteed empty of eligible bearers.
	 */
	private static void isolate(ServerPlayer player, int slot) {
		player.teleportTo(player.getX() + 512.0 + 96.0 * slot, player.getY() + 160.0, player.getZ());
		player.setNoGravity(true);
	}

	/**
	 * Free the far chunk tickets — leftover bearers unload and quietly lose their hearts.
	 * Bearers spawn offset from the owner and can sit in a neighbouring chunk, so the
	 * 3x3 area around the player is released, not just the player's own chunk.
	 */
	private static void releaseIsolation(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = player.blockPosition();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.getLevel().setChunkForced((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz, false);
			}
		}
	}

	private static <T extends Entity> T spawnEntity(GameTestHelper helper, EntityType<T> type,
			double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		helper.getLevel().setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
		T entity = type.create(helper.getLevel());
		entity.moveTo(x, y, z, 0f, 0f);
		entity.setNoGravity(true);
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			mob.setNoAi(true);
		}
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}
}
