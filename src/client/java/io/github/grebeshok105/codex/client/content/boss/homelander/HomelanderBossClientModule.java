package io.github.grebeshok105.codex.client.content.boss.homelander;

import io.github.grebeshok105.codex.client.content.boss.homelander.render.HomelanderBossRenderer;
import io.github.grebeshok105.codex.client.core.module.ContentClientContext;
import io.github.grebeshok105.codex.client.core.module.ContentClientModule;
import io.github.grebeshok105.codex.content.boss.homelander.entity.HomelanderBossEntities;

/** The Homelander boss fight: its entity renderer. */
public final class HomelanderBossClientModule implements ContentClientModule {
	@Override
	public void register(ContentClientContext ctx) {
		ctx.entityRenderer(HomelanderBossEntities.HOMELANDER_BOSS, HomelanderBossRenderer::new);
	}
}
