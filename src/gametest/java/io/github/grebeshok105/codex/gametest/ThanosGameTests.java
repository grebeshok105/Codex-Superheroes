package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.ThanosGauntletStateController;
import io.github.grebeshok105.codex.effect.ThanosSnapWindupController;
import io.github.grebeshok105.codex.item.infinity.InfinityGauntletData;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneItem;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;
import io.github.grebeshok105.codex.network.ThanosStonesS2CPayload;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Stage I5a characterization: pins Thanos' CURRENT observable server behavior on the
 * pre-move layout — the hero→stone reward table and its guard rows, gauntlet
 * insert/eject/transform use() paths, the per-stone ability gates, the 10-tick
 * stone→modifier scan and its join/broadcast payloads, the snap gate + 95-tick
 * windup + effect bundle + consumption, the tooltip contains-stone tail on the six
 * stone-bearing transformation items, and the block-breaking mixin semantics.
 * Hero/ability/item ids are literals because the constants move into the hero module
 * with it; {@code getCurrentStones}/{@code isWindingUp} are pinned through the
 * controllers' public API, not their storage.
 */
public final class ThanosGameTests implements FabricGameTest {
	private static final ResourceLocation THANOS = ModId.of("thanos");
	private static final ResourceLocation SNAP = ModId.of("thanos_snap");

	/** ability id -> the stone that unlocks it; the snap unlocks on all six. */
	private static final Object[][] ABILITY_STONES = {
			{ModId.of("thanos_cosmic_slam"), InfinityStoneType.POWER},
			{ModId.of("thanos_reality_tear"), InfinityStoneType.REALITY},
			{ModId.of("thanos_mind_pulse"), InfinityStoneType.MIND},
			{ModId.of("thanos_time_rewind"), InfinityStoneType.TIME},
			{ModId.of("thanos_space_portal"), InfinityStoneType.SPACE},
			{ModId.of("thanos_soul_pulse"), InfinityStoneType.SOUL}};

	/** victim hero id -> reward stone id, in kill order. */
	private static final String[][] HERO_STONES = {
			{"kratos", "power"}, {"captain_america", "soul"}, {"loki", "mind"},
			{"naruto", "space"}, {"sung_jinwoo", "reality"}, {"regulus", "time"}};

	/** transformation item id -> hero id it carries -> the advertised stone id. */
	private static final String[][] STONE_ITEMS = {
			{"blade_of_chaos", "kratos", "power"},
			{"captain_america_suit", "captain_america", "soul"},
			{"loki_scepter", "loki", "mind"},
			{"naruto_headband", "naruto", "space"},
			{"regulus_suit", "regulus", "time"},
			{"shadow_monarchs_cloak", "sung_jinwoo", "reality"}};

