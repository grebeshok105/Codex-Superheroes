package com.example.superheroes.client.render.horde;

import com.example.superheroes.ModId;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;

/**
 * Generic renderer for horde entities using zombie model with custom texture.
 * The GeckoLib models/animations are imported for future upgrade, but for now
 * we use simple vanilla models to stay consistent with the codebase.
 */
public class GenericHordeRenderer<T extends Monster> extends MobRenderer<T, ZombieModel<T>> {
	private final ResourceLocation texture;

	public GenericHordeRenderer(EntityRendererProvider.Context ctx, String textureName, float shadow) {
		super(ctx, new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), shadow);
		this.texture = ModId.of("textures/entity/horde/" + textureName + ".png");
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture;
	}

	@Override
	protected boolean shouldShowName(T entity) {
		return false;
	}

	public static <T extends Monster> EntityRendererProvider<T> factory(String tex, float shadow) {
		return ctx -> new GenericHordeRenderer<>(ctx, tex, shadow);
	}
}
