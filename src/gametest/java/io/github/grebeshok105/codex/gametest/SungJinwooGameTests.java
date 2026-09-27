package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.sungjinwoo.SungJinwooAttachments;
import io.github.grebeshok105.codex.hero.sungjinwoo.runtime.SungShadowArmy;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.sungjinwoo.runtime.MonarchsDomainController;
import io.github.grebeshok105.codex.hero.sungjinwoo.runtime.SungJinwooController;
import io.github.grebeshok105.codex.hero.sungjinwoo.entity.ShadowSoldierEntity;
import io.github.grebeshok105.codex.hero.sungjinwoo.net.SungShadowArmyS2CPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Characterization for the I4a module move, written against the pre-move layout:
 * Sung Jinwoo's ability slot order, the initial shadow army spawn + owner binding,
 * Arise's echo-drain and weakened-mob paths (incl. the no-op when nothing is
 * raisable), damage redirect to a random shadow, shadow target selection from the
 * owner's last victim, Sacrifice, Shadow Exchange, Ruler's Authority, Monarch's
 * Domain's active window + expiry debuffs, and the army payload codec.
 * Ability ids are literals because the constants move from {@code AbilityIds} to the
 * module (SungJinwooAbilities).
 */
public final class SungJinwooGameTests implements FabricGameTest {
	private static final ResourceLocation SUNG = ModId.of("sung_jinwoo");
	private static final ResourceLocation ARISE = ModId.of("arise");
	private static final ResourceLocation SHADOW_EXCHANGE = ModId.of("shadow_exchange");
	private static final ResourceLocation SACRIFICE = ModId.of("sacrifice");
	private static final ResourceLocation RULERS_AUTHORITY = ModId.of("rulers_authority");
	private static final ResourceLocation SHADOW_EXTRACTION = ModId.of("shadow_extraction");
	private static final ResourceLocation MONARCHS_DOMAIN = ModId.of("monarchs_domain");