	// ─────────────────────────── registration ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void thanosOwnsItsSevenAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero hero = Heroes.get(THANOS);
		helper.assertTrue(hero != null, "the thanos hero is registered");
		List<ResourceLocation> expected = List.of(
				ModId.of("thanos_cosmic_slam"), ModId.of("thanos_reality_tear"),
				ModId.of("thanos_mind_pulse"), ModId.of("thanos_time_rewind"),
				ModId.of("thanos_space_portal"), ModId.of("thanos_soul_pulse"), SNAP);
		helper.assertTrue(expected.equals(hero.getAbilities()),
				"thanos ability slot order: expected " + expected + " got " + hero.getAbilities());
		for (ResourceLocation id : expected) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " is registered in AbilityRegistry");
		}
		helper.succeed();
	}

	// ─────────────────────────── stone rewards ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void stoneRewardsFollowTheSixRowTable(GameTestHelper helper) {
		ServerPlayer thanos = TestPlayers.join(helper, "t5-thanos");
		TestHeroes.transform(thanos, THANOS);
		List<ServerPlayer> victims = new ArrayList<>();
		for (int i = 0; i < HERO_STONES.length; i++) {
			ServerPlayer victim = TestPlayers.join(helper, "t5-v" + i);
			TestHeroes.transform(victim, ModId.of(HERO_STONES[i][0]));
			victims.add(victim);
		}
		// A second mapped victim exercises the alreadyHasStone guard.
		ServerPlayer secondCap = TestPlayers.join(helper, "t5-cap2");
		TestHeroes.transform(secondCap, ModId.of("captain_america"));
		victims.add(secondCap);
		helper.runAfterDelay(2, () -> {
			boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
			helper.getLevel().getServer().setPvpAllowed(true);
			try {
				for (int i = 0; i < HERO_STONES.length; i++) {
					ServerPlayer victim = victims.get(i);
					kill(thanos, victim);
					helper.assertTrue(!victim.isAlive(), HERO_STONES[i][0] + " victim died to the hit");
					helper.assertTrue(TestPlayers.count(thanos, item(HERO_STONES[i][1] + "_stone")) == 1,
							"killing " + HERO_STONES[i][0] + " grants " + HERO_STONES[i][1] + "_stone");
				}
				kill(thanos, secondCap);
				helper.assertTrue(!secondCap.isAlive(), "second cap victim died");
				helper.assertTrue(TestPlayers.count(thanos, item("soul_stone")) == 1,
						"alreadyHasStone blocks a second SOUL drop");
			} finally {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
			}
			for (ServerPlayer p : victims) {
				TestPlayers.leave(p);
			}
			TestPlayers.leave(thanos);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void stoneRewardNeedsThanosKillerAndMappedVictim(GameTestHelper helper) {
		ServerPlayer raiden = TestPlayers.join(helper, "t5-raiden");
		TestHeroes.transform(raiden, ModId.of("raiden_shogun"));
		ServerPlayer capForRaiden = TestPlayers.join(helper, "t5-cap-r");
		TestHeroes.transform(capForRaiden, ModId.of("captain_america"));

		ServerPlayer thanos = TestPlayers.join(helper, "t5-than-b");
		TestHeroes.transform(thanos, THANOS);
		ServerPlayer scorpionVictim = TestPlayers.join(helper, "t5-scp");
		TestHeroes.transform(scorpionVictim, ModId.of("scorpion")); // no stone row
		ServerPlayer plainVictim = TestPlayers.join(helper, "t5-pln"); // no hero at all

		// A thanos who already owns SOUL inside the gauntlet (not as a loose item).
		ServerPlayer thanosGauntlet = TestPlayers.join(helper, "t5-than-g");
		TestHeroes.transform(thanosGauntlet, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		InfinityGauntletData.tryInsert(gauntlet, InfinityStoneType.SOUL);
		thanosGauntlet.getInventory().add(gauntlet);
		ServerPlayer capForGauntlet = TestPlayers.join(helper, "t5-cap-g");
		TestHeroes.transform(capForGauntlet, ModId.of("captain_america"));

		helper.runAfterDelay(2, () -> {
			boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
			helper.getLevel().getServer().setPvpAllowed(true);
			try {
				kill(raiden, capForRaiden);
				kill(thanos, scorpionVictim);
				kill(thanos, plainVictim);
				kill(thanosGauntlet, capForGauntlet);
				helper.assertTrue(!capForRaiden.isAlive() && !scorpionVictim.isAlive()
						&& !plainVictim.isAlive() && !capForGauntlet.isAlive(), "all victims died");
				helper.assertTrue(countStones(raiden) == 0, "a non-thanos killer receives nothing");
				helper.assertTrue(countStones(thanos) == 0, "unmapped/heroless victims drop nothing");
				helper.assertTrue(countStones(thanosGauntlet) == 0,
						"a stone already inside the gauntlet blocks the drop");
			} finally {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
			}
			TestPlayers.leave(raiden);
			TestPlayers.leave(thanos);
			TestPlayers.leave(thanosGauntlet);
			TestPlayers.leave(capForRaiden);
			TestPlayers.leave(scorpionVictim);
			TestPlayers.leave(plainVictim);
			TestPlayers.leave(capForGauntlet);
			helper.succeed();
		});
	}

	// ─────────────────────────── gauntlet use() ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void gauntletUseTransformsToThanos(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-g1");
		Item gauntlet = item("infinity_gauntlet");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gauntlet));
		InteractionResult result = useGauntlet(helper, player);
		helper.assertTrue(result == InteractionResult.CONSUME, "empty-hand use transforms");
		helper.assertTrue(THANOS.equals(HeroDataStore.get(player).heroId()), "player is thanos now");
		result = useGauntlet(helper, player);
		helper.assertTrue(result == InteractionResult.FAIL,
				"re-using while already thanos fails (same hero, transform cooldown)");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void gauntletInsertsOffhandStone(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-g2");
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("power_stone")));
		InteractionResult result = useGauntlet(helper, player);
		helper.assertTrue(result == InteractionResult.CONSUME, "offhand stone inserts");
		helper.assertTrue(InfinityGauntletData.hasStone(gauntlet, InfinityStoneType.POWER),
				"gauntlet holds the inserted stone");
		helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).isEmpty(),
				"the offhand stone stack is consumed");
		helper.assertTrue(!HeroDataStore.get(player).hasHero(),
				"the insert path does not transform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void gauntletDuplicateStoneFallsThroughToTransform(GameTestHelper helper) {
		// The insert branch is skipped when the stone is already inside (or the gauntlet
		// is full — indistinguishable here since only six stone types exist), and use()
		// falls through to transform: fail for thanos, transform for anyone else.
		ServerPlayer thanos = TestPlayers.join(helper, "t5-g3t");
		TestHeroes.transform(thanos, THANOS);
		ItemStack full = new ItemStack(item("infinity_gauntlet"));
		InfinityGauntletData.tryInsert(full, InfinityStoneType.POWER);
		thanos.setItemInHand(InteractionHand.MAIN_HAND, full);
		thanos.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("power_stone")));
		helper.assertTrue(useGauntlet(helper, thanos) == InteractionResult.FAIL,
				"duplicate stone + thanos holder fails");
		helper.assertTrue(thanos.getItemInHand(InteractionHand.OFF_HAND).getCount() == 1
				&& InfinityGauntletData.getStones(full).size() == 1,
				"the refused insert leaves both stacks untouched");

		ServerPlayer nobody = TestPlayers.join(helper, "t5-g3n");
		ItemStack half = new ItemStack(item("infinity_gauntlet"));
		InfinityGauntletData.tryInsert(half, InfinityStoneType.POWER);
		nobody.setItemInHand(InteractionHand.MAIN_HAND, half);
		nobody.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("power_stone")));
		helper.assertTrue(useGauntlet(helper, nobody) == InteractionResult.CONSUME,
				"duplicate stone falls through to transform");
		helper.assertTrue(THANOS.equals(HeroDataStore.get(nobody).heroId()),
				"the fall-through transforms into thanos");
		helper.assertTrue(nobody.getItemInHand(InteractionHand.OFF_HAND).getCount() == 1,
				"the duplicate stone is not eaten by the fall-through");
		TestPlayers.leave(thanos);
		TestPlayers.leave(nobody);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void gauntletEjectsLastStoneThenUntransforms(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-g4");
		TestHeroes.transform(player, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		InfinityGauntletData.tryInsert(gauntlet, InfinityStoneType.POWER);
		InfinityGauntletData.tryInsert(gauntlet, InfinityStoneType.MIND);
		player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
		// untransform shares the 20-tick transform cooldown — wait it out.
		helper.runAfterDelay(22, () -> {
			player.setShiftKeyDown(true);
			helper.assertTrue(useGauntlet(helper, player) == InteractionResult.CONSUME,
					"shift-use ejects a stone");
			helper.assertTrue(InfinityGauntletData.hasStone(gauntlet, InfinityStoneType.POWER)
							&& !InfinityGauntletData.hasStone(gauntlet, InfinityStoneType.MIND),
					"eject pops the LAST inserted stone (LIFO)");
			helper.assertTrue(TestPlayers.count(player, item("mind_stone")) == 1,
					"the ejected stone lands in the inventory");
			helper.assertTrue(useGauntlet(helper, player) == InteractionResult.CONSUME,
					"second shift-use ejects the remaining stone");
			helper.assertTrue(InfinityGauntletData.getStones(gauntlet).isEmpty()
							&& TestPlayers.count(player, item("power_stone")) == 1,
					"both stones came back");
			helper.assertTrue(useGauntlet(helper, player) == InteractionResult.CONSUME,
					"shift-use on an empty gauntlet untransforms");
			helper.assertTrue(!HeroDataStore.get(player).hasHero(), "player is no longer thanos");
			player.setShiftKeyDown(false);
			helper.assertTrue(useGauntlet(helper, player) == InteractionResult.FAIL,
					"untransform also marks the 20-tick cooldown");
			player.setShiftKeyDown(true);
			helper.assertTrue(useGauntlet(helper, player) == InteractionResult.FAIL,
					"empty-gauntlet shift-use on a non-thanos fails");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void gauntletDataCapsAtSixAndEjectsLifo(GameTestHelper helper) {
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		helper.assertTrue(InfinityGauntletData.getStones(gauntlet).isEmpty(), "fresh gauntlet is empty");
		helper.assertTrue(InfinityGauntletData.ejectLast(gauntlet) == null, "eject on empty returns null");
		for (InfinityStoneType type : InfinityStoneType.values()) {
			helper.assertTrue(InfinityGauntletData.tryInsert(gauntlet, type), "first insert of " + type);
		}
		helper.assertTrue(InfinityGauntletData.isFull(gauntlet), "six stones fill the gauntlet");
		helper.assertTrue(!InfinityGauntletData.tryInsert(gauntlet, InfinityStoneType.POWER),
				"a duplicate insert is refused");
		helper.assertTrue(InfinityGauntletData.getStones(gauntlet).equals(List.of(InfinityStoneType.values())),
				"stones keep insertion (enum) order");
		helper.assertTrue(gauntlet.get(DataComponents.CUSTOM_MODEL_DATA) != null
						&& gauntlet.get(DataComponents.CUSTOM_MODEL_DATA).value() == 6,
				"CUSTOM_MODEL_DATA tracks the stone count");
		helper.assertTrue(InfinityGauntletData.ejectLast(gauntlet) == InfinityStoneType.MIND,
				"eject returns the last inserted stone");
		InfinityGauntletData.clearStones(gauntlet);
		helper.assertTrue(InfinityGauntletData.getStones(gauntlet).isEmpty(), "clearStones empties it");
		helper.succeed();
	}

	// ─────────────────────────── stone gates ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void thanosAbilitiesGateOnTheirStones(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-gate");
		TestHeroes.transform(player, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		player.getInventory().add(gauntlet);
		Hero hero = Heroes.get(THANOS);
		// A non-thanos holder scans as an empty set — getCurrentStones is hero-scoped.
		ServerPlayer other = TestPlayers.join(helper, "t5-gate-o");
		ItemStack foreign = new ItemStack(item("infinity_gauntlet"));
		InfinityGauntletData.tryInsert(foreign, InfinityStoneType.POWER);
		other.getInventory().add(foreign);
		helper.assertTrue(ThanosGauntletStateController.getCurrentStones(other).isEmpty(),
				"the gauntlet scan is thanos-only");
		for (InfinityStoneType stone : InfinityStoneType.values()) {
			InfinityGauntletData.clearStones(gauntlet);
			helper.assertTrue(InfinityGauntletData.tryInsert(gauntlet, stone), "insert " + stone);
			helper.assertTrue(EnumSet.of(stone).equals(ThanosGauntletStateController.getCurrentStones(player)),
					"getCurrentStones sees exactly " + stone);
			for (Object[] row : ABILITY_STONES) {
				ResourceLocation abilityId = (ResourceLocation) row[0];
				boolean expected = row[1] == stone;
				helper.assertTrue(hero.canUseAbility(player, HeroDataStore.get(player), abilityId) == expected,
						abilityId + " usable iff " + row[1] + " is inserted (have " + stone + ")");
			}
			helper.assertTrue(!hero.canUseAbility(player, HeroDataStore.get(player), SNAP),
					"the snap is locked below six stones");
		}
		for (InfinityStoneType stone : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(gauntlet, stone);
		}
		helper.assertTrue(hero.canUseAbility(player, HeroDataStore.get(player), SNAP),
				"the snap unlocks at six stones");
		TestPlayers.leave(player);
		TestPlayers.leave(other);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void insertedStonesApplyTheirModifierTable(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-mod");
		TestHeroes.transform(player, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		for (InfinityStoneType type : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(gauntlet, type);
		}
		player.getInventory().add(gauntlet);
		// The stone scan runs on a 10-tick cadence (server.getTickCount() % 10).
		awaitTrue(helper, () -> modifier(player, InfinityStoneType.POWER) != null, 25, () -> {
			for (InfinityStoneType type : InfinityStoneType.values()) {
				AttributeModifier mod = modifier(player, type);
				helper.assertTrue(mod != null, type + " modifier applied");
				helper.assertTrue(mod.amount() == type.getAmount(),
						type + " amount " + type.getAmount() + " (was " + mod.amount() + ")");
				helper.assertTrue(mod.operation() == type.getOperation(),
						type + " operation " + type.getOperation());
			}
			InfinityGauntletData.clearStones(gauntlet);
			awaitTrue(helper, () -> modifier(player, InfinityStoneType.POWER) == null
							&& modifier(player, InfinityStoneType.SOUL) == null, 25, () -> {
				for (InfinityStoneType type : InfinityStoneType.values()) {
					helper.assertTrue(modifier(player, type) == null,
							type + " modifier removed once the stone leaves the gauntlet");
				}
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untransformStripsStoneModifiers(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-strip");
		TestHeroes.transform(player, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		InfinityGauntletData.tryInsert(gauntlet, InfinityStoneType.POWER);
		player.getInventory().add(gauntlet);
		awaitTrue(helper, () -> modifier(player, InfinityStoneType.POWER) != null, 25, () -> {
			HeroTransformService.forceUntransform(player);
			helper.assertTrue(!HeroDataStore.get(player).hasHero(), "untransformed");
			helper.assertTrue(modifier(player, InfinityStoneType.POWER) == null,
					"removePassives strips the stone modifiers synchronously");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── the snap ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapGateNeedsSixStonesAndRespectsDisabledAbilities(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "t5-gate6");
		TestHeroes.transform(player, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		for (InfinityStoneType type : InfinityStoneType.values()) {
			if (type != InfinityStoneType.MIND) {
				InfinityGauntletData.tryInsert(gauntlet, type);
			}
		}
		player.getInventory().add(gauntlet);
		AbilityRouter.activate(player, SNAP);
		helper.assertTrue(!ThanosSnapWindupController.isWindingUp(player)
						&& !AbilityCooldowns.isOnCooldown(player, SNAP),
				"the snap refuses below six stones");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 400f) < 0.01f,
				"a refused snap spends no energy");
		// DISABLED_ABILITIES blocks every ability before the stone check.
		player.addEffect(new MobEffectInstance(ModEffects.DISABLED_ABILITIES, 200, 0));
		InfinityGauntletData.tryInsert(gauntlet, InfinityStoneType.MIND);
		AbilityRouter.activate(player, SNAP);
		helper.assertTrue(!ThanosSnapWindupController.isWindingUp(player),
				"DISABLED_ABILITIES blocks the snap even with six stones");
		player.removeEffect(ModEffects.DISABLED_ABILITIES);
		AbilityRouter.activate(player, SNAP);
		helper.assertTrue(ThanosSnapWindupController.isWindingUp(player), "six stones arm the windup");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, SNAP), "activation arms the 1800-tick cooldown");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - 50f) < 0.01f,
				"the snap costs 350 of 400 energy");
		// Leaving drops the pending snap (ClearOn.LEAVE) — it never fires.
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapFiresAtWindupTickAndAppliesTheBundle(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper, "t5-snap");
		TestHeroes.transform(caster, THANOS);
		ServerPlayer victim = TestPlayers.join(helper, "t5-snap-v");
		// +6000/-6000 — unique per test so the 128-radius scan only sees this victim.
		double x = caster.getX() + 6000.0;
		double z = caster.getZ() - 6000.0;
		teleportFar(helper, caster, x, z);
		teleportFar(helper, victim, x + 2.0, z + 2.0);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		for (InfinityStoneType type : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(gauntlet, type);
		}
		caster.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.runAfterDelay(2, () -> {
			try {
				// the harmableBy scan runs at fire time — pvp must stay on until then
				helper.getLevel().getServer().setPvpAllowed(true);
				AbilityRouter.activate(caster, SNAP);
				helper.assertTrue(ThanosSnapWindupController.isWindingUp(caster), "windup scheduled");
			} catch (Throwable t) {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
				throw t;
			}
		});
		helper.runAfterDelay(90, () -> {
			try {
				helper.assertFalse(victim.hasEffect(ModEffects.SNAPPED),
						"the snap has not fired before tick ~95 of the windup");
				helper.assertTrue(ThanosSnapWindupController.isWindingUp(caster), "still winding up");
			} catch (Throwable t) {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
				throw t;
			}
		});
		helper.runAfterDelay(125, () -> {
			try {
				// windup scheduled at ~T+2: fires at ~T+97, entry drops at ~T+117.
				assertEffect(helper, victim, MobEffects.MOVEMENT_SLOWDOWN, 4);
				assertEffect(helper, victim, MobEffects.WEAKNESS, 4);
				assertEffect(helper, victim, MobEffects.DIG_SLOWDOWN, 4);
				assertEffect(helper, victim, MobEffects.BLINDNESS, 0);
				assertEffect(helper, victim, MobEffects.CONFUSION, 0);
				assertEffect(helper, victim, ModEffects.SNAPPED, 0);
				assertEffect(helper, victim, ModEffects.DISABLED_ABILITIES, 0);
				assertEffect(helper, victim, ModEffects.HEAL_BLOCK, 0);
				helper.assertTrue(InfinityGauntletData.getStones(gauntlet).isEmpty(),
						"the snap consumes every stone in the gauntlet");
				helper.assertTrue(!ThanosSnapWindupController.isWindingUp(caster),
						"the windup entry is gone after the end tick");
			} finally {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
			}
			TestPlayers.leave(caster);
			TestPlayers.leave(victim);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapPendingDropsWhenCasterLeaves(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper, "t5-leave");
		ServerPlayer victim = TestPlayers.join(helper, "t5-leave-v");
		ThanosSnapWindupController.schedule(caster, 30, 60);
		helper.assertTrue(ThanosSnapWindupController.isWindingUp(caster), "pending snap registered");
		TestPlayers.leave(caster);
		ServerPlayer rejoined = TestPlayers.rejoin(helper, caster);
		helper.assertTrue(!ThanosSnapWindupController.isWindingUp(rejoined),
				"relogging drops the pending snap");
		helper.runAfterDelay(65, () -> {
			helper.assertFalse(victim.hasEffect(ModEffects.SNAPPED), "the dropped snap never fires");
			TestPlayers.leave(rejoined);
			TestPlayers.leave(victim);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapStillFiresAfterCasterUntransforms(GameTestHelper helper) {
		// Pins CURRENT lifecycle policy: the pending-snap map only clears on LEAVE/DEATH,
		// so a snap armed before untransform still goes off.
		ServerPlayer caster = TestPlayers.join(helper, "t5-utrans");
		TestHeroes.transform(caster, THANOS);
		ServerPlayer victim = TestPlayers.join(helper, "t5-utrans-v");
		double x = caster.getX() - 8000.0;
		double z = caster.getZ() + 8000.0;
		teleportFar(helper, caster, x, z);
		teleportFar(helper, victim, x + 2.0, z + 2.0);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		for (InfinityStoneType type : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(gauntlet, type);
		}
		caster.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.runAfterDelay(2, () -> {
			try {
				helper.getLevel().getServer().setPvpAllowed(true);
				AbilityRouter.activate(caster, SNAP);
				helper.assertTrue(ThanosSnapWindupController.isWindingUp(caster), "windup scheduled");
				helper.assertTrue(HeroTransformService.forceUntransform(caster), "untransformed mid-windup");
			} catch (Throwable t) {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
				throw t;
			}
		});
		helper.runAfterDelay(120, () -> {
			try {
				helper.assertTrue(victim.hasEffect(ModEffects.SNAPPED),
						"the snap fires even though the caster lost the hero (PENDING is LEAVE/DEATH-only)");
			} finally {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
			}
			TestPlayers.leave(caster);
			TestPlayers.leave(victim);
			helper.succeed();
		});
	}

	// ─────────────────────────── stone sync payloads ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void joiningPlayerReplaysThanosMask(GameTestHelper helper) {
		ServerPlayer thanos = TestPlayers.join(helper, "t5-sync");
		TestHeroes.transform(thanos, THANOS);
		ItemStack gauntlet = new ItemStack(item("infinity_gauntlet"));
		for (InfinityStoneType type : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(gauntlet, type);
		}
		thanos.getInventory().add(gauntlet);
		// Wait for a scan so APPLIED holds this thanos' mask before the watcher joins.
		awaitTrue(helper, () -> modifier(thanos, InfinityStoneType.POWER) != null, 25, () -> {
			Wire watcher = joinAudible(helper, "t5-watch");
			awaitStonesMask(helper, watcher.channel(), thanos.getUUID(), 63, 15, () -> {
				InfinityGauntletData.clearStones(gauntlet);
				// The next 10-tick scan diffs the set and broadcasts an empty mask.
				awaitStonesMask(helper, watcher.channel(), thanos.getUUID(), 0, 25, () -> {
					TestPlayers.leave(thanos);
					TestPlayers.leave(watcher.player());
					helper.succeed();
				});
			});
		});
	}

	// ─────────────────────────── stone-bearing items ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void stoneBearingItemsExposeHeroAndStone(GameTestHelper helper) {
		for (String[] row : STONE_ITEMS) {
			Item item = item(row[0]);
			helper.assertTrue(item instanceof TransformationItem, row[0] + " is a TransformationItem");
			helper.assertTrue(ModId.of(row[1]).equals(((TransformationItem) item).getHeroId()),
					row[0] + " carries hero " + row[1]);
		}
		for (InfinityStoneType type : InfinityStoneType.values()) {
			Item stone = item(type.getItemRegistryName());
			helper.assertTrue(stone instanceof InfinityStoneItem, type.getItemRegistryName() + " is an InfinityStoneItem");
			helper.assertTrue(((InfinityStoneItem) stone).getStoneType() == type,
					type.getItemRegistryName() + " reports " + type);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void stoneItemTooltipsEndWithTheContainsStoneLine(GameTestHelper helper) {
		for (String[] row : STONE_ITEMS) {
			Item item = item(row[0]);
			List<Component> tooltip = new ArrayList<>();
			item.appendHoverText(new ItemStack(item), Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
			int size = tooltip.size();
			helper.assertTrue(size >= 3, row[0] + " tooltip has a stone-line tail");
			helper.assertTrue("DIVIDER".equals(serializeLine(tooltip.get(size - 1))),
					row[0] + " closes with a divider");
			helper.assertTrue(("tooltip.superheroes.contains_stone@GRAY+item.superheroes."
							+ row[2] + "_stone@LIGHT_PURPLE").equals(serializeLine(tooltip.get(size - 2))),
					row[0] + " advertises its " + row[2] + " stone just above the close divider");
			helper.assertTrue("EMPTY".equals(serializeLine(tooltip.get(size - 3))),
					row[0] + " has a blank spacer before the stone line");
		}
		// The gauntlet's own lore: stone count line, per-stone bullets and the "full" flag.
		ItemStack empty = new ItemStack(item("infinity_gauntlet"));
		List<Component> tooltip = new ArrayList<>();
		empty.getItem().appendHoverText(empty, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
		helper.assertTrue(tooltipContains(tooltip, "item.superheroes.infinity_gauntlet.stones@AQUA"),
				"gauntlet tooltip has the stones counter");
		helper.assertTrue(!tooltipContains(tooltip, "item.superheroes.infinity_gauntlet.full@LIGHT_PURPLE"),
				"gauntlet tooltip hides the full line when empty");
		ItemStack fullGauntlet = new ItemStack(item("infinity_gauntlet"));
		for (InfinityStoneType type : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(fullGauntlet, type);
		}
		tooltip.clear();
		fullGauntlet.getItem().appendHoverText(fullGauntlet, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
		helper.assertTrue(tooltipContains(tooltip, "item.superheroes.infinity_gauntlet.full@LIGHT_PURPLE"),
				"gauntlet tooltip shows the full line at six stones");
		helper.assertTrue(tooltipContains(tooltip, "item.superheroes.power_stone@B44CFF")
						&& tooltipContains(tooltip, "item.superheroes.mind_stone@FFE048"),
				"gauntlet tooltip lists each inserted stone in its own color");
		helper.succeed();
	}

	// ─────────────────────────── block breaking ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void thanosCountsAsHoldingADiamondPick(GameTestHelper helper) {
		ServerPlayer thanos = TestPlayers.join(helper, "t5-mine");
		TestHeroes.transform(thanos, THANOS);
		ServerPlayer miner = TestPlayers.join(helper, "t5-miner");
		helper.assertTrue(thanos.hasCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
				"thanos harvests obsidian barehanded");
		helper.assertFalse(miner.hasCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
				"a non-thanos player cannot");
		helper.assertTrue(thanos.getDestroySpeed(Blocks.DEEPSLATE.defaultBlockState()) >= 8f,
				"thanos digs at least at the 8.0 floor (diamond-pick speed)");
		helper.assertTrue(miner.getDestroySpeed(Blocks.DEEPSLATE.defaultBlockState()) < 8f,
				"a barehanded player digs at ~1.0");
		TestPlayers.leave(thanos);
		TestPlayers.leave(miner);
		helper.succeed();
	}

	// ─────────────────────────── helpers ───────────────────────────

	private static void kill(ServerPlayer killer, ServerPlayer victim) {
		TestPlayers.clearSpawnInvulnerability(victim);
		victim.hurt(killer.damageSources().mobAttack(killer), victim.getMaxHealth() * 10f);
	}

	private static int countStones(ServerPlayer player) {
		int n = 0;
		for (InfinityStoneType type : InfinityStoneType.values()) {
			n += TestPlayers.count(player, item(type.getItemRegistryName()));
		}
		return n;
	}

	private static Item item(String path) {
		Item item = BuiltInRegistries.ITEM.get(ModId.of(path));
		if (item == Items.AIR) {
			throw new IllegalStateException("unregistered item " + path);
		}
		return item;
	}

	private static InteractionResult useGauntlet(GameTestHelper helper, ServerPlayer player) {
		return player.getItemInHand(InteractionHand.MAIN_HAND).getItem()
				.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
	}

	private static void teleportFar(GameTestHelper helper, ServerPlayer player, double x, double z) {
		// Force-load the destination chunk first: entities in an unloaded chunk never
		// register for area scans, so the snap's getEntitiesOfClass would miss the victim.
		helper.getLevel().getChunk(BlockPos.containing(x, player.getY(), z));
		player.teleportTo(x, player.getY(), z);
	}

	private static AttributeModifier modifier(ServerPlayer player, InfinityStoneType type) {
		AttributeInstance attribute = player.getAttribute(type.getAttribute());
		return attribute == null ? null : attribute.getModifier(type.getModifierId());
	}

	private static void assertEffect(GameTestHelper helper, ServerPlayer victim,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		MobEffectInstance instance = victim.getEffect(effect);
		helper.assertTrue(instance != null, "victim received " + effect);
		helper.assertTrue(instance.getAmplifier() == amplifier,
				effect + " amplifier " + amplifier + " (was " + instance.getAmplifier() + ")");
		helper.assertTrue(instance.getDuration() >= 140 && instance.getDuration() <= 200,
				effect + " duration ~200 ticks (was " + instance.getDuration() + ")");
	}

	private static void awaitTrue(GameTestHelper helper, BooleanSupplier cond, int tries, Runnable body) {
		if (tries <= 0 || cond.getAsBoolean()) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitTrue(helper, cond, tries - 1, body));
	}

	private static boolean tooltipContains(List<Component> tooltip, String serialized) {
		for (Component line : tooltip) {
			if (serialized.equals(serializeLine(line))) {
				return true;
			}
		}
		return false;
	}

	// Same serialization the transformation-lore golden uses (copy — the helper is private there).
	private static String serializeLine(Component line) {
		if (line.getContents() instanceof PlainTextContents plain && line.getSiblings().isEmpty()) {
			String text = plain.text();
			if (text.isEmpty()) {
				return "EMPTY";
			}
			if (text.chars().allMatch(c -> c == '━')) {
				return "DIVIDER";
			}
		}
		List<String> parts = new ArrayList<>();
		collectTranslatables(line, parts);
		if (parts.isEmpty()) {
			return line.getString();
		}
		return String.join("+", parts);
	}

	private static void collectTranslatables(Component component, List<String> out) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			out.add(translatable.getKey() + "@" + colorName(component.getStyle().getColor()));
		}
		for (Component sibling : component.getSiblings()) {
			collectTranslatables(sibling, out);
		}
	}

	private static String colorName(TextColor color) {
		if (color == null) {
			return "NONE";
		}
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (formatting.isColor() && Objects.equals(formatting.getColor(), color.getValue())) {
				return formatting.getName().toUpperCase(Locale.ROOT);
			}
		}
		return color.serialize();
	}

	// ─────────────────────────── packet capture ───────────────────────────

	/** A joined player whose raw outbound packets stay readable for assertions. */
	private record Wire(ServerPlayer player, EmbeddedChannel channel) {
	}

	private static Wire joinAudible(GameTestHelper helper, String name) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				profile, cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		// Connection.send dispatches writes via eventLoop().execute() when called off
		// the embedded loop; without running pending tasks the packets sit queued and
		// readOutbound sees nothing — delivery timing is otherwise racy.
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static boolean hasStonesMask(List<Object> packets, UUID playerId, int mask) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof ThanosStonesS2CPayload payload
					&& payload.playerId().equals(playerId) && payload.bitmask() == mask) {
				return true;
			}
		}
		return false;
	}

	/** Polls the wire each tick until the stones payload shows up or the budget runs out. */
	private static void awaitStonesMask(GameTestHelper helper, EmbeddedChannel channel,
			UUID playerId, int mask, int tries, Runnable done) {
		if (tries <= 0) {
			helper.fail("thanos_stones payload for " + playerId + " (mask " + mask + ") never arrived");
			return;
		}
		if (hasStonesMask(drain(channel), playerId, mask)) {
			done.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitStonesMask(helper, channel, playerId, mask, tries - 1, done));
	}
}
