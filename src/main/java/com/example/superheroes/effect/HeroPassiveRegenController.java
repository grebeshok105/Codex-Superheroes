package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class HeroPassiveRegenController {
	private static final int REAPPLY_INTERVAL = 40;
	private static final int EFFECT_DURATION = 100;

	private HeroPassiveRegenController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % REAPPLY_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				if (!data.hasHero()) {
					continue;
				}
				MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
				if (current != null && current.getAmplifier() > 0) {
					continue;
				}
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 0, true, false, true));
			}
		});
	}
}
