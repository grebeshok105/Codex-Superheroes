package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.hero.scaramouche.ScaramoucheAbilities;
import io.github.grebeshok105.codex.hero.scaramouche.ScaramoucheHero;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** The router alone gates a cooling-down ability: activation is a silent no-op that charges nothing. */
public final class CooldownGateGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void routerRejectsAbilityOnCooldown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ScaramoucheHero.ID);

		// Electro Swirl is a plain cast — no toggle, no target precondition — so the
		// first activation must run and pay its energy cost.
		float energyAtStart = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, ScaramoucheAbilities.SCARAMOUCHE_ELECTRO_SWIRL);
		float energyAfterFirst = HeroDataStore.get(player).energy();
		helper.assertTrue(energyAfterFirst < energyAtStart,
				"first activation ran and charged its cost");

		AbilityCooldowns.setCooldownTicks(player, ScaramoucheAbilities.SCARAMOUCHE_ELECTRO_SWIRL, 100);
		AbilityRouter.activate(player, ScaramoucheAbilities.SCARAMOUCHE_ELECTRO_SWIRL);

		helper.assertTrue(HeroDataStore.get(player).energy() == energyAfterFirst,
				"on-cooldown activation is a no-op and charges nothing");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
