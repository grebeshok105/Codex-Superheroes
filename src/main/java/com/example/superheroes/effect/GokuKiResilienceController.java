package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.GokuHero;
import com.example.superheroes.transform.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.MinecraftServer;

public final class GokuKiResilienceController {
	private GokuKiResilienceController() {
	}


	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		if (!data.hasHero() || !GokuHero.ID.equals(data.heroId())) {
			return;
		}
		if (GokuKiStackController.getStacks(player) >= 3 && player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
		}
	}

}
