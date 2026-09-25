package com.example.superheroes.gametest;

import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.hero.ScaramoucheHero;
import com.example.superheroes.transform.HeroDataStore;
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
		AbilityRouter.activate(player, AbilityIds.SCARAMOUCHE_ELECTRO_SWIRL);
		float energyAfterFirst = HeroDataStore.get(player).energy();
		helper.assertTrue(energyAfterFirst < energyAtStart,
				"first activation ran and charged its cost");

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.SCARAMOUCHE_ELECTRO_SWIRL, 100);
		AbilityRouter.activate(player, AbilityIds.SCARAMOUCHE_ELECTRO_SWIRL);

		helper.assertTrue(HeroDataStore.get(player).energy() == energyAfterFirst,
				"on-cooldown activation is a no-op and charges nothing");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
