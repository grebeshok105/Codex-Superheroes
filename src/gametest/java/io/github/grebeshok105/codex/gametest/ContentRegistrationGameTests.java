package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.core.content.CreativeTabContents;
import io.github.grebeshok105.codex.hero.scorpion.ScorpionItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** {@code ContentRegistrar.creativeTab} items land in the mod's creative tab tail. */
public final class ContentRegistrationGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void moduleItemsAppearInTheModTab(GameTestHelper helper) {
		helper.assertTrue(CreativeTabContents.all().contains(ScorpionItems.KUNAI),
				"scorpion_kunai reached the tab through the module's content registrar");
		helper.succeed();
	}
}
