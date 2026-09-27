package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.CreativeTabContents;
import io.github.grebeshok105.codex.content.boss.homelander.registry.HomelanderBossDamageTypes;
import io.github.grebeshok105.codex.damage.ModDamageTypes;
import io.github.grebeshok105.codex.content.boss.homelander.entity.HomelanderBossEntity;
import io.github.grebeshok105.codex.content.boss.homelander.entity.HomelanderBossEntities;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderBlockThrowGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderEyeLaserGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderFlightGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderGroundMagnetGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderHandClapGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderHeatVisionSweepGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderLightningCallGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderRoarGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderShockwaveDiveGoal;
import io.github.grebeshok105.codex.content.boss.homelander.entity.ai.HomelanderSonicSlamGoal;
import io.github.grebeshok105.codex.item.ModItemGroups;
import io.github.grebeshok105.codex.content.boss.homelander.HomelanderBossItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * IC2 phase-1 characterization: pins the Homelander BOSS's observable behavior so the
 * {@code entity/} + {@code entity/ai/} → {@code content/boss/homelander/} move can be
 * verified byte-equivalent in behavior. Covers registry ids, attributes and initial
 * cooldowns, the exact goalSelector/targetSelector contents, the vought_signal summon
 * (boss spawn, stack consumption, CONSUME result), the spawn egg's admin gating and
 * spawn path, the eight homelander_* damage types (registration, tags, and the
 * DamageSource helpers), the melee damage type, and live targeting + boss bar wiring.
 *
 * <p>The summon lightning bolt is intentionally not pinned: it is visual-only and
 * despawns within a few ticks, which races the shared-level section-visibility delay.
 */
public final class HomelanderBossGameTests implements FabricGameTest {

