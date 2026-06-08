package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

public final class AutoSaturationController {
	private static final int TARGET_FOOD = 20;
	private static final float TARGET_SATURATION = 20f;

	private AutoSaturationController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isSpectator() || player.isCreative()) continue;
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				if (!data.hasHero()) continue;

				int food = player.getFoodData().getFoodLevel();
				if (food < TARGET_FOOD) {
					player.getFoodData().setFoodLevel(Math.min(TARGET_FOOD, food + 1));
				} else if (food > TARGET_FOOD) {
					player.getFoodData().setFoodLevel(TARGET_FOOD);
				}
				player.getFoodData().setSaturation(TARGET_SATURATION);
			}
		});
	}
}
