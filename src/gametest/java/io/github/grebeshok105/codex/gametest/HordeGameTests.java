package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.content.horde.HordeManager;
import io.github.grebeshok105.codex.content.horde.entity.BaseHordeEntity;
import io.github.grebeshok105.codex.content.horde.entity.HordeEntities;
import io.github.grebeshok105.codex.content.horde.HordeItems;
import io.github.grebeshok105.codex.content.horde.net.HordeDebugS2CPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IC1 phase-1 characterization: pins the horde's observable behavior so the
 * {@code horde/} → {@code content/horde/} move can be verified byte-equivalent
 * in behavior. Covers entity/payload registration, first-wave spawn, the
 * audience freeze/resume cycle, mob-death bookkeeping, the admin ops
 * (forceNextWave/clearMobs/stopHorde), the crystal item, and resetAll.
 *
 * <p>Every test that starts a horde goes through {@link #acquireHorde}:
 * {@code HordeManager}'s level-scoped readers ({@code liveMobCount},
 * {@code activeIn}) return the FIRST unfinished horde in the level, so two
 * concurrent hordes in one level make each other's reads ambiguous — e.g. the
 * {@code hordePausesWithoutAudience} regression test asserts
 * {@code liveMobCount(level) == 0}. The slot + {@code hasActiveHorde} gate
 * serializes all hordes in the shared gametest world.
 */
public final class HordeGameTests implements FabricGameTest {

	/** Held for a test's whole duration; released after its horde is stopped. */
	private static boolean hordeSlotTaken;

