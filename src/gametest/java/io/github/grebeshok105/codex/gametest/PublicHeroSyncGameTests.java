package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.NarutoHero;
import io.github.grebeshok105.codex.transform.HeroData;
import io.github.grebeshok105.codex.transform.HeroDataStore;
import io.github.grebeshok105.codex.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Audit B14 — the synced {@code PUBLIC_HERO} attachment mirrors {@code HERO_DATA.heroId}. */
public class PublicHeroSyncGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void transformPublishesHeroId(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);

		helper.assertValueEqual(NarutoHero.ID, player.getAttached(ModAttachments.PUBLIC_HERO),
				"transform writes the synced public hero id (audit B14)");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		helper.assertTrue(player.getAttached(ModAttachments.PUBLIC_HERO) == null,
				"untransform clears the synced public hero id");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void joinBackfillsPublicHeroForLegacyHero(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		// Legacy save: HERO_DATA written directly (pre-PUBLIC_HERO), no projection present.
		HeroData legacy = HeroDataStore.get(player)
				.withHero(NarutoHero.ID);
		player.setAttached(ModAttachments.HERO_DATA, legacy);
		helper.assertTrue(player.getAttached(ModAttachments.PUBLIC_HERO) == null,
				"precondition: no projection for a directly-written hero");

		HeroDataStore.syncPublicHero(player);
		helper.assertValueEqual(NarutoHero.ID, player.getAttached(ModAttachments.PUBLIC_HERO),
				"join back-fill derives the projection from HERO_DATA");

		TestPlayers.leave(player);
		helper.succeed();
	}
}
