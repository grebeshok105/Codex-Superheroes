package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.GokuHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class GokuKiResilienceController {
	private GokuKiResilienceController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				if (!data.hasHero() || !GokuHero.ID.equals(data.heroId())) {
					continue;
				}
				if (GokuKiStackController.getStacks(player) >= 3 && player.tickCount % 40 == 0) {
					player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
				}
			}
		});
	}
}
