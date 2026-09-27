package io.github.grebeshok105.codex.client.hero.sungjinwoo;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.sungjinwoo.render.ShadowSoldierRenderer;
import io.github.grebeshok105.codex.client.hero.sungjinwoo.state.ClientShadowArmyState;
import io.github.grebeshok105.codex.hero.sungjinwoo.SungJinwooHero;
import io.github.grebeshok105.codex.hero.sungjinwoo.entity.SungJinwooEntities;
import io.github.grebeshok105.codex.hero.sungjinwoo.net.SungShadowArmyS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record SungJinwooClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return SungJinwooHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new SungJinwooSkinProvider());
		ctx.entityRenderer(SungJinwooEntities.SHADOW_SOLDIER, ShadowSoldierRenderer::new);
		ctx.receive(SungShadowArmyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientShadowArmyState.update(
						payload.playerId(), payload.hasShadows(), payload.count(), payload.phase2())));
	}
}
