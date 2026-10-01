package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusHearts;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task 5 debris-kick pins — the lion_roar slot's authored-cast shotgun: a 9-ray fan
 * inside a 35° cone at 16 blocks, COLLIDER raycasts that break through at most three
 * destructible blocks per ray and stop on the first unbreakable one, pellet hits
 * without dedupe (every ray crossing a target's AABB damages it independently), and
 * {@code 7 * RegulusHearts.damageScale} per pellet under the {@code regulus_debris}
 * damage type.
 *
 * <p>Two shared-server realities shape the assertions. First, the 16-block cone is
 * wider than the structure spacing, so a neighbouring batch test's entities — or its
 * own debris fan — legitimately enter the wedge: every "untouched" pin therefore
 * asserts the <em>cause</em> ({@code lastDamageSource.getEntity() != the caster})
 * rather than raw health, and every break pin records the breaker through
 * {@link PlayerBlockBreakEvents#AFTER}. Second, the empty structure is void — all
 * entities ride {@code setNoGravity}/{@code setNoAi}, and the caster's rotation is
 * re-pinned just before the authored fire tick because the mock player's join
 * placement may overwrite it mid-windup.
 */
public final class RegulusDebrisGameTests implements FabricGameTest {

	private static final ResourceLocation LION_ROAR = ModId.of("lion_roar");

	/** pos → breaker uuid for every AFTER break this run — attributes block breaks to their ray's owner. */
	private static final Map<BlockPos, UUID> BREAKS = new ConcurrentHashMap<>();

	/** (victim, attacker) → last damage offered — causal attribution survives a
	 *  foreign pellet landing in the same window. */
	private static final Map<HitKey, Float> LAST_HIT = new ConcurrentHashMap<>();

	private record HitKey(UUID victim, UUID attacker) {
		private static HitKey of(LivingEntity victim, ServerPlayer attacker) {
			return new HitKey(victim.getUUID(), attacker.getUUID());
		}
	}

	static {
		PlayerBlockBreakEvents.AFTER.register(
				(level, player, pos, state, blockEntity) -> BREAKS.put(pos.immutable(), player.getUUID()));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.getEntity() instanceof ServerPlayer attacker && entity instanceof LivingEntity victim) {
				LAST_HIT.put(HitKey.of(victim, attacker), amount);
			}
			return true;
		});
	}

	/**
	 * The fan lives inside a 35° cone in front of the caster: the zombie ahead is
	 * shredded while the one behind is never touched by the caster, and the windup
	 * slows the caster for the cast duration.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void debrisFanHitsConeOnly(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		Zombie ahead = spawnAhead(helper, player, 4.0);
		// yaw 0 faces +Z — "behind" is -Z on the caster's plane.
		Zombie behind = spawnEntity(helper, EntityType.ZOMBIE,
				player.getX(), player.getY(), player.getZ() - 4.0);
		behind.setNoAi(true);

		TestPlayers.awaitVisible(helper, ahead, () -> TestPlayers.awaitVisible(helper, behind, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(8, () -> helper.assertTrue(
					player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null,
					"the windup slows the caster"));
			helper.runAfterDelay(20, () -> {
				helper.assertTrue(ahead.getHealth() < ahead.getMaxHealth(),
						"the fan hits in front of the caster");
				helper.assertTrue(!wasHurtBy(behind, player),
						"the caster's own fan never reaches behind");
				TestPlayers.leave(player);
				helper.succeed();
			});
		}));
	}

	/**
	 * Pellet rule without dedupe — the vanilla-shotgun rule: one target takes damage
	 * from every ray whose AABB it crosses. A point-blank giant intersects several
	 * rays, so the total must exceed a couple of pellets, not a flat cone hit.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void debrisPelletsStackOnLargeTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		Giant giant = spawnEntity(helper, EntityType.GIANT,
				player.getX(), player.getY(), player.getZ() + 2.5);
		giant.setNoAi(true);

		TestPlayers.awaitVisible(helper, giant, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(20, () -> {
				float dealt = giant.getMaxHealth() - giant.getHealth();
				helper.assertTrue(dealt > 15f && wasHurtBy(giant, player),
						"a large point-blank target eats several pellets (dealt " + dealt
								+ ", a single pellet is 7 and the old flat cone was 14)");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * Each pellet is {@code 7 * damageScale(owner)} — the pin is the live formula:
	 * the last pellet the caster landed must equal {@code 7 * scale} for whichever
	 * scale the claim race left the owner with (the 20-block aura can overlap a
	 * concurrent Regulus test).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
	public void debrisDamageScalesWithHearts(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		// Eight heart-bearer candidates behind the caster: usually enough to land
		// at least one heart even when another Regulus test grazes the aura.
		for (int i = 0; i < 8; i++) {
			Cow cow = spawnEntity(helper, EntityType.COW,
					player.getX() - 2.5, player.getY(), player.getZ() - 4.0 + i * 0.7);
			cow.setNoAi(true);
		}
		// Hearts land on the owner's 20-tick claim scan — give it one full window.
		helper.runAfterDelay(30, () -> {
			// A ravager reads back the raw pellet amount: 100 hp survives a stray
			// foreign pellet, zero armor, and 2.2m of height for the eye-level ray
			// (a cow is too short — the ray sails over a 1.4m box).
			Ravager target = spawnEntity(helper, EntityType.RAVAGER,
					player.getX(), player.getY(), player.getZ() + 4.5);
			target.setNoAi(true);
			TestPlayers.awaitVisible(helper, target, () -> {
				float scaleAtCast = RegulusHearts.damageScale(player);
				activateAiming(helper, player);
				helper.runAfterDelay(20, () -> {
					float scaleNow = RegulusHearts.damageScale(player);
					// ALLOW_DAMAGE records the offered amount — raw pellets, no
					// armor/death drift — attributed to the caster who fired.
					Float hit = hitAmount(target, player);
					helper.assertTrue(hit != null,
							"the center pellet is the caster's own");
					// The claim scan or a bearer death can shift scale inside the
					// 20-tick window — accept either boundary of the formula.
					float pellet = hit == null ? -1f : hit;
					helper.assertTrue(Math.abs(pellet - 7f * scaleAtCast) < 0.05f
									|| Math.abs(pellet - 7f * scaleNow) < 0.05f,
							"one pellet deals 7 * damageScale (cast=" + scaleAtCast
									+ ", now=" + scaleNow + ", pellet=" + pellet + ")");
					TestPlayers.leave(player);
					helper.succeed();
				});
			});
		});
	}

	/**
	 * A ray that chewed through its three destructible blocks dies on the next
	 * indestructible one — the bedrock cap stands, no break lands the caster's id,
	 * and the zombie behind it is never touched by this caster.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void debrisBreaksBlocksButNotBedrock(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		BlockPos floor = BlockPos.containing(player.position());
		// mayInteract's reach gate only lets the policy break blocks roughly
		// within 5.5 of the eye — the wall lives inside it so only the
		// destructibility rules decide what breaks.
		for (int depth = 2; depth <= 4; depth++) {
			setBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + depth, Blocks.DIRT);
		}
		setBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + 5, Blocks.BEDROCK);
		Zombie target = spawnAhead(helper, player, 7.5);
		target.setNoAi(true);

		TestPlayers.awaitVisible(helper, target, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(20, () -> {
				for (int depth = 2; depth <= 4; depth++) {
					assertBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + depth,
							Blocks.AIR, "the wall line lost its dirt block at depth " + depth);
				}
				assertBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + 5,
						Blocks.BEDROCK, "bedrock caps the broken line intact");
				helper.assertTrue(!wasHurtBy(target, player),
						"the caster's ray dies on the bedrock — the zombie is safe");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * The break budget is exactly three per ray — a wall of four destructible
	 * blocks loses only the first three, the caster's ray never breaks the fourth,
	 * and the zombie behind the wall is never reached by this caster.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void debrisBreaksAtMostThreeBlocksPerRay(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		BlockPos floor = BlockPos.containing(player.position());
		for (int depth = 2; depth <= 5; depth++) {
			setBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + depth, Blocks.DIRT);
		}
		Zombie target = spawnAhead(helper, player, 7.0);
		target.setNoAi(true);

		TestPlayers.awaitVisible(helper, target, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(20, () -> {
				for (int depth = 2; depth <= 4; depth++) {
					assertBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + depth,
							Blocks.AIR, "the wall line lost dirt at depth " + depth);
				}
				BlockPos fourth = new BlockPos(floor.getX(), floor.getY() + 1, floor.getZ() + 5);
				helper.assertTrue(!player.getUUID().equals(BREAKS.get(fourth)),
						"the per-ray cap is three — the fourth block was never broken by the caster");
				helper.assertTrue(!wasHurtBy(target, player),
						"the ray stops at the cap's last block and never reaches the zombie");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * A ray that meets an indestructible block dies on it immediately — a single
	 * bedrock slab eats the whole center ray and shields the zombie behind it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void debrisRayBlockedByUnbreakable(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		BlockPos floor = BlockPos.containing(player.position());
		setBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + 3, Blocks.BEDROCK);
		Zombie target = spawnAhead(helper, player, 6.0);
		target.setNoAi(true);

		TestPlayers.awaitVisible(helper, target, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(20, () -> {
				assertBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + 3,
						Blocks.BEDROCK, "the unbreakable block survives");
				helper.assertTrue(!wasHurtBy(target, player),
						"an unbreakable block eats the caster's ray — the zombie is untouched");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * The fan shoots through walls it just broke: a 2-deep dirt wall loses both
	 * blocks and the ray continues into the zombie behind it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void debrisRayPassesThroughBrokenBlocks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		BlockPos floor = BlockPos.containing(player.position());
		for (int depth = 2; depth <= 3; depth++) {
			setBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + depth, Blocks.DIRT);
		}
		// 100 hp — a stray foreign pellet can't kill the pin's witness mid-cast.
		Ravager target = spawnEntity(helper, EntityType.RAVAGER,
				player.getX(), player.getY(), player.getZ() + 5.5);
		target.setNoAi(true);

		TestPlayers.awaitVisible(helper, target, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(20, () -> {
				for (int depth = 2; depth <= 3; depth++) {
					assertBlock(helper, floor.getX(), floor.getY() + 1, floor.getZ() + depth,
							Blocks.AIR, "the wall line lost its dirt block at depth " + depth);
				}
				helper.assertTrue(wasHurtBy(target, player),
						"the caster's own ray continues through the blocks it broke");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * A cancelled cast is free: cancelled before the authored fire tick the shot
	 * never exists — this caster never lands a pellet, no charge, no cooldown, and
	 * the windup slowness leaves with the cast.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void castInterruptCancelsShot(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		HeroDataStore.update(player, d -> d.withEnergy(1000f));
		Zombie target = spawnAhead(helper, player, 4.5);
		target.setNoAi(true);

		TestPlayers.awaitVisible(helper, target, () -> {
			activateAiming(helper, player);
			helper.runAfterDelay(4, () -> RegulusCastState.cancel(player));
			helper.runAfterDelay(20, () -> {
				helper.assertTrue(!wasHurtBy(target, player),
						"a cancelled cast never fires");
				helper.assertTrue(!AbilityCooldowns.isOnCooldown(player, LION_ROAR),
						"the free cancel arms no cooldown");
				helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null,
						"the interrupt strips the cast slowness");
				helper.assertTrue(HeroDataStore.get(player).energy() > 999f,
						"the free cancel was never charged");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/**
	 * The router sees {@code costOnActivate() = 0} — the cast machine charges the
	 * 250 only on the authored fire tick (14). Energy before it is untouched;
	 * energy after it is down by roughly the activation cost, and the cooldown
	 * is armed.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void castDoesNotChargeBeforeAuthoredEvent(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		aimForward(player);
		HeroDataStore.update(player, d -> d.withEnergy(1000f));
		Zombie target = spawnAhead(helper, player, 4.5);
		target.setNoAi(true);

		TestPlayers.awaitVisible(helper, target, () -> {
			float before = HeroDataStore.get(player).energy();
			activateAiming(helper, player);
			helper.runAfterDelay(10, () -> helper.assertTrue(
					HeroDataStore.get(player).energy() >= before - 0.5f,
					"no energy is spent before the authored fire tick"));
			helper.runAfterDelay(20, () -> {
				helper.assertTrue(HeroDataStore.get(player).energy() <= before - 200f,
						"the machine charges the 250 on the fire tick");
				helper.assertTrue(AbilityCooldowns.isOnCooldown(player, LION_ROAR),
						"the fire tick arms the cooldown");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	/** The debris sound events and their OGG payloads ship with the mod. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void soundEventRegistered(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.SOUND_EVENT.get(ModId.of("regulus.debris_roar")) != null,
				"the debris_roar sound event is registered");
		helper.assertTrue(BuiltInRegistries.SOUND_EVENT.get(ModId.of("regulus.debris_impact")) != null,
				"the debris_impact sound event is registered");
		helper.assertTrue(RegulusDebrisGameTests.class
						.getResource("/assets/superheroes/sounds/regulus/debris_roar.ogg") != null,
				"debris_roar.ogg ships in the jar");
		helper.assertTrue(RegulusDebrisGameTests.class
						.getResource("/assets/superheroes/sounds/regulus/debris_impact.ogg") != null,
				"debris_impact.ogg ships in the jar");
		helper.succeed();
	}

	/**
	 * Levels the caster dead ahead (+Z) at eye height — the horizontal ray lives
	 * inside a same-floor zombie's box (top 1.95 > eye 1.62) at every range and
	 * crosses the {@code floorY+1} wall row block tests place.
	 */
	private static void aimForward(ServerPlayer player) {
		player.setNoGravity(true);
		player.setYRot(0f);
		player.setXRot(0f);
	}

	/**
	 * Activates the slot and re-pins the rotation just before the authored fire
	 * tick (14): the mock player's spawn placement may overwrite rotation during
	 * the first ticks after join, so the fire tick reads whatever was set last.
	 */
	private static void activateAiming(GameTestHelper helper, ServerPlayer player) {
		AbilityRouter.activate(player, LION_ROAR);
		helper.runAfterDelay(13, () -> aimForward(player));
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player, double distance) {
		Zombie zombie = spawnEntity(helper, EntityType.ZOMBIE,
				player.getX(), player.getY(), player.getZ() + distance);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		return zombie;
	}

	private static <T extends Entity> T spawnEntity(GameTestHelper helper, EntityType<T> type,
			double x, double y, double z) {
		// Force-load the destination chunk first: in an unloaded chunk the entity is
		// never registered for area scans, so controllers would see an empty world.
		helper.getLevel().getChunk(BlockPos.containing(x, y, z));
		T entity = type.create(helper.getLevel());
		entity.moveTo(x, y, z, 0f, 0f);
		entity.setNoGravity(true);
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}

	private static void setBlock(GameTestHelper helper, int x, int y, int z, Block block) {
		helper.getLevel().setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
	}

	private static void assertBlock(GameTestHelper helper, int x, int y, int z, Block block,
			String message) {
		helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, y, z)).is(block), message);
	}

	/** The damage this caster offered the victim, or null when none of it landed. */
	private static Float hitAmount(LivingEntity victim, ServerPlayer player) {
		return LAST_HIT.get(HitKey.of(victim, player));
	}

	/** True when the caster's own damage was offered to the victim — the causal check. */
	private static boolean wasHurtBy(LivingEntity victim, ServerPlayer player) {
		return hitAmount(victim, player) != null;
	}
}
