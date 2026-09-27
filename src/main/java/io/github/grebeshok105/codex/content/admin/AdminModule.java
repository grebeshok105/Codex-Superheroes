package io.github.grebeshok105.codex.content.admin;

import io.github.grebeshok105.codex.core.module.ContentModule;
import io.github.grebeshok105.codex.core.module.ContentModuleContext;

/**
 * Admin tooling: the admin-build flag attachment, its S2C sync (join + toggle),
 * the creative-tab visibility flag and the ability-debug facade over
 * {@code core.ability.MobTargetDebug}. No ticks, no content — wiring only.
 */
public final class AdminModule implements ContentModule {
	@Override
	public void register(ContentModuleContext ctx) {
		AdminAttachments.init();
		ctx.payloads().s2c(AdminBuildS2CPayload.TYPE, AdminBuildS2CPayload.STREAM_CODEC);
		AdminBuildSyncController.register(ctx);
	}
}
