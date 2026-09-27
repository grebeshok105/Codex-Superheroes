package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public final class AutoSaturationController {
	private static final int TARGET_FOOD = 20;
	private static final float TARGET_SATURATION = 20f;

	private AutoSaturationController() {
	}


	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		if (server.getTickCount() % 20 != 0) return;
		if (player.isSpectator() || player.isCreative()) return;
		if (!data.hasHero()) return;

		int food = player.getFoodData().getFoodLevel();
		if (food < TARGET_FOOD) {
			player.getFoodData().setFoodLevel(Math.min(TARGET_FOOD, food + 1));
		} else if (food > TARGET_FOOD) {
			player.getFoodData().setFoodLevel(TARGET_FOOD);
		}
		player.getFoodData().setSaturation(TARGET_SATURATION);
	}

}
