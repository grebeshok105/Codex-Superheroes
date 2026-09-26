package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.ability.AbilityRouter;
import io.github.grebeshok105.codex.effect.MirrorDimensionController;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.hero.HomelanderHero;
import io.github.grebeshok105.codex.hero.IronManHero;
import io.github.grebeshok105.codex.hero.PandoraHero;
import io.github.grebeshok105.codex.hero.ScaramoucheHero;
import io.github.grebeshok105.codex.resource.ResourceKind;
import io.github.grebeshok105.codex.transform.HeroDataStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Plan 2 stage C2.1: characterization of every gate {@link AbilityRouter} applies on
 * activation — hero-state blockers (Snap, Vanity strip, aftermath), hero hooks (Doomsday
 * tiers, Pandora's House, Iron Fists suppression) and the Unibeam energy reserve. Every
 * assertion pins current behavior and must pass before and after the effect rules are
 * extracted in C2.2.
 */
public final class AbilityGateGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapDisabledPlayerCannotActivate(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ScaramoucheHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.DISABLED_ABILITIES, 200));
		float before = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, AbilityIds.SCARAMOUCHE_WIND_PRISON);
		helper.assertFalse(HeroDataStore.get(player).isActive(AbilityIds.SCARAMOUCHE_WIND_PRISON), "blocked by Snap");
		helper.assertTrue(HeroDataStore.get(player).energy() == before, "nothing charged");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void madnessMakesActivationFree(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.MADNESS, 200));
		HeroDataStore.update(player, d -> d.withResources(0f, d.mana()));
		AbilityRouter.activate(player, AbilityIds.STUNNING_ROAR);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.STUNNING_ROAR), "madness pays for the roar");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void vanityStrippedPlayerCannotActivate(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ScaramoucheHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.VANITY_STRIPPED, 200));
		float before = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, AbilityIds.SCARAMOUCHE_WIND_PRISON);
		helper.assertFalse(HeroDataStore.get(player).isActive(AbilityIds.SCARAMOUCHE_WIND_PRISON),
				"blocked by Vanity strip");
		helper.assertTrue(HeroDataStore.get(player).energy() == before, "nothing charged");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aftermathBlocksSilently(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ScaramoucheHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.MADNESS_AFTERMATH, 200));
		float before = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, AbilityIds.SCARAMOUCHE_WIND_PRISON);
		helper.assertFalse(HeroDataStore.get(player).isActive(AbilityIds.SCARAMOUCHE_WIND_PRISON),
				"aftermath blocks activation");
		helper.assertTrue(HeroDataStore.get(player).energy() == before, "nothing charged");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void lockedDoomsdayTierAbilityIsRejectedSilently(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, DoomsdayHero.ID);
		helper.assertTrue(DoomsdayHero.getTier(player) == 1, "fresh Doomsday starts at tier 1");
		float before = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, AbilityIds.DOOMSDAY_DOOM_GRIP);
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, AbilityIds.DOOMSDAY_DOOM_GRIP),
				"the tier gate rejects before the ability runs");
		helper.assertTrue(HeroDataStore.get(player).energy() == before, "nothing charged");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pandoraDimensionOnlyAbilityOutsideHouseIsRejected(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, PandoraHero.ID);
		helper.assertFalse(MirrorDimensionController.hasActiveHouse(player), "no House is open");
		float before = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, AbilityIds.SPATIAL_BIND);
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, AbilityIds.SPATIAL_BIND),
				"the House gate rejects before the ability runs");
		helper.assertTrue(HeroDataStore.get(player).energy() == before, "nothing charged");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironFistsBlocksOtherAbilities(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		AbilityRouter.activate(player, AbilityIds.IRON_FISTS);
		helper.assertTrue(HeroDataStore.get(player).isActive(AbilityIds.IRON_FISTS),
				"Iron Fists toggles on at full energy");

		// Bound to mana so the 200-tick energy lock Iron Fists sets cannot be what blocks it.
		AbilityRouter.bind(player, AbilityIds.X_RAY, ResourceKind.MANA);
		AbilityRouter.activate(player, AbilityIds.X_RAY);
		helper.assertFalse(HeroDataStore.get(player).isActive(AbilityIds.X_RAY),
				"the stance suppresses other abilities");

		AbilityRouter.activate(player, AbilityIds.IRON_FISTS);
		helper.assertFalse(HeroDataStore.get(player).isActive(AbilityIds.IRON_FISTS),
				"pressing Iron Fists again toggles it off");

		AbilityRouter.activate(player, AbilityIds.X_RAY);
		helper.assertTrue(HeroDataStore.get(player).isActive(AbilityIds.X_RAY),
				"with the stance down the same ability starts");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void unibeamReserveBlocksOtherEnergyAbilities(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, IronManHero.ID);
		// Above the repulsor cost (200) but below cost + the 100-energy Unibeam reserve.
		HeroDataStore.update(player, d -> d.withResources(250f, d.mana()));
		AbilityRouter.activate(player, AbilityIds.REPULSOR);
		helper.assertTrue(HeroDataStore.get(player).energy() == 250f,
				"the reserve keeps a 100-energy floor for Unibeam");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
