package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.hero.scorpion.runtime.ScorpionController;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Scorpion's module contract: exactly its four abilities, and the breath channel ends on time. */
public final class ScorpionGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void scorpionOwnsExactlyItsFourAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero scorpion = Heroes.get(ModId.of("scorpion"));
		helper.assertTrue(scorpion != null, "scorpion registered");
		helper.assertTrue(scorpion.getAbilities().equals(List.of(ModId.of("scorpion_spear"), ModId.of("scorpion_hellfire"),
				ModId.of("scorpion_fire_teleport"), ModId.of("scorpion_hell_breath"))), "slot order " + scorpion.getAbilities());
		for (ResourceLocation id : scorpion.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void hellBreathEndsAfterItsDuration(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("scorpion"));
		AbilityRouter.activate(player, ModId.of("scorpion_hell_breath"));
		helper.assertTrue(ScorpionController.isBreathing(player), "breath started");
		helper.runAfterDelay(60, () -> {
			helper.assertFalse(ScorpionController.isBreathing(player), "50-tick breath is over");
			helper.succeed();
		});
	}
}