	// ─────────────────────────────────── registration pins ────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void hordeEntityTypesRegisteredAndInstantiable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> bad = new ArrayList<>();
		int count = 0;
		for (Field f : HordeEntities.class.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers()) || !EntityType.class.isAssignableFrom(f.getType())) {
				continue;
			}
			EntityType<?> type;
			try {
				type = (EntityType<?>) f.get(null);
			} catch (IllegalAccessException e) {
				bad.add(f.getName() + ": " + e);
				continue;
			}
			count++;
			ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || !"superheroes".equals(id.getNamespace())
					|| !id.getPath().startsWith("horde_")) {
				bad.add(f.getName() + " -> " + id);
				continue;
			}
			// create() exercises the attribute wiring: a missing
			// FabricDefaultAttributeRegistry entry throws here.
			try {
				if (type.create(level) == null) {
					bad.add(f.getName() + ": create() returned null");
				}
			} catch (Throwable t) {
				bad.add(f.getName() + ": " + t);
			}
		}
		helper.assertTrue(bad.isEmpty(), "horde entity types not registered/instantiable: " + bad);
		helper.assertTrue(count == 21, "expected 21 horde EntityType fields (19 mobs + 2 bombs), found " + count);
		// SPAWNABLE is the /superheroes horde spawn name table: all 19 mobs, no projectiles.
		helper.assertTrue(HordeEntities.SPAWNABLE.size() == 19,
				"SPAWNABLE must list 19 mobs, found " + HordeEntities.SPAWNABLE.size());
		helper.assertTrue(!HordeEntities.SPAWNABLE.containsKey("acid_bomb")
						&& !HordeEntities.SPAWNABLE.containsKey("fire_bomb"),
				"projectiles must stay out of SPAWNABLE");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void hordeDebugPayloadIsRegistered(GameTestHelper helper) {
		boolean registered;
		try {
			ServerPlayNetworking.createS2CPacket(new HordeDebugS2CPayload("ic1-probe"));
			registered = true;
		} catch (RuntimeException e) {
			registered = false;
		}
		helper.assertTrue(registered, "superheroes:horde_debug must be registered as an S2C payload");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void hordeSpawnSingleLeavesMobsUntracked(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		int spawned = HordeManager.spawnSingle(level, HordeEntities.CRAWLER, pos, 2);
		helper.assertTrue(spawned == 2, "spawnSingle must report spawned count, got " + spawned);
		helper.runAfterDelay(10, () -> {
			List<BaseHordeEntity> fresh = level.getEntitiesOfClass(BaseHordeEntity.class,
					new AABB(pos.x - 10, pos.y - 10, pos.z - 10, pos.x + 10, pos.y + 10, pos.z + 10),
					m -> m.getType() == HordeEntities.CRAWLER && m.getHordeId() == null);
			helper.assertTrue(fresh.size() >= 2,
					"spawnSingle mobs must exist with no horde binding, found " + fresh.size());
			for (BaseHordeEntity m : fresh) {
				m.discard();
			}
			helper.succeed();
		});
	}

	// ───────────────────────────────────── wave lifecycle ─────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3000)
	public void hordeFirstWaveSpawnsAroundAudience(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		acquireHorde(helper, level, 20, 2800, () -> {
			player.teleportTo(center.x, center.y + 2, center.z);
			UUID id = HordeManager.startHorde(level, center, player);
			helper.runAfterDelay(150, () -> {
				helper.assertTrue(HordeManager.liveMobCount(level) > 0,
						"wave 1 must spawn mobs once an audience is near");
				helper.assertTrue(!hordeMobs(level, id).isEmpty(),
						"wave 1 mobs must be horde entities bound to this horde");
				helper.assertTrue(HordeManager.getDebugStatus(level).contains("волна §e1"),
						"debug status must report wave 1, got: " + HordeManager.getDebugStatus(level));
				cleanupHorde(helper, level, player, id);
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3000)
	public void hordeFreezesWithoutAudienceMidWave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		// 200 blocks up so no player of a concurrently running test counts as audience.
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5)).add(0, 200, 0);
		acquireHorde(helper, level, 28, 2800, () -> {
			BlockPos perch = BlockPos.containing(center.x, center.y - 1, center.z);
			level.setBlockAndUpdate(perch, Blocks.STONE.defaultBlockState());
			player.teleportTo(center.x, center.y, center.z);
			UUID id = HordeManager.startHorde(level, center, player);
			helper.runAfterDelay(150, () -> {
				int before = HordeManager.liveMobCount(level);
				helper.assertTrue(before > 0, "wave 1 must be running while the player watches");
				player.teleportTo(center.x + 300, center.y, center.z);
				helper.runAfterDelay(100, () -> {
					helper.assertTrue(HordeManager.liveMobCount(level) == before,
							"no audience must freeze the horde (" + before + " -> "
									+ HordeManager.liveMobCount(level) + ")");
					// Reconcile is tick-driven: discarding during the freeze must not
					// shrink the live set until the horde ticks with an audience again.
					for (BaseHordeEntity m : hordeMobs(level, id)) {
						m.discard();
					}
					helper.assertTrue(HordeManager.liveMobCount(level) == before,
							"discards during the freeze must stay invisible until reconcile");
					player.teleportTo(center.x, center.y, center.z);
					helper.runAfterDelay(80, () -> {
						helper.assertTrue(HordeManager.liveMobCount(level) == 0,
								"reconcile must prune discarded mobs once ticking resumes");
						helper.assertTrue(HordeManager.getDebugStatus(level).contains("след. волна"),
								"a finished wave must enter the inter-wave countdown, got: "
										+ HordeManager.getDebugStatus(level));
						level.setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
						cleanupHorde(helper, level, player, id);
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3000)
	public void hordeMobDeathDropsFromLiveCount(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		acquireHorde(helper, level, 36, 2800, () -> {
			player.teleportTo(center.x, center.y + 2, center.z);
			UUID id = HordeManager.startHorde(level, center, player);
			helper.runAfterDelay(150, () -> {
				int before = HordeManager.liveMobCount(level);
				helper.assertTrue(before > 0, "wave 1 must be running");
				BaseHordeEntity victim = hordeMobs(level, id).get(0);
				victim.die(level.damageSources().generic());
				helper.assertTrue(HordeManager.liveMobCount(level) == before - 1,
						"a dying horde mob must leave the live set (" + before + " -> "
								+ HordeManager.liveMobCount(level) + ")");
				cleanupHorde(helper, level, player, id);
			});
		});
	}

	// ───────────────────────────────────── admin ops ──────────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3000)
	public void hordeAdminOpsActOnTheLevel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		acquireHorde(helper, level, 44, 2800, () -> {
			player.teleportTo(center.x, center.y + 2, center.z);
			UUID id = HordeManager.startHorde(level, center, player);
			helper.runAfterDelay(150, () -> {
				helper.assertTrue(HordeManager.liveMobCount(level) > 0, "wave 1 must be running");
				helper.assertTrue(HordeManager.getDebugStatus(level).contains("волна §e1"),
						"expected wave 1 in status: " + HordeManager.getDebugStatus(level));
				helper.assertTrue(HordeManager.forceNextWave(level),
						"forceNextWave must report success with an active horde");
				helper.assertTrue(HordeManager.getDebugStatus(level).contains("волна §e2"),
						"forceNextWave must jump to wave 2: " + HordeManager.getDebugStatus(level));
				helper.assertTrue(HordeManager.liveMobCount(level) > 0,
						"wave 2 mobs must spawn immediately after forceNextWave");
				int cleared = HordeManager.clearMobs(level);
				helper.assertTrue(cleared > 0 && HordeManager.liveMobCount(level) == 0,
						"clearMobs must empty the live set synchronously (cleared " + cleared + ")");
				helper.runAfterDelay(5, () -> {
					helper.assertTrue(HordeManager.getDebugStatus(level).contains("след. волна"),
							"empty wave must enter the inter-wave countdown, got: "
									+ HordeManager.getDebugStatus(level));
					helper.assertTrue(HordeManager.stopHorde(level),
							"stopHorde must report success with an active horde");
					helper.assertTrue(!HordeManager.hasActiveHorde(level),
							"stopped horde must no longer count as active");
					cleanupHorde(helper, level, player, id);
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3000)
	public void hordeCrystalItemStartsAndConsumes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		acquireHorde(helper, level, 52, 2800, () -> {
			ItemStack stack = new ItemStack(HordeItems.HORDE_CRYSTAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			InteractionResultHolder<ItemStack> first =
					HordeItems.HORDE_CRYSTAL.use(level, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(first.getResult() == InteractionResult.CONSUME,
					"first crystal use must consume, got " + first.getResult());
			helper.assertTrue(HordeManager.hasActiveHorde(level),
					"the crystal must start a horde in this level");
			helper.assertTrue(player.getMainHandItem().isEmpty(),
					"a successful use consumes the held crystal");

			ItemStack second = new ItemStack(HordeItems.HORDE_CRYSTAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, second);
			InteractionResultHolder<ItemStack> again =
					HordeItems.HORDE_CRYSTAL.use(level, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(again.getResult() == InteractionResult.FAIL,
					"a second use while a horde runs must fail, got " + again.getResult());
			helper.assertTrue(second.getCount() == 1, "a failed use must not consume the crystal");
			cleanupHorde(helper, level, player, null);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3000)
	public void hordeResetAllWipesState(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		acquireHorde(helper, level, 60, 2800, () -> {
			player.teleportTo(center.x, center.y + 2, center.z);
			UUID id = HordeManager.startHorde(level, center, player);
			helper.runAfterDelay(80, () -> {
				helper.assertTrue(HordeManager.hasActiveHorde(level), "horde must be running");
				helper.assertTrue(HordeManager.toggleOverlay(player), "overlay must toggle on");
				HordeManager.resetAll();
				helper.assertTrue(!HordeManager.hasActiveHorde(level),
						"resetAll must drop every tracked horde");
				helper.assertTrue(HordeManager.liveMobCount(level) == 0,
						"resetAll must zero the level's live count");
				// The overlay set is cleared too: toggling again reports "on".
				helper.assertTrue(HordeManager.toggleOverlay(player),
						"resetAll must clear the overlay set");
				HordeManager.toggleOverlay(player); // leave it off for other tests
				// Live mobs survive resetAll — they only had map bookkeeping.
				for (BaseHordeEntity m : hordeMobs(level, id)) {
					m.discard();
				}
				hordeSlotTaken = false;
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	// ─────────────────────────────────────── internals ────────────────────

	/**
	 * Runs {@code body} once this test holds the horde slot AND no unfinished
	 * horde exists in {@code level}. {@code settleTicks} is a per-test offset so
	 * two tests never observe a free level on the same tick and start together.
	 */
	private static void acquireHorde(GameTestHelper helper, ServerLevel level,
			int settleTicks, int triesLeft, Runnable body) {
		if (triesLeft <= 0) {
			helper.fail("timed out waiting for a free horde slot");
			return;
		}
		if (hordeSlotTaken || HordeManager.hasActiveHorde(level)) {
			helper.runAfterDelay(20,
					() -> acquireHorde(helper, level, settleTicks, triesLeft - 20, body));
			return;
		}
		if (settleTicks > 0) {
			helper.runAfterDelay(settleTicks,
					() -> acquireHorde(helper, level, 0, triesLeft - settleTicks, body));
			return;
		}
		hordeSlotTaken = true;
		body.run();
	}

	private static List<BaseHordeEntity> hordeMobs(ServerLevel level, UUID hordeId) {
		return level.getEntitiesOfClass(BaseHordeEntity.class,
				new AABB(-3.0E7, -1.0E4, -3.0E7, 3.0E7, 1.0E4, 3.0E7),
				m -> hordeId == null || hordeId.equals(m.getHordeId()));
	}

	private static void cleanupHorde(GameTestHelper helper, ServerLevel level,
			ServerPlayer player, UUID hordeId) {
		for (BaseHordeEntity m : hordeMobs(level, hordeId)) {
			m.discard();
		}
		HordeManager.stopHorde(level);
		hordeSlotTaken = false;
		TestPlayers.leave(player);
		helper.succeed();
	}
}
