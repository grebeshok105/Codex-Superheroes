package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.CaptainAmericaHero;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AutoSaturationController {
	private static final int DEFAULT_FOOD = 17;
	private static final float DEFAULT_SATURATION = 0f;

	private static final int CAP_FOOD = 20;
	private static final float CAP_SATURATION = 20f;

	private AutoSaturationController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isSpectator() || player.isCreative()) continue;
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				if (!data.hasHero()) continue;

				ResourceLocation heroId = data.heroId();
				int targetFood = DEFAULT_FOOD;
				float targetSaturation = DEFAULT_SATURATION;
				if (heroId != null && (CaptainAmericaHero.ID.equals(heroId)
						|| KratosHero.ID.equals(heroId)
						|| ThanosHero.ID.equals(heroId))) {
					targetFood = CAP_FOOD;
					targetSaturation = CAP_SATURATION;
				}

				int food = player.getFoodData().getFoodLevel();
				if (food < targetFood) {
					player.getFoodData().setFoodLevel(Math.min(targetFood, food + 1));
				} else if (food > targetFood) {
					player.getFoodData().setFoodLevel(targetFood);
				}
				player.getFoodData().setSaturation(targetSaturation);
			}
		});
	}
}
