package io.github.grebeshok105.codex.hero.goku.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.MinecraftServer;

public final class GokuKiResilienceController {
	private static final ResourceLocation HERO_ID = ModId.of("goku");
	private GokuKiResilienceController() {
	}


	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		if (!data.hasHero() || !HERO_ID.equals(data.heroId())) {
			return;
		}
		if (GokuKiStackController.getStacks(player) >= 3 && player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
		}
	}

}
