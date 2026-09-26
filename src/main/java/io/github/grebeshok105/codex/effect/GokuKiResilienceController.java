package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.GokuHero;
import io.github.grebeshok105.codex.transform.HeroData;
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
