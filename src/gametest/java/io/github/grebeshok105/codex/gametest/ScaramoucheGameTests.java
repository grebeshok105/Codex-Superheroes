package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Pins for the I1a Scaramouche module move: written against the pre-move layout and
 * must pass identically after {@code hero/scaramouche/} absorbs the server side.
 * Ability ids are literals because the constants move from {@code AbilityIds} to the
 * module. Zone expiry itself is already pinned by {@code windPrisonEndsWhenItsZoneExpires}.
 */
public final class ScaramoucheGameTests implements FabricGameTest {
	private static final ResourceLocation WIND_PRISON = ModId.of("scaramouche_wind_prison");

	@GameTest(template = EMPTY_STRUCTURE)
	public void scaramoucheOwnsItsFourAbilitiesPlusFlightInSlotOrder(GameTestHelper helper) {
		Hero scaramouche = Heroes.get(ModId.of("scaramouche"));
		helper.assertTrue(scaramouche != null, "scaramouche registered");
		helper.assertTrue(scaramouche.getAbilities().equals(List.of(ModId.of("flight"),
				ModId.of("scaramouche_windstep"), ModId.of("scaramouche_electro_swirl"), WIND_PRISON,
				ModId.of("scaramouche_skyfall_burst"))), "slot order " + scaramouche.getAbilities());
		for (ResourceLocation id : scaramouche.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void windPrisonZoneDiesWithTheHero(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("scaramouche"));

		AbilityRouter.activate(player, WIND_PRISON);
		helper.assertTrue(HeroDataStore.get(player).isActive(WIND_PRISON), "prison went active");
		helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "passives applied");

		HeroTransformService.forceUntransform(player);
		helper.assertFalse(HeroDataStore.get(player).isActive(WIND_PRISON),
				"untransform clears the active flag");
		helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING),
				"removePassives stripped the hero effects");

		// A re-transform must not resurrect stale prison state — the zone map has to die
		// with the hero (explicit clear on removePassives today).
		helper.runAfterDelay(25, () -> {
			TestHeroes.transform(player, ModId.of("scaramouche"));
			helper.assertFalse(HeroDataStore.get(player).isActive(WIND_PRISON),
					"no ghost activation after re-transform");
			// cooldowns survive hero swaps (audit B5) — clear so the pin tests the
			// zone map, not the cooldown gate
			AbilityCooldowns.clearAndSync(player);
			AbilityRouter.activate(player, WIND_PRISON);
			helper.assertTrue(HeroDataStore.get(player).isActive(WIND_PRISON),
					"a fresh prison activates cleanly");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}
}
