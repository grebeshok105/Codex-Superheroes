package io.github.grebeshok105.codex.client.hero.sungjinwoo;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.render.ShadowSoldierRenderer;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.hero.SungJinwooHero;
import io.github.grebeshok105.codex.network.SungShadowArmyS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record SungJinwooClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return SungJinwooHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new SungJinwooSkinProvider());
		ctx.entityRenderer(ModEntities.SHADOW_SOLDIER, ShadowSoldierRenderer::new);
		ctx.receive(SungShadowArmyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientShadowArmyState.update(
						payload.playerId(), payload.hasShadows(), payload.count(), payload.phase2())));
	}
}
