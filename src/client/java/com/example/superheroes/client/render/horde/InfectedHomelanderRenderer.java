package com.example.superheroes.client.render.horde;

import com.example.superheroes.ModId;
import com.example.superheroes.horde.entity.InfectedHomelanderBossEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class InfectedHomelanderRenderer extends MobRenderer<InfectedHomelanderBossEntity, PlayerModel<InfectedHomelanderBossEntity>> {
	private static final ResourceLocation TEXTURE = ModId.of("textures/entity/hero/infected_homelander.png");

	public InfectedHomelanderRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(InfectedHomelanderBossEntity entity) {
		return TEXTURE;
	}

	@Override
	protected boolean shouldShowName(InfectedHomelanderBossEntity entity) {
		return false;
	}
}
