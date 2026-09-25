package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.MinecraftServer;

public final class HeroPassiveRegenController {
	private static final int REAPPLY_INTERVAL = 40;
	private static final int EFFECT_DURATION = 100;

	private HeroPassiveRegenController() {
	}


	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		if (server.getTickCount() % REAPPLY_INTERVAL != 0) {
			return;
		}
		if (!data.hasHero()) {
			return;
		}
		MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
		if (current != null && current.getAmplifier() > 0) {
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 0, true, false, true));
	}

}
