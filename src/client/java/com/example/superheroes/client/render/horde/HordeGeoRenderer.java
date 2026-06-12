package com.example.superheroes.client.render.horde;

import com.example.superheroes.horde.entity.BaseHordeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib-рендерер для всех тварей орды, у которых есть гео-модель.
 * Заменяет ванильный {@link GenericHordeRenderer}.
 */
public class HordeGeoRenderer extends GeoEntityRenderer<BaseHordeEntity> {
	public HordeGeoRenderer(EntityRendererProvider.Context context) {
		super(context, new HordeGeoModel());
		this.shadowRadius = 0.5f;
	}
}
