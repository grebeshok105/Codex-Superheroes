package com.example.superheroes.client.render;

import com.example.superheroes.ModId;
import com.example.superheroes.entity.RamEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RamRenderer extends MobRenderer<RamEntity, PlayerModel<RamEntity>> {
	private static final ResourceLocation TEXTURE = ModId.of("textures/entity/ram.png");

	public RamRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
		// renders the enchanted "wand" stick in ranged mode
		this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(RamEntity entity) {
		return TEXTURE;
	}
}
