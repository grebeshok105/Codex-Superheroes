package com.example.superheroes.client.render;

import com.example.superheroes.ability.ironman.IronManSuitVariant;
import com.example.superheroes.entity.IronLegionDroneEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class IronLegionDroneRenderer extends MobRenderer<IronLegionDroneEntity, PlayerModel<IronLegionDroneEntity>> {

	public IronLegionDroneRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(IronLegionDroneEntity entity) {
		IronManSuitVariant variant = IronManSuitVariant.get(entity.getSuitVariant());
		return variant.texture();
	}

	@Override
	protected boolean shouldShowName(IronLegionDroneEntity entity) {
		return false;
	}
}
