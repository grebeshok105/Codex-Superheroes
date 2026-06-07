package com.example.superheroes.client.render;

import com.example.superheroes.ModId;
import com.example.superheroes.entity.ShadowSoldierEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShadowSoldierRenderer extends MobRenderer<ShadowSoldierEntity, PlayerModel<ShadowSoldierEntity>> {
	private static final ResourceLocation[] TEXTURES = {
			ModId.of("textures/entity/shadow_soldier/variant_1.png"),
			ModId.of("textures/entity/shadow_soldier/variant_2.png"),
			ModId.of("textures/entity/shadow_soldier/variant_3.png"),
	};

	public ShadowSoldierRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(ShadowSoldierEntity entity) {
		int v = entity.getVariant();
		if (v < 0 || v >= TEXTURES.length) v = 0;
		return TEXTURES[v];
	}
}
