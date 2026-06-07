package com.example.superheroes.client.render;

import com.example.superheroes.ModId;
import com.example.superheroes.entity.HomelanderBossEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HomelanderBossRenderer extends MobRenderer<HomelanderBossEntity, PlayerModel<HomelanderBossEntity>> {
	private static final ResourceLocation TEXTURE = ModId.of("textures/entity/hero/infected_homelander.png");

	public HomelanderBossRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(HomelanderBossEntity entity) {
		return TEXTURE;
	}
}
