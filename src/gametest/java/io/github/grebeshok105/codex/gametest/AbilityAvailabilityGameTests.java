package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.ability.AbilityAvailabilitySync;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability.Visibility;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.effect.DoomsdayProgress;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.RegulusMadnessState;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.hero.NarutoHero;
import io.github.grebeshok105.codex.hero.RegulusHero;
import io.github.grebeshok105.codex.hero.RemHero;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Stage C4 — the server computes {@code ability_availability} and writes the synced
 * attachment only when the answer changes. Covers the Doomsday tier gate end-to-end,
 * the write-on-change contract, the vanity-strip player rule, and heroless players.
 */
public class AbilityAvailabilityGameTests implements FabricGameTest {

	private static void tickSync(ServerPlayer player) {
		AbilityAvailabilitySync.tickPlayer(player.level().getServer(), player,
				player.getAttachedOrCreate(CoreAttachments.HERO_DATA));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void doomsdayTier1HidesDoomGrip(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, DoomsdayHero.ID);

		tickSync(player);
		AbilityAvailability availability = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(availability != null, "the sync task wrote the attachment");
		helper.assertValueEqual(availability.visibilityOf(AbilityIds.DOOMSDAY_DOOM_GRIP),
				Visibility.HIDDEN, "doom grip at tier 1");
		helper.assertValueEqual(availability.visibilityOf(AbilityIds.DOOMSDAY_SMASH),
				Visibility.HIDDEN, "smash needs tier 2");
		helper.assertTrue(availability.entries().size() == 6,
				"tier 1 hides all six tier-gated abilities, got " + availability.entries());

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void doomGripBecomesAvailableAtTier7(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, DoomsdayHero.ID);
		player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, DoomsdayProgress.EMPTY.withTier(7));

		tickSync(player);
		AbilityAvailability availability = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(availability == null || availability.entries().isEmpty(),
				"tier 7 unlocks everything — attachment stays absent or empty, got " + availability);

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void attachmentIsWrittenOnlyOnChange(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, DoomsdayHero.ID);

		tickSync(player);
		AbilityAvailability first = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(first != null, "first tick writes");

		tickSync(player);
		helper.assertTrue(player.getAttached(CoreAttachments.ABILITY_AVAILABILITY) == first,
				"an unchanged answer is not rewritten (same instance)");

		player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, DoomsdayProgress.EMPTY.withTier(7));
		tickSync(player);
		helper.assertTrue(player.getAttached(CoreAttachments.ABILITY_AVAILABILITY) != first,
				"a tier change writes a new value");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void remDemonAbilitiesNeedDemonism(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RemHero.ID);

		tickSync(player);
		AbilityAvailability availability = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(availability != null, "the sync task wrote the attachment");
		helper.assertValueEqual(availability.visibilityOf(AbilityIds.REM_MORNING_STAR),
				Visibility.HIDDEN, "demon-only ability without demonism");
		helper.assertValueEqual(availability.visibilityOf(AbilityIds.REM_ONI_RAGE),
				Visibility.AVAILABLE, "oni rage shows outside demon form");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void regulusCounterStrikeNeedsMadness(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);

		tickSync(player);
		AbilityAvailability sane = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(sane != null, "the sync task wrote the attachment");
		helper.assertValueEqual(sane.visibilityOf(AbilityIds.COUNTER_STRIKE),
				Visibility.HIDDEN, "counter strike hidden while sane");

		RegulusMadnessState madness = player.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS)
				.withMadness(true);
		player.setAttached(ModAttachments.REGULUS_MADNESS, madness);
		tickSync(player);
		AbilityAvailability mad = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(mad == null
				|| mad.visibilityOf(AbilityIds.COUNTER_STRIKE) == Visibility.AVAILABLE,
				"counter strike shows once the madness flag is set");

		player.setAttached(ModAttachments.REGULUS_MADNESS, madness.withMadness(false));
		tickSync(player);
		AbilityAvailability cleared = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(cleared != null
				&& cleared.visibilityOf(AbilityIds.COUNTER_STRIKE) == Visibility.HIDDEN,
				"counter strike hides again when madness ends");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void vanityStrippedHidesEverything(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.VANITY_STRIPPED, 200));

		tickSync(player);
		AbilityAvailability availability = player.getAttached(CoreAttachments.ABILITY_AVAILABILITY);
		helper.assertTrue(availability != null, "the sync task wrote the attachment");
		var abilities = Heroes.get(NarutoHero.ID).getAbilities();
		helper.assertTrue(availability.entries().size() == abilities.size(),
				"every Naruto ability hidden under vanity strip, got " + availability.entries());
		helper.assertTrue(availability.entries().values().stream().allMatch(v -> v == Visibility.HIDDEN),
				"all entries are HIDDEN");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void herolessPlayersNeverGetTheAttachment(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);

		tickSync(player);
		helper.assertTrue(player.getAttached(CoreAttachments.ABILITY_AVAILABILITY) == null,
				"no hero — nothing is ever written");

		TestPlayers.leave(player);
		helper.succeed();
	}
}