	@GameTest(template = EMPTY_STRUCTURE)
	public void sungOwnsItsAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero hero = Heroes.get(SUNG);
		helper.assertTrue(hero != null, "sung_jinwoo registered");
		helper.assertTrue(hero.getAbilities().equals(List.of(
				ARISE, SHADOW_EXCHANGE, SACRIFICE, RULERS_AUTHORITY, SHADOW_EXTRACTION, MONARCHS_DOMAIN)),
				"slot order " + hero.getAbilities());
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void initialArmySpawnsBoundToOwner(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			SungShadowArmy army = player.getAttachedOrCreate(SungJinwooAttachments.ARMY);
			helper.assertTrue(army.summoned(), "the summon is recorded in the persistent attachment");
			helper.assertFalse(army.phase2(), "phase2 starts cleared");
			helper.assertTrue(army.shadowIds().size() == SungJinwooController.MAX_SHADOWS,
					"ten shadow ids recorded, got " + army.shadowIds().size());
			List<ShadowSoldierEntity> shadows = SungJinwooController.aliveShadows(player);
			helper.assertTrue(shadows.size() == SungJinwooController.MAX_SHADOWS,
					"ten live shadows resolvable, got " + shadows.size());
			for (ShadowSoldierEntity shadow : shadows) {
				helper.assertTrue(player.getUUID().equals(shadow.getOwnerId()),
						"shadow is owner-bound");
				helper.assertTrue(SungJinwooController.isArmyMember(player, shadow.getUUID()),
						"shadow is a listed army member");
			}
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ariseRaisesKilledMobFromEcho(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		// The initial 10-shadow army must be summoned first: the summon overwrites the
		// attachment's shadow list, so any shadow raised before it would be dropped.
		helper.runAfterDelay(10, () -> {
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
			Vec3 corpse = zombie.position();
			zombie.kill();
			// Pin the echo AT the corpse, not by an in-range count: kills in other
			// tests can add echoes within the shared level's 50-block arise range.
			Vec3 echo = SungJinwooController.nearestDeathEcho(player, 5);
			helper.assertTrue(echo != null && echo.distanceTo(corpse) < 1.5,
					"a fresh corpse leaves a death echo, nearest " + echo
							+ " in-range count " + SungJinwooController.countDeathEchoesInRange(player, SungJinwooController.ARISE_RANGE));

			float energyBefore = HeroDataStore.get(player).energy();
			AbilityRouter.activate(player, ARISE);
			helper.assertTrue(player.getAttachedOrCreate(SungJinwooAttachments.ARMY).shadowIds().size()
							>= SungJinwooController.MAX_SHADOWS + 1,
					"arise adds the raised shadow to the army");
			Vec3 after = SungJinwooController.nearestDeathEcho(player, 5);
			helper.assertTrue(after == null || after.distanceTo(corpse) >= 1.5,
					"arise drains the corpse's echo, nearest now " + after);
			helper.assertTrue(HeroDataStore.get(player).energy() == energyBefore - 20f,
					"arise costs 20 energy, delta " + (HeroDataStore.get(player).energy() - energyBefore));
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ARISE), "arise cooldown armed");

			helper.runAfterDelay(10, () -> {
				helper.assertTrue(SungJinwooController.aliveCount(player) >= SungJinwooController.MAX_SHADOWS + 1,
						"the raised shadow resolves as a live army member, got "
								+ SungJinwooController.aliveCount(player));
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ariseFinishesWeakenedMobWithoutEcho(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		// Unique offset: arise drains every death echo and finishes every weakened
		// mob in a 50-block radius, so grid-adjacent tests pollute the count.
		double isoX = inside.x + 8000.0;
		double isoZ = inside.z + 8000.0;
		helper.getLevel().getChunk(BlockPos.containing(isoX, inside.y, isoZ));
		player.teleportTo(isoX, inside.y, isoZ);

		helper.runAfterDelay(10, () -> {
			// Fresh spawn at the isolated spot: a teleport can lag a chunk-section
			// update and arise's bounding-box scan then misses the mob.
			Zombie zombie = new Zombie(EntityType.ZOMBIE, helper.getLevel());
			helper.getLevel().getChunk(BlockPos.containing(isoX + 2, inside.y, isoZ + 2));
			zombie.moveTo(isoX + 2, inside.y, isoZ + 2, 0f, 0f);
			helper.getLevel().addFreshEntity(zombie);
			zombie.setHealth(4f); // 4/20 < 25% — the weakened-finish path
			helper.runAfterDelay(1, () -> TestPlayers.awaitVisible(helper, zombie, () -> {
				AbilityRouter.activate(player, ARISE);
				helper.assertFalse(zombie.isAlive(), "arise finishes the weakened mob");
				helper.assertTrue(player.getAttachedOrCreate(SungJinwooAttachments.ARMY).shadowIds().size()
								== SungJinwooController.MAX_SHADOWS + 1,
						"the finished mob joins the army");
				helper.assertTrue(SungJinwooController.countDeathEchoesInRange(player, SungJinwooController.ARISE_RANGE) == 0,
						"the suppressed death leaves no echo behind");

				helper.runAfterDelay(10, () -> {
					List<ShadowSoldierEntity> shadows = SungJinwooController.aliveShadows(player);
					helper.assertTrue(shadows.size() == SungJinwooController.MAX_SHADOWS + 1,
							"the raised shadow is among the live army");
					TestPlayers.leave(player);
					helper.succeed();
				});
			}));
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ariseDoesNothingWithoutCorpses(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		// Isolation far outside the grid: no death echoes, no weakened mobs in range.
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x + 5000, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			float energyBefore = HeroDataStore.get(player).energy();
			AbilityRouter.activate(player, ARISE);
			helper.assertFalse(AbilityCooldowns.isOnCooldown(player, ARISE),
					"nothing to raise — no cooldown");
			helper.assertTrue(HeroDataStore.get(player).energy() == energyBefore,
					"nothing to raise — no energy spent");
			helper.assertTrue(player.getAttachedOrCreate(SungJinwooAttachments.ARMY).shadowIds().size()
							== SungJinwooController.MAX_SHADOWS,
					"army unchanged");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void damageRedirectsToRandomShadow(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		TestPlayers.clearSpawnInvulnerability(player);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			// The redirect is synchronous, so snapshot health around hurt() inside the
			// same tick — freshly spawned shadows can still take ambient (e.g. fall)
			// damage during the delay, which a getHealth()<maxHealth scan would count.
			List<ShadowSoldierEntity> army = SungJinwooController.aliveShadows(player);
			helper.assertTrue(!army.isEmpty(), "the initial army is present");
			Map<UUID, Float> healthBefore = new java.util.HashMap<>();
			for (ShadowSoldierEntity s : army) {
				healthBefore.put(s.getUUID(), s.getHealth());
			}
			float playerHealthBefore = player.getHealth();

			boolean applied = player.hurt(player.damageSources().generic(), 5f);
			helper.assertFalse(applied, "the hit is cancelled by the redirect");
			helper.assertTrue(player.getHealth() == playerHealthBefore, "the owner takes no damage");
			long changed = SungJinwooController.aliveShadows(player).stream()
					.filter(s -> s.getHealth() < healthBefore.getOrDefault(s.getUUID(), Float.MAX_VALUE))
					.count();
			helper.assertTrue(changed == 1, "exactly one shadow absorbs the hit, got " + changed);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shadowTargetsOwnersLastVictim(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, 3, 1, 3);
			player.setLastHurtMob(zombie);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(zombie.isAlive(), "setup: the victim is still alive");
				helper.assertTrue(SungJinwooController.aliveShadows(player).stream()
								.anyMatch(s -> s.getTarget() == zombie),
						"at least one shadow hunts the owner's last victim");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sacrificeDetonatesTheArmy(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
			zombie.teleportTo(player.getX(), player.getY(), player.getZ());
			TestPlayers.awaitVisible(helper, zombie, () -> {
				float energyBefore = HeroDataStore.get(player).energy();
				AbilityRouter.activate(player, SACRIFICE);

				helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth() || !zombie.isAlive(),
						"the blast damages nearby hostiles");
				helper.assertTrue(player.getAttachedOrCreate(SungJinwooAttachments.ARMY).shadowIds().isEmpty(),
						"the army attachment is emptied");
				helper.assertTrue(SungJinwooController.aliveCount(player) == 0, "no live shadows remain");
				helper.assertTrue(HeroDataStore.get(player).energy() == energyBefore - 30f,
						"sacrifice costs 30 energy");
				helper.assertTrue(AbilityCooldowns.isOnCooldown(player, SACRIFICE), "sacrifice cooldown armed");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shadowExchangeSwapsPositions(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			ShadowSoldierEntity nearest = SungJinwooController.aliveShadows(player).stream()
					.min(Comparator.comparingDouble(s -> s.distanceToSqr(player))).orElseThrow();
			Vec3 playerPos = player.position();
			Vec3 shadowPos = nearest.position();
			AbilityRouter.activate(player, SHADOW_EXCHANGE);

			helper.assertTrue(player.position().distanceTo(shadowPos) < 2.0,
					"the owner lands where the shadow stood");
			helper.assertTrue(nearest.position().distanceTo(playerPos) < 2.0,
					"the shadow lands where the owner stood");
			helper.assertTrue(player.getEffect(MobEffects.DAMAGE_RESISTANCE) != null
							&& player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 4,
					"resistance V for 10 ticks after the swap");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, SHADOW_EXCHANGE),
					"exchange cooldown armed");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rulersAuthorityLaunchesTheLookTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		// Face +Z so the look-dot pick is deterministic; the zombie stands in front.
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);
		player.setYRot(0f);
		player.setXRot(0f);
		player.setYHeadRot(0f);

		helper.runAfterDelay(10, () -> {
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 5);
			TestPlayers.awaitVisible(helper, zombie, () -> {
				AbilityRouter.activate(player, RULERS_AUTHORITY);
				helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
						"the picked target takes 6 damage");
				helper.assertTrue(zombie.getDeltaMovement().y > 1.5,
						"the picked target is launched upward");
				helper.assertTrue(zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null
								&& zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 6,
						"slowness VII on the picked target");
				helper.assertTrue(zombie.getEffect(MobEffects.WEAKNESS) != null
								&& zombie.getEffect(MobEffects.WEAKNESS).getAmplifier() == 4,
						"weakness V on the picked target");
				helper.assertTrue(AbilityCooldowns.isOnCooldown(player, RULERS_AUTHORITY),
						"authority cooldown armed");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void monarchsDomainHurtsHostilesInRadius(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, 6, 1, 1);
			TestPlayers.awaitVisible(helper, zombie, () -> {
				AbilityRouter.activate(player, MONARCHS_DOMAIN);
				helper.assertTrue(MonarchsDomainController.isActive(player), "the domain window opens");
				helper.assertTrue(SungJinwooController.isPhase2(player), "the army enters phase2");
				helper.assertTrue(player.getEffect(MobEffects.DAMAGE_RESISTANCE) != null
								&& player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 1,
						"resistance II for the domain duration");

				helper.runAfterDelay(20, () -> {
					helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth() || !zombie.isAlive(),
							"the domain ticks 4 magic damage into hostiles within 25 blocks");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void monarchsDomainExpiresIntoDebuffs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.teleportTo(inside.x, inside.y, inside.z);

		helper.runAfterDelay(10, () -> {
			AbilityRouter.activate(player, MONARCHS_DOMAIN);
			helper.assertTrue(MonarchsDomainController.isActive(player), "setup: domain active");

			// The 200-tick window, then expiry applies Weakness II + Slowness III.
			helper.runAfterDelay(215, () -> {
				helper.assertFalse(MonarchsDomainController.isActive(player), "the domain expires");
				helper.assertTrue(player.getEffect(MobEffects.WEAKNESS) != null
								&& player.getEffect(MobEffects.WEAKNESS).getAmplifier() == 1,
						"weakness II after the domain");
				helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null
								&& player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 2,
						"slowness III after the domain");
				helper.assertTrue(SungJinwooController.isPhase2(player),
						"phase2 persists past the domain window");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shadowExtractionHealsAndRefundsEnergy(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, SUNG);
		player.setHealth(player.getMaxHealth() - 5f);

		float energyBefore = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, SHADOW_EXTRACTION);
		helper.assertTrue(Math.abs(player.getHealth() - (player.getMaxHealth() - 3f)) < 0.01f,
				"extraction heals +2, got " + player.getHealth());
		helper.assertTrue(HeroDataStore.get(player).energy() == energyBefore - 5f,
				"net cost is 10 spent - 5 returned");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, SHADOW_EXTRACTION),
				"extraction cooldown armed");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void armyPayloadCodecRoundTrips(GameTestHelper helper) {
		helper.assertTrue(SungShadowArmyS2CPayload.TYPE.id().equals(ModId.of("sung_shadow_army")),
				"payload type id is byte-identical");
		UUID id = UUID.randomUUID();
		SungShadowArmyS2CPayload payload = new SungShadowArmyS2CPayload(id, true, 7, true);
		ByteBuf buf = Unpooled.buffer();
		try {
			SungShadowArmyS2CPayload.STREAM_CODEC.encode(buf, payload);
			SungShadowArmyS2CPayload decoded = SungShadowArmyS2CPayload.STREAM_CODEC.decode(buf);
			helper.assertTrue(decoded.playerId().equals(id)
							&& decoded.hasShadows() && decoded.count() == 7 && decoded.phase2(),
					"encode/decode preserves all fields");
		} finally {
			buf.release();
		}
		helper.succeed();
	}
}
