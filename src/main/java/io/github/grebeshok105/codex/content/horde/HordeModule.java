package io.github.grebeshok105.codex.content.horde;

import io.github.grebeshok105.codex.content.horde.entity.HordeDeaths;
import io.github.grebeshok105.codex.content.horde.entity.HordeEntities;
import io.github.grebeshok105.codex.content.horde.net.HordeDebugS2CPayload;
import io.github.grebeshok105.codex.core.module.ContentModule;
import io.github.grebeshok105.codex.core.module.ContentModuleContext;

/** The parasite horde: its entity/attribute registration, debug payload and the per-level tick. */
public final class HordeModule implements ContentModule {
	@Override
	public void register(ContentModuleContext ctx) {
		HordeEntities.init();
		HordeDeaths.onMobDied(HordeManager::onMobDied);
		HordeItems.register(ctx.content());
		ctx.payloads().s2c(HordeDebugS2CPayload.TYPE, HordeDebugS2CPayload.STREAM_CODEC);
		ctx.ticks().level((server, level) -> HordeManager.tick(level));
	}
}