	// ─────────────────────────────── registration pins ────────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bossRegistrationsResolve(GameTestHelper helper) {
		helper.assertTrue(
				ModId.of("homelander_boss").equals(BuiltInRegistries.ENTITY_TYPE.getKey(HomelanderBossEntities.HOMELANDER_BOSS)),
				"homelander_boss EntityType must keep its registry id, got "
						+ BuiltInRegistries.ENTITY_TYPE.getKey(HomelanderBossEntities.HOMELANDER_BOSS));
		helper.assertTrue(
				ModId.of("vought_signal").equals(BuiltInRegistries.ITEM.getKey(HomelanderBossItems.VOUGHT_SIGNAL)),
				"vought_signal must keep its registry id, got "
						+ BuiltInRegistries.ITEM.getKey(HomelanderBossItems.VOUGHT_SIGNAL));
		helper.assertTrue(
				ModId.of("homelander_boss_spawn_egg").equals(BuiltInRegistries.ITEM.getKey(HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG)),
				"homelander_boss_spawn_egg must keep its registry id, got "
						+ BuiltInRegistries.ITEM.getKey(HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG));
		// Admin-only gating: in ADMIN_ONLY_ITEMS, never on the creative tab list.
		helper.assertTrue(ModItemGroups.ADMIN_ONLY_ITEMS.contains(HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG),
				"homelander_boss_spawn_egg must stay in ModItemGroups.ADMIN_ONLY_ITEMS");
		helper.assertTrue(!CreativeTabContents.all().contains(HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG),
				"homelander_boss_spawn_egg must not leak onto the creative tab");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bossAttributesAndCooldownsPinned(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HomelanderBossEntity boss = HomelanderBossEntities.HOMELANDER_BOSS.create(level);
		helper.assertTrue(boss != null, "HOMELANDER_BOSS.create() must instantiate");
		// getAttributeValue CLAMPS to each attribute's declared max (armor caps at 30) —
		// pin the constructor-set base values instead.
		helper.assertTrue(boss.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == 500.0D, "500 hp");
		helper.assertTrue(boss.getAttribute(Attributes.ARMOR).getBaseValue() == 100.0D, "100 armor");
		helper.assertTrue(boss.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue() == 12.0D, "12 toughness");
		helper.assertTrue(boss.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() == 14.0D, "14 damage");
		helper.assertTrue(boss.getAttribute(Attributes.ATTACK_KNOCKBACK).getBaseValue() == 2.0D, "2 attack knockback");
		helper.assertTrue(boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue() == 1.0D, "1 knockback resistance");
		helper.assertTrue(boss.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() == 0.8D, "0.8 movement speed");
		helper.assertTrue(boss.getAttribute(Attributes.FLYING_SPEED).getBaseValue() == 1.4D, "1.4 flying speed");
		helper.assertTrue(boss.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue() == 96.0D, "96 follow range");
		helper.assertTrue(boss.fireImmune(), "the boss is fire immune");
		helper.assertTrue(!boss.isPushable(), "the boss cannot be pushed");
		// Constructor-seeded cooldowns (laser is left at 0 — the only one ready immediately).
		helper.assertTrue(boss.getLaserCooldown() == 0, "laserCooldown starts at 0");
		helper.assertTrue(boss.getShockwaveCooldown() == 200, "shockwaveCooldown starts at 200");
		helper.assertTrue(boss.getSweepCooldown() == 240, "sweepCooldown starts at 240");
		helper.assertTrue(boss.getLightningCooldown() == 100, "lightningCooldown starts at 100");
		helper.assertTrue(boss.getSlamCooldown() == 160, "slamCooldown starts at 160");
		helper.assertTrue(boss.getMagnetCooldown() == 280, "magnetCooldown starts at 280");
		helper.assertTrue(boss.getThrowCooldown() == 200, "throwCooldown starts at 200");
		helper.assertTrue(boss.getRoarCooldown() == 220, "roarCooldown starts at 220");
		helper.assertTrue(boss.getHandClapCooldown() == 180, "handClapCooldown starts at 180");
		boss.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bossGoalSelectorsPinned(GameTestHelper helper) {
		HomelanderBossEntity boss = HomelanderBossEntities.HOMELANDER_BOSS.create(helper.getLevel());
		helper.assertTrue(boss != null, "HOMELANDER_BOSS.create() must instantiate");
		Set<Class<?>> goals = selectorOf(boss, "goalSelector").getAvailableGoals().stream()
				.map(w -> w.getGoal().getClass())
				.collect(Collectors.toSet());
		Set<Class<?>> expectedGoals = Set.of(
				HomelanderRoarGoal.class,
				HomelanderGroundMagnetGoal.class,
				HomelanderShockwaveDiveGoal.class,
				HomelanderHeatVisionSweepGoal.class,
				HomelanderSonicSlamGoal.class,
				HomelanderHandClapGoal.class,
				HomelanderEyeLaserGoal.class,
				HomelanderLightningCallGoal.class,
				HomelanderBlockThrowGoal.class,
				HomelanderFlightGoal.class,
				MeleeAttackGoal.class,
				LookAtPlayerGoal.class,
				RandomLookAroundGoal.class);
		helper.assertTrue(goals.equals(expectedGoals),
				"goalSelector must hold exactly the 13 wired goals, got " + goals);
		Set<Class<?>> targets = selectorOf(boss, "targetSelector").getAvailableGoals().stream()
				.map(w -> w.getGoal().getClass())
				.collect(Collectors.toSet());
		helper.assertTrue(targets.equals(Set.of(NearestAttackableTargetGoal.class)),
				"targetSelector must hold exactly one NearestAttackableTargetGoal, got " + targets);
		boss.discard();
		helper.succeed();
	}

	// ─────────────────────────────── summon paths ─────────────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void voughtSignalSummonsBoss(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		ItemStack stack = new ItemStack(HomelanderBossItems.VOUGHT_SIGNAL, 4);
		InteractionResult result = HomelanderBossItems.VOUGHT_SIGNAL.useOn(new UseOnContext(level, player,
				InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atBottomCenterOf(pos), net.minecraft.core.Direction.UP, pos, false)) {});
		helper.assertTrue(result == InteractionResult.CONSUME,
				"vought_signal useOn must return CONSUME, got " + result);
		helper.assertTrue(stack.getCount() == 3,
				"a survival summon must shrink the stack by one, got " + stack.getCount());
		awaitBoss(helper, box(pos), found -> {
			helper.assertTrue(found != null,
					"vought_signal must summon a homelander_boss above the clicked block");
			found.discard();
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void spawnEggSpawnsBoss(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		ItemStack stack = new ItemStack(HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG);
		InteractionResult result = HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG.useOn(new UseOnContext(level, player,
				InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atBottomCenterOf(pos), net.minecraft.core.Direction.UP, pos, false)) {});
		helper.assertTrue(result == InteractionResult.CONSUME,
				"the spawn egg useOn must return CONSUME, got " + result);
		helper.assertTrue(stack.getCount() == 0,
				"a survival spawn-egg use must consume the egg, got " + stack.getCount());
		awaitBoss(helper, box(pos), found -> {
			helper.assertTrue(found != null,
					"homelander_boss_spawn_egg must spawn a homelander_boss");
			helper.assertTrue(found.getType() == HomelanderBossEntities.HOMELANDER_BOSS,
					"the egg must spawn the HOMELANDER_BOSS entity type");
			found.discard();
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────────── damage types ─────────────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bossDamageTypesResolve(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ResourceKey<DamageType>> keys = List.of(
				HomelanderBossDamageTypes.HOMELANDER_EYE_LASER,
				HomelanderBossDamageTypes.HOMELANDER_HEAT_VISION,
				HomelanderBossDamageTypes.HOMELANDER_HAND_CLAP,
				HomelanderBossDamageTypes.HOMELANDER_SONIC_SLAM,
				HomelanderBossDamageTypes.HOMELANDER_SHOCKWAVE_DIVE,
				HomelanderBossDamageTypes.HOMELANDER_LIGHTNING_CALL,
				HomelanderBossDamageTypes.HOMELANDER_ROAR_BOSS,
				HomelanderBossDamageTypes.HOMELANDER_MELEE);
		for (ResourceKey<DamageType> key : keys) {
			Holder.Reference<DamageType> holder = level.registryAccess()
					.lookupOrThrow(Registries.DAMAGE_TYPE)
					.get(key)
					.orElseThrow(() -> new IllegalStateException(key + " not registered"));
			boolean inCooldownBypass = holder.is(DamageTypeTags.BYPASSES_COOLDOWN);
			boolean inBeam = holder.is(ModDamageTypes.BEAM);
			boolean expectedBypass = key != HomelanderBossDamageTypes.HOMELANDER_MELEE;
			boolean expectedBeam = key == HomelanderBossDamageTypes.HOMELANDER_EYE_LASER
					|| key == HomelanderBossDamageTypes.HOMELANDER_HEAT_VISION;
			helper.assertTrue(inCooldownBypass == expectedBypass,
					key.location() + " bypasses_cooldown membership must be " + expectedBypass);
			helper.assertTrue(inBeam == expectedBeam,
					key.location() + " #superheroes:beam membership must be " + expectedBeam);
		}
		// The DamageSource helpers the boss and its goals call must keep producing their types.
		HomelanderBossEntity boss = HomelanderBossEntities.HOMELANDER_BOSS.create(level);
		helper.assertTrue(HomelanderBossDamageTypes.eyeLaser(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_EYE_LASER),
				"homelanderEyeLaser must produce homelander_eye_laser");
		helper.assertTrue(HomelanderBossDamageTypes.heatVision(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_HEAT_VISION),
				"homelanderHeatVision must produce homelander_heat_vision");
		helper.assertTrue(HomelanderBossDamageTypes.handClap(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_HAND_CLAP),
				"homelanderHandClap must produce homelander_hand_clap");
		helper.assertTrue(HomelanderBossDamageTypes.sonicSlam(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_SONIC_SLAM),
				"homelanderSonicSlam must produce homelander_sonic_slam");
		helper.assertTrue(HomelanderBossDamageTypes.shockwaveDive(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_SHOCKWAVE_DIVE),
				"homelanderShockwaveDive must produce homelander_shockwave_dive");
		helper.assertTrue(HomelanderBossDamageTypes.lightningCall(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_LIGHTNING_CALL),
				"homelanderLightningCall must produce homelander_lightning_call");
		helper.assertTrue(HomelanderBossDamageTypes.roarBoss(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_ROAR_BOSS),
				"homelanderRoarBoss must produce homelander_roar_boss");
		helper.assertTrue(HomelanderBossDamageTypes.melee(level, boss).is(HomelanderBossDamageTypes.HOMELANDER_MELEE),
				"homelanderMelee must produce homelander_melee");
		boss.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bossMeleeUsesHomelanderMelee(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		TestPlayers.clearSpawnInvulnerability(player);
		HomelanderBossEntity boss = HomelanderBossEntities.HOMELANDER_BOSS.create(level);
		float before = player.getHealth();
		boolean hurt = boss.doHurtTarget(player);
		helper.assertTrue(hurt, "doHurtTarget must land on a vulnerable player");
		helper.assertTrue(player.getHealth() < before,
				"the hit must cost the player health (" + before + " -> " + player.getHealth() + ")");
		DamageSource last = player.getLastDamageSource();
		helper.assertTrue(last != null && last.is(HomelanderBossDamageTypes.HOMELANDER_MELEE),
				"the boss melee must deal homelander_melee damage, got " + last);
		boss.discard();
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ─────────────────────────── live targeting / bar ─────────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void bossTargetsPlayerAndShowsBar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.join(helper);
		Vec3 center = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		// The player must be inside the structure BEFORE the boss spawns, or the
		// nearest-hostile scan can pick up a player from a concurrent test.
		player.teleportTo(center.x, center.y, center.z);
		HomelanderBossEntity boss = HomelanderBossEntities.HOMELANDER_BOSS.create(level);
		helper.assertTrue(boss != null, "HOMELANDER_BOSS.create() must instantiate");
		boss.moveTo(center.x + 4.0, center.y + 1.0, center.z, 0.0f, 0.0f);
		level.addFreshEntity(boss);
		TestPlayers.awaitVisible(helper, boss, () -> helper.runAfterDelay(30, () -> {
			helper.assertTrue(boss.getTarget() == player,
					"the boss must target the player inside its structure, got " + boss.getTarget());
			ServerBossEvent bar = bossEventOf(boss);
			helper.assertTrue(bar.getPlayers().contains(player),
					"the boss bar must attach to the tracking player");
			helper.assertTrue(bar.getColor() == BossEvent.BossBarColor.GREEN,
					"boss bar color must stay GREEN, got " + bar.getColor());
			helper.assertTrue(bar.getOverlay() == BossEvent.BossBarOverlay.NOTCHED_10,
					"boss bar overlay must stay NOTCHED_10, got " + bar.getOverlay());
			boss.discard();
			TestPlayers.leave(player);
			helper.succeed();
		}));
	}

	// ─────────────────────────────── helpers ──────────────────────────────

	private static AABB box(BlockPos center) {
		return new AABB(center).inflate(10.0);
	}

	/**
	 * Fresh {@code addFreshEntity} spawns sit in a section until chunk tracking upgrades —
	 * poll for the boss like {@link TestPlayers#awaitVisible} polls for a uuid.
	 */
	private static void awaitBoss(GameTestHelper helper, AABB box, Consumer<HomelanderBossEntity> body) {
		awaitBoss(helper, box, 40, body);
	}

	private static void awaitBoss(GameTestHelper helper, AABB box, int tries, Consumer<HomelanderBossEntity> body) {
		List<HomelanderBossEntity> found = helper.getLevel().getEntitiesOfClass(HomelanderBossEntity.class, box);
		if (tries <= 0 || !found.isEmpty()) {
			body.accept(found.isEmpty() ? null : found.get(0));
			return;
		}
		helper.runAfterDelay(1, () -> awaitBoss(helper, box, tries - 1, body));
	}

	private static GoalSelector selectorOf(Mob mob, String name) {
		try {
			Field f = Mob.class.getDeclaredField(name);
			f.setAccessible(true);
			return (GoalSelector) f.get(mob);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static ServerBossEvent bossEventOf(HomelanderBossEntity boss) {
		try {
			Field f = HomelanderBossEntity.class.getDeclaredField("bossEvent");
			f.setAccessible(true);
			return (ServerBossEvent) f.get(boss);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
