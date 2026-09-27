package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldSlamAbility;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.lifecycle.HeroTickDispatcher;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.captainamerica.entity.ShieldProjectileEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Stage I2c characterization: Captain America's observable behavior on the pre-move
 * layout — shield throw lifecycle (shield-in-hand gate, hand emptying, bounce, return
 * to hand, lifetime expiry), shield slam session timing (launch, ≥4 air ticks gate,
 * detonate on touchdown, 100-tick airborne expiry, clear-on-leave), shield dash sweep,
 * counter-stance toggle, fall immunity and the Thanos SOUL-stone kill reward.
 * Ability, damage-type and item ids are literals because the constants move into the
 * module with it.
 *
 * <p>Slam ticks are driven by calling {@code CapShieldSlamAbility.serverTick(player)}
 * directly so the sequence stays atomic inside one game tick; real dispatcher ticks
 * only run while a test waits for a spawn or the projectile's flight.
 */
public final class CaptainAmericaGameTests implements FabricGameTest {
	private static final ResourceLocation CAPTAIN_AMERICA = ModId.of("captain_america");
	private static final ResourceLocation THANOS = ModId.of("thanos");
	private static final ResourceLocation CAP_SHIELD_THROW = ModId.of("cap_shield_throw");
	private static final ResourceLocation CAP_SHIELD_SLAM = ModId.of("cap_shield_slam");
	private static final ResourceLocation CAP_SHIELD_DASH = ModId.of("cap_shield_dash");
	private static final ResourceLocation CAP_COUNTER_STANCE = ModId.of("cap_counter_stance");
	private static final ResourceLocation VIBRANIUM_SHIELD = ModId.of("vibranium_shield");
	private static final ResourceKey<DamageType> CAP_SHIELD_THROW_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of("cap_shield_throw"));
	private static final ResourceKey<DamageType> CAP_SHIELD_SLAM_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of("cap_shield_slam"));

	private static void grantEnergy(ServerPlayer player) {
		HeroDataStore.update(player, d -> d.withResources(1000f, 0f));
	}

	private static void faceForward(ServerPlayer player) {
		// yaw 0 / pitch 0 — view vector = +Z.
		player.setYRot(0f);
		player.setXRot(0f);
	}

	private static Zombie spawnZombieAhead(ServerPlayer player, double blocksAhead) {
		ServerLevel level = player.serverLevel();
		Zombie zombie = EntityType.ZOMBIE.create(level);
		if (zombie == null) {
			throw new IllegalStateException("could not create zombie");
		}
		zombie.setNoAi(true);
		Vec3 pos = player.position().add(player.getLookAngle().scale(blocksAhead));
		zombie.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
		level.addFreshEntity(zombie);
		return zombie;
	}

	private static boolean hurtBy(ServerPlayer player, LivingEntity victim) {
		return victim.getLastDamageSource() != null
				&& victim.getLastDamageSource().getEntity() == player;
	}

	private static Item vibraniumShield() {
		return BuiltInRegistries.ITEM.get(VIBRANIUM_SHIELD);
	}

	private static Item item(String path) {
		Item item = BuiltInRegistries.ITEM.get(ModId.of(path));
		if (item == Items.AIR) {
			throw new IllegalStateException("unregistered item " + path);
		}
		return item;
	}

	/**
	 * Puts the player inside this test's own structure so nearest-hostile scans
	 * cannot pick up leftovers another test left near the shared world spawn.
	 */
	private static void teleportIntoStructure(GameTestHelper helper, ServerPlayer player) {
		var pos = helper.absolutePos(net.minecraft.core.BlockPos.containing(1, 1, 1));
		player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	private static List<ShieldProjectileEntity> shieldProjectilesNear(ServerPlayer player, double radius) {
		// Filter by owner: concurrent cap tests spawn their own projectiles
		// ~10-40 blocks away inside the same shared level.
		return player.serverLevel().getEntitiesOfClass(ShieldProjectileEntity.class,
				player.getBoundingBox().inflate(radius)).stream()
				.filter(p -> p.getOwner() == player)
				.toList();
	}

	/** Polls once per game tick until {@code cond} holds or {@code tries} run out, then runs {@code body}. */
	private static void awaitTrue(GameTestHelper helper, BooleanSupplier cond, int tries, Runnable body) {
		if (tries <= 0 || cond.getAsBoolean()) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitTrue(helper, cond, tries - 1, body));
	}

	/**
	 * Like {@link #awaitTrue} but the condition must hold for two consecutive ticks —
	 * {@code getEntitiesOfClass} can flicker a single tick while a moving entity
	 * crosses a section boundary or its removal propagates.
	 */
	private static void awaitStable(GameTestHelper helper, BooleanSupplier cond, int tries, Runnable body) {
		if (tries <= 0) {
			body.run();
			return;
		}
		if (!cond.getAsBoolean()) {
			helper.runAfterDelay(1, () -> awaitStable(helper, cond, tries - 1, body));
			return;
		}
		helper.runAfterDelay(1, () -> {
			if (cond.getAsBoolean()) {
				body.run();
			} else {
				awaitStable(helper, cond, tries - 2, body);
			}
		});
	}

	// ---------- ability registration ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void captainAmericaRegistersItsFourAbilities(GameTestHelper helper) {
		Hero hero = Heroes.get(CAPTAIN_AMERICA);
		helper.assertTrue(hero != null, "captain_america registered");
		helper.assertTrue(hero.getAbilities().equals(List.of(CAP_SHIELD_THROW, CAP_SHIELD_SLAM,
						CAP_SHIELD_DASH, CAP_COUNTER_STANCE)),
				"slot order " + hero.getAbilities());
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	// ---------- shield throw ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldThrowNeedsTheShieldInAHand(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-throw");
		faceForward(player);
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			float energy = HeroDataStore.get(player).energy();
			Ability throwAbility = AbilityRegistry.get(CAP_SHIELD_THROW);
			helper.assertTrue(!throwAbility.canActivate(player),
					"the throw is gated on holding the vibranium shield");
			AbilityRouter.activate(player, CAP_SHIELD_THROW);
			helper.assertTrue(HeroDataStore.get(player).energy() == energy,
					"a denied throw spends no energy");
			helper.assertTrue(!AbilityCooldowns.isOnCooldown(player, CAP_SHIELD_THROW),
					"a denied throw starts no cooldown");
			helper.assertTrue(shieldProjectilesNear(player, 8.0).isEmpty(),
					"a denied throw spawns no projectile");

			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(vibraniumShield()));
			helper.assertTrue(throwAbility.canActivate(player), "an offhand shield unlocks the throw");
			AbilityRouter.activate(player, CAP_SHIELD_THROW);
			helper.assertTrue(player.getOffhandItem().isEmpty(),
					"a successful throw empties the hand holding the shield");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, CAP_SHIELD_THROW),
					"a successful throw starts its cooldown");
			helper.assertTrue(HeroDataStore.get(player).energy() == energy - 50f,
					"a successful throw costs 50 energy");
		});
		// fresh entities take a few ticks to become scan-visible — poll for it.
		awaitTrue(helper, () -> !shieldProjectilesNear(player, 16.0).isEmpty(), 30, () -> {
			helper.assertTrue(!shieldProjectilesNear(player, 16.0).isEmpty(),
					"a successful throw spawns the shield projectile");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldThrowBouncesOffAHostileAndReturnsToHand(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-return");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		teleportIntoStructure(helper, player);
		faceForward(player);
		Zombie target = spawnZombieAhead(player, 3.0);
		TestPlayers.awaitVisible(helper, target, () -> {
			grantEnergy(player);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(vibraniumShield()));
			AbilityRouter.activate(player, CAP_SHIELD_THROW);
			helper.assertTrue(player.getOffhandItem().isEmpty(), "the shield left the offhand");
		});
		awaitTrue(helper,
				() -> player.getOffhandItem().is(vibraniumShield())
						&& shieldProjectilesNear(player, 32.0).isEmpty(),
				90, () -> {
			helper.assertTrue(player.getOffhandItem().is(vibraniumShield()),
					"the shield returned to the hand it was thrown from");
			helper.assertTrue(shieldProjectilesNear(player, 32.0).isEmpty(),
					"the projectile discarded itself on catch");
			helper.assertTrue(hurtBy(player, target),
					"the thrown shield damaged the hostile on its way out");
			helper.assertTrue(target.getLastDamageSource().is(CAP_SHIELD_THROW_DAMAGE),
					"throw damage uses the cap_shield_throw damage type");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void shieldThrowRestoresTheShieldWhenItsLifetimeExpires(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-expire");
		faceForward(player);
		player.setXRot(-90f); // straight up — nothing to hit on the way out
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(vibraniumShield()));
			AbilityRouter.activate(player, CAP_SHIELD_THROW);
			helper.assertTrue(player.getOffhandItem().isEmpty(), "the shield left the offhand");
		});
		// MAX_LIFETIME_TICKS=100 wins over the forced return at 80 (the owner is ~110
		// blocks below by then) — the shield lands via remove()'s owner-restore.
		awaitStable(helper, () -> TestPlayers.count(player, vibraniumShield()) == 1
				&& shieldProjectilesNear(player, 260.0).isEmpty(), 140, () -> {
			helper.assertTrue(TestPlayers.count(player, vibraniumShield()) == 1,
					"an expired projectile restores the shield to its owner");
			helper.assertTrue(shieldProjectilesNear(player, 260.0).isEmpty(),
					"the projectile is gone after its lifetime");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---------- shield slam ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldSlamLaunchesThePlayerAndOpensASession(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-slam-launch");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			float energy = HeroDataStore.get(player).energy();
			AbilityRouter.activate(player, CAP_SHIELD_SLAM);
			Ability slam = AbilityRegistry.get(CAP_SHIELD_SLAM);
			helper.assertTrue(player.getDeltaMovement().y() >= 1.39,
					"activation launches the player straight up (dy=1.4)");
			helper.assertTrue(player.hurtMarked, "the launch marks the velocity dirty for the client");
			helper.assertTrue(!slam.canActivate(player), "an airborne session blocks re-activation");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, CAP_SHIELD_SLAM),
					"the slam starts its cooldown on activation");
			helper.assertTrue(HeroDataStore.get(player).energy() == energy - 80f,
					"the slam costs 80 energy");

			TestPlayers.leave(player); // ClearOn.LEAVE drops the session
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldSlamNeedsFourAirTicksBeforeLandingDetonates(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-slam-min");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		faceForward(player);
		Zombie target = spawnZombieAhead(player, 2.5);
		TestPlayers.awaitVisible(helper, target, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, CAP_SHIELD_SLAM);
			player.setOnGround(true);
			for (int i = 0; i < 4; i++) {
				CapShieldSlamAbility.serverTick(player);
			}
			helper.assertTrue(!hurtBy(player, target),
					"touching ground inside the first 4 air ticks does not detonate");
			CapShieldSlamAbility.serverTick(player);
			helper.assertTrue(hurtBy(player, target),
					"landing once airTicks reaches 4 detonates the slam");
			helper.assertTrue(target.getLastDamageSource().is(CAP_SHIELD_SLAM_DAMAGE),
					"detonation uses the cap_shield_slam damage type");
			helper.assertTrue(Math.abs(target.getDeltaMovement().x()) > 0.5
							|| Math.abs(target.getDeltaMovement().z()) > 0.5,
					"detonation knocks the victim away");
			helper.assertTrue(AbilityRegistry.get(CAP_SHIELD_SLAM).canActivate(player),
					"detonation closes the session");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldSlamWaitsAirborneAndDetonatesOnTouchdown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-slam-air");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		faceForward(player);
		Zombie target = spawnZombieAhead(player, 2.5);
		TestPlayers.awaitVisible(helper, target, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, CAP_SHIELD_SLAM);
			for (int i = 0; i < 6; i++) {
				player.setOnGround(false);
				CapShieldSlamAbility.serverTick(player);
			}
			helper.assertTrue(!hurtBy(player, target),
					"the slam does not detonate while airborne, even past 4 air ticks");
			helper.assertTrue(!AbilityRegistry.get(CAP_SHIELD_SLAM).canActivate(player),
					"the airborne session is still open");
			player.setOnGround(true);
			CapShieldSlamAbility.serverTick(player);
			helper.assertTrue(hurtBy(player, target),
					"touching down after the airborne wait detonates the slam");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldSlamExpiresAfterHundredAirTicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-slam-expire");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		faceForward(player);
		Zombie target = spawnZombieAhead(player, 2.5);
		TestPlayers.awaitVisible(helper, target, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, CAP_SHIELD_SLAM);
			for (int i = 0; i < 100; i++) {
				player.setOnGround(false);
				CapShieldSlamAbility.serverTick(player);
			}
			helper.assertTrue(!hurtBy(player, target), "an airborne slam never detonates");
			helper.assertTrue(!AbilityRegistry.get(CAP_SHIELD_SLAM).canActivate(player),
					"the session is still open at 100 air ticks");
			player.setOnGround(false);
			CapShieldSlamAbility.serverTick(player);
			helper.assertTrue(AbilityRegistry.get(CAP_SHIELD_SLAM).canActivate(player),
					"the session expires silently on the tick that reads 100 air ticks");
			helper.assertTrue(!hurtBy(player, target), "expiry is silent — no detonation");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldSlamSessionDropsOnLeave(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-slam-leave");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, CAP_SHIELD_SLAM);
			helper.assertTrue(!AbilityRegistry.get(CAP_SHIELD_SLAM).canActivate(player),
					"the session is open before leave");
			TestPlayers.leave(player);
			ServerPlayer rejoined = TestPlayers.rejoin(helper, player);
			helper.assertTrue(AbilityRegistry.get(CAP_SHIELD_SLAM).canActivate(rejoined),
					"leave clears the airborne session (ClearOn.LEAVE)");
			TestPlayers.leave(rejoined);
			helper.succeed();
		});
	}

	// ---------- shield dash ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldDashStrikesAndSlowsTheNearestHostile(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-dash");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		teleportIntoStructure(helper, player);
		faceForward(player);
		Zombie target = spawnZombieAhead(player, 3.0);
		TestPlayers.awaitVisible(helper, target, () -> {
			grantEnergy(player);
			float energy = HeroDataStore.get(player).energy();
			AbilityRouter.activate(player, CAP_SHIELD_DASH);
			helper.assertTrue(player.getDeltaMovement().z() > 1.0,
					"the dash throws the player forward along the look vector");
			helper.assertTrue(hurtBy(player, target), "the dash hits the hostile on its path");
			MobEffectInstance slowness = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
			helper.assertTrue(slowness != null && slowness.getAmplifier() == 1
							&& slowness.getDuration() == 60,
					"the dash applies Slowness II for 60 ticks");
			helper.assertTrue(target.getDeltaMovement().z() > 0.5,
					"the dash knocks the victim away along the look vector");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, CAP_SHIELD_DASH),
					"the dash starts its cooldown");
			helper.assertTrue(HeroDataStore.get(player).energy() == energy - 60f,
					"the dash costs 60 energy");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldDashPushesThePlayerEvenWithoutATarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-dash-clear");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		faceForward(player);
		player.setXRot(-45f); // aim up — away from leftover entities on the ground
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, CAP_SHIELD_DASH);
			helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 0.5,
					"the impulse always applies, hit or miss");
			helper.assertTrue(player.fallDistance == 0f, "the dash zeroes fall distance");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, CAP_SHIELD_DASH),
					"the cooldown applies even on a whiffed dash");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---------- counter stance / hero passives ----------

	@GameTest(template = EMPTY_STRUCTURE)
	public void counterStanceTogglesBuffsAndDrainsEnergy(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-stance");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			grantEnergy(player);
			AbilityRouter.activate(player, CAP_COUNTER_STANCE);
			helper.assertTrue(HeroDataStore.get(player).isActive(CAP_COUNTER_STANCE),
					"counter stance is a toggle — activation marks it active");
			MobEffectInstance resistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
			helper.assertTrue(resistance != null && resistance.getAmplifier() == 1,
					"counter stance grants Resistance II");
			helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST),
					"counter stance grants Strength I");
			helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED),
					"counter stance grants Speed I");

			float before = HeroDataStore.get(player).energy(); // 1000 > max 200 — no regen
			HeroTickDispatcher.tick(helper.getLevel().getServer());
			helper.assertTrue(HeroDataStore.get(player).energy() == before - 4f,
					"the stance drains 4 energy per active tick");

			AbilityRouter.activate(player, CAP_COUNTER_STANCE); // re-activation toggles it off
			helper.assertTrue(!HeroDataStore.get(player).isActive(CAP_COUNTER_STANCE),
					"a second activation toggles the stance off");
			helper.assertTrue(!player.hasEffect(MobEffects.DAMAGE_RESISTANCE)
							&& !player.hasEffect(MobEffects.DAMAGE_BOOST)
							&& !player.hasEffect(MobEffects.MOVEMENT_SPEED),
					"deactivation strips the stance buffs");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void captainAmericaCancelsFallDamage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cap-fall");
		TestHeroes.transform(player, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			TestPlayers.clearSpawnInvulnerability(player);
			float hp = player.getHealth();
			boolean hurt = player.causeFallDamage(10f, 1.0f, player.damageSources().fall());
			helper.assertTrue(!hurt && player.getHealth() == hp,
					"cap's cancelsFallDamage hook cancels fall damage outright");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void killingCaptainAmericaGrantsThanosTheSoulStone(GameTestHelper helper) {
		ServerPlayer killer = TestPlayers.join(helper, "thanos-killer");
		ServerPlayer victim = TestPlayers.join(helper, "cap-victim");
		TestHeroes.transform(killer, THANOS);
		TestHeroes.transform(victim, CAPTAIN_AMERICA);
		helper.runAfterDelay(2, () -> {
			TestPlayers.clearSpawnInvulnerability(victim);
			// The gametest server runs pvp off — ServerPlayer.canHarmPlayer then
			// blocks both hurt() and die() when the attacker is a player. Enable
			// it only around the kill; the flag is global to every concurrent
			// test, so the window stays a few ticks of one callback.
			boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
			helper.getLevel().getServer().setPvpAllowed(true);
			victim.hurt(killer.damageSources().mobAttack(killer), victim.getMaxHealth() * 10f);
			helper.getLevel().getServer().setPvpAllowed(oldPvp);
			helper.assertTrue(!victim.isAlive(), "the victim died to the hit");
			helper.assertTrue(TestPlayers.count(killer, item("soul_stone")) == 1,
					"the thanos killer receives cap's SOUL stone drop row");
			TestPlayers.leave(killer);
			TestPlayers.leave(victim);
			helper.succeed();
		});
	}
}
