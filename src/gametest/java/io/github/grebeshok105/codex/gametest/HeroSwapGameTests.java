package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.ability.ThanosSnapAbility;
import io.github.grebeshok105.codex.hero.RaidenHero;
import io.github.grebeshok105.codex.hero.ScaramoucheHero;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.item.infinity.InfinityGauntletData;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Audit B5 (hero swap must not reset cooldowns, heal, or refill energy) and
 * B6 (the Snap burns the stones inside the gauntlet, not just loose ones).
 */
public final class HeroSwapGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroSwapKeepsAbilityCooldowns(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		HeroTransformService.transform(player, RaidenHero.ID);
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.RAIDEN_SWORD_DRAW, 200);

		// the transform cooldown is 20 ticks — swap as soon as it legally allows
		helper.runAfterDelay(25, () -> {
			TestHeroes.transform(player, ScaramoucheHero.ID);
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.RAIDEN_SWORD_DRAW),
					"a hero swap must not reset ability cooldowns");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroSwapDoesNotHealOrRefillEnergy(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		HeroTransformService.transform(player, RaidenHero.ID);
		player.setHealth(player.getMaxHealth() * 0.4f);
		HeroDataStore.update(player, d -> d.withResources(50f, d.mana()));

		helper.runAfterDelay(25, () -> {
			float energyBefore = HeroDataStore.get(player).energy();
			TestHeroes.transform(player, ScaramoucheHero.ID);
			helper.assertTrue(player.getHealth() < player.getMaxHealth(),
					"transforming must not be a free heal");
			float energyAfter = HeroDataStore.get(player).energy();
			helper.assertTrue(energyAfter <= energyBefore + 0.001f && energyAfter > 0f,
					"energy carries over like mana instead of refilling");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapConsumesGauntletAndLooseStones(GameTestHelper helper) {
		ServerPlayer thanos = TestPlayers.join(helper);
		ItemStack gauntlet = new ItemStack(ModItems.INFINITY_GAUNTLET);
		for (InfinityStoneType type : InfinityStoneType.values()) {
			InfinityGauntletData.tryInsert(gauntlet, type);
		}
		thanos.getInventory().add(gauntlet);
		thanos.getInventory().add(new ItemStack(ModItems.POWER_STONE));

		ThanosSnapAbility.executeSnap(thanos);

		helper.assertTrue(InfinityGauntletData.getStones(gauntlet).isEmpty(),
				"the snap burns the stones out of the gauntlet");
		helper.assertTrue(TestPlayers.count(thanos, ModItems.POWER_STONE) == 0,
				"loose stones are consumed too");
		TestPlayers.leave(thanos);
		helper.succeed();
	}
}
