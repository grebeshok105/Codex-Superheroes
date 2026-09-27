package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Pins for the I1a Kazuha module move: written against the pre-move layout and must
 * pass identically after {@code hero/kazuha/} absorbs the server side. Ability ids are
 * literals because the constants move from {@code AbilityIds} to the module.
 */
public final class KazuhaGameTests implements FabricGameTest {
	private static final ResourceLocation AUTUMN_WHIRLWIND = ModId.of("kazuha_autumn_whirlwind");

	@GameTest(template = EMPTY_STRUCTURE)
	public void kazuhaOwnsExactlyItsFourAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero kazuha = Heroes.get(ModId.of("kazuha"));
		helper.assertTrue(kazuha != null, "kazuha registered");
		helper.assertTrue(kazuha.getAbilities().equals(List.of(ModId.of("kazuha_chihayaburu"),
				ModId.of("kazuha_midare_ranzan"), AUTUMN_WHIRLWIND, ModId.of("kazuha_maple_storm"))),
				"slot order " + kazuha.getAbilities());
		for (ResourceLocation id : kazuha.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void autumnWhirlwindAppliesAndClearsWindEffects(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("kazuha"));

		AbilityRouter.activate(player, AUTUMN_WHIRLWIND);
		helper.assertTrue(HeroDataStore.get(player).isActive(AUTUMN_WHIRLWIND), "toggle went active");
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "wind speed applied");
		helper.assertTrue(player.hasEffect(MobEffects.JUMP), "wind jump applied");
		helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "wind slow falling applied");

		AbilityRouter.deactivate(player, AUTUMN_WHIRLWIND);
		helper.assertFalse(HeroDataStore.get(player).isActive(AUTUMN_WHIRLWIND), "toggle went inactive");
		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "wind speed removed");
		helper.assertFalse(player.hasEffect(MobEffects.JUMP), "wind jump removed");
		helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), "wind slow falling removed");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AUTUMN_WHIRLWIND),
				"deactivation starts the exit cooldown");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
