package com.example.superheroes.client.render.horde;

import com.example.superheroes.ModId;
import com.example.superheroes.horde.entity.BaseHordeEntity;
import com.example.superheroes.horde.entity.HordeGeoAssets;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Универсальная GeckoLib-модель тварей орды: гео/текстура/анимации выбираются
 * по короткому имени твари (id без префикса {@code horde_}).
 */
public class HordeGeoModel extends GeoModel<BaseHordeEntity> {
	@Override
	public ResourceLocation getModelResource(BaseHordeEntity entity) {
		return ModId.of("geo/horde/" + HordeGeoAssets.geoName(entity.getType()) + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BaseHordeEntity entity) {
		return ModId.of("textures/entity/horde/" + HordeGeoAssets.geoName(entity.getType()) + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(BaseHordeEntity entity) {
		return ModId.of("animations/horde/" + HordeGeoAssets.geoName(entity.getType()) + ".animation.json");
	}
}
