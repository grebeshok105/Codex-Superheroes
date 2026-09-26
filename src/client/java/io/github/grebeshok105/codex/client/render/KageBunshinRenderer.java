package io.github.grebeshok105.codex.client.render;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.entity.KageBunshinEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class KageBunshinRenderer extends MobRenderer<KageBunshinEntity, PlayerModel<KageBunshinEntity>> {
	private static final ResourceLocation TEXTURE = ModId.of("textures/entity/hero/naruto.png");

	public KageBunshinRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(KageBunshinEntity entity) {
		return TEXTURE;
	}

	@Override
	protected boolean shouldShowName(KageBunshinEntity entity) {
		return false;
	}
}
