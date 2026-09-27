package io.github.grebeshok105.codex.client.content.admin;

import io.github.grebeshok105.codex.client.core.module.ContentClientContext;
import io.github.grebeshok105.codex.client.core.module.ContentClientModule;
import io.github.grebeshok105.codex.content.admin.AdminBuildS2CPayload;
import io.github.grebeshok105.codex.content.admin.AdminBuildVisibility;
import io.github.grebeshok105.codex.item.ModItemGroups;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;

/** Админ-билд: клиентский приёмник флага видимости админ-предметов в креатив-вкладке. */
public final class AdminClientModule implements ContentClientModule {
	@Override
	public void register(ContentClientContext ctx) {
		ctx.receive(AdminBuildS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					AdminBuildVisibility.setClientVisible(payload.enabled());
					rebuildSuperheroesTab(context.client());
				}));
	}

	/**
	 * Пересобирает содержимое креатив-вкладки Superheroes после смены
	 * состояния админ-билда (vanilla кэширует вкладки и сам не обновит).
	 */
	private static void rebuildSuperheroesTab(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		var parameters = new CreativeModeTab.ItemDisplayParameters(
				mc.player.connection.enabledFeatures(),
				mc.player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get(),
				mc.level.registryAccess());
		ModItemGroups.SUPERHEROES_TAB.buildContents(parameters);
	}
}
