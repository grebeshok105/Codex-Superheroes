package io.github.grebeshok105.codex.client.content.horde;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.content.horde.render.GenericHordeRenderer;
import io.github.grebeshok105.codex.client.content.horde.render.InfectedHomelanderRenderer;
import io.github.grebeshok105.codex.client.core.module.ContentClientContext;
import io.github.grebeshok105.codex.client.core.module.ContentClientModule;
import io.github.grebeshok105.codex.content.horde.entity.HordeEntities;
import io.github.grebeshok105.codex.content.horde.net.HordeDebugS2CPayload;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** The parasite horde: entity renderers, the debug HUD layer and its payload receiver. */
public final class HordeClientModule implements ContentClientModule {
	@Override
	public void register(ContentClientContext ctx) {
		// Horde entity renderers — vanilla models matched to each mob's texture UV
		// (custom geo/textures are mismatched imports → garbled UVs, deferred to a proper import PR).
		ctx.entityRenderer(HordeEntities.CRAWLER, GenericHordeRenderer.spider("crawler", 0.4f, 0.55f));
		ctx.entityRenderer(HordeEntities.LURKER, GenericHordeRenderer.humanoid("lurker", 0.4f, 0.72f));
		ctx.entityRenderer(HordeEntities.SPITTER, GenericHordeRenderer.humanoid("spitter", 0.35f, 0.62f));
		ctx.entityRenderer(HordeEntities.SWOOPER, GenericHordeRenderer.ghast("swooper", 0.4f, 0.8f));
		ctx.entityRenderer(HordeEntities.STALKER, GenericHordeRenderer.humanoid("stalker", 0.4f, 0.82f));
		ctx.entityRenderer(HordeEntities.INFECTOR, GenericHordeRenderer.humanoid("infector", 0.3f, 0.51f));
		ctx.entityRenderer(HordeEntities.PARASITIC_HOUND, GenericHordeRenderer.cow("parasitic_hound", 0.35f, 0.5f));
		ctx.entityRenderer(HordeEntities.INFECTED_ZOMBIE, GenericHordeRenderer.humanoid("infected_zombie", 0.5f, 1.0f));
		ctx.entityRenderer(HordeEntities.INFECTED_SKELETON, GenericHordeRenderer.humanoid("infected_skeleton", 0.5f, 1.0f));
		ctx.entityRenderer(HordeEntities.INFECTED_SPIDER, GenericHordeRenderer.spider("infected_spider", 0.7f, 1.0f));
		ctx.entityRenderer(HordeEntities.INFECTED_CREEPER, GenericHordeRenderer.creeper("infected_creeper", 0.5f, 1.0f));
		ctx.entityRenderer(HordeEntities.VOID_PARASITE, GenericHordeRenderer.humanoid("void_parasite", 0.35f, 0.67f));
		ctx.entityRenderer(HordeEntities.HOLLOW_VILLAGER, GenericHordeRenderer.villager("hollow_villager", 0.5f, 1.0f));
		ctx.entityRenderer(HordeEntities.INFECTED_CATTLE, GenericHordeRenderer.cow("infected_cattle", 0.5f, 1.0f));
		ctx.entityRenderer(HordeEntities.BROODMOTHER, GenericHordeRenderer.spider("broodmother", 0.9f, 1.25f));
		ctx.entityRenderer(HordeEntities.CORRUPTED_GOLEM, GenericHordeRenderer.humanoid("corrupted_golem", 0.8f, 1.38f));
		ctx.entityRenderer(HordeEntities.HIVEMIND, GenericHordeRenderer.humanoid("hivemind", 0.6f, 1.03f));
		ctx.entityRenderer(HordeEntities.LEVIATHAN, GenericHordeRenderer.humanoid("leviathan", 1.0f, 1.54f));
		ctx.entityRenderer(HordeEntities.INFECTED_HOMELANDER, InfectedHomelanderRenderer::new);
		// Horde bomb projectiles render as thrown items.
		ctx.entityRenderer(HordeEntities.ACID_BOMB,
				c -> new ThrownItemRenderer<>(c, 1.0f, false));
		ctx.entityRenderer(HordeEntities.FIRE_BOMB,
				c -> new ThrownItemRenderer<>(c, 1.0f, false));

		ctx.hud(2000, ModId.of("horde_debug"), HordeDebugOverlay::render);
		ctx.receive(HordeDebugS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> HordeDebugOverlay.update(payload.text())));
	}
}
