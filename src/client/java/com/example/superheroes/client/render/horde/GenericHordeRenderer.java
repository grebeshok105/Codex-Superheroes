package com.example.superheroes.client.render.horde;

import com.example.superheroes.ModId;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;

/**
 * Универсальный рендерер тварей орды: каждая разновидность получает подходящую
 * ванильную модель (гуманоид, паук, четвероногое, крипер и т.д.) и визуальный
 * масштаб, подогнанный под её хитбокс — никаких «зомби в чужой шкуре» и
 * моделей, торчащих из хитбокса.
 */
public class GenericHordeRenderer<T extends Monster> extends MobRenderer<T, EntityModel<T>> {
	private final ResourceLocation texture;
	private final float visualScale;

	public GenericHordeRenderer(EntityRendererProvider.Context ctx, EntityModel<T> model,
			String textureName, float shadow, float visualScale) {
		super(ctx, model, shadow);
		this.texture = ModId.of("textures/entity/horde/" + textureName + ".png");
		this.visualScale = visualScale;
	}

	@Override
	protected void scale(T entity, PoseStack poseStack, float partialTick) {
		if (visualScale != 1.0f) {
			poseStack.scale(visualScale, visualScale, visualScale);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture;
	}

	@Override
	protected boolean shouldShowName(T entity) {
		return false;
	}

	// ===== фабрики по типу модели =====

	public static <T extends Monster> EntityRendererProvider<T> humanoid(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), tex, shadow, scale);
	}

	public static <T extends Monster> EntityRendererProvider<T> spider(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new SpiderModel<>(ctx.bakeLayer(ModelLayers.SPIDER)), tex, shadow, scale);
	}

	public static <T extends Monster> EntityRendererProvider<T> creeper(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new CreeperModel<>(ctx.bakeLayer(ModelLayers.CREEPER)), tex, shadow, scale);
	}

	public static <T extends Monster> EntityRendererProvider<T> cow(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new CowModel<>(ctx.bakeLayer(ModelLayers.COW)), tex, shadow, scale);
	}

	public static <T extends Monster> EntityRendererProvider<T> villager(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), tex, shadow, scale);
	}

	public static <T extends Monster> EntityRendererProvider<T> ghast(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new GhastModel<>(ctx.bakeLayer(ModelLayers.GHAST)), tex, shadow, scale);
	}

	public static <T extends Monster> EntityRendererProvider<T> silverfish(String tex, float shadow, float scale) {
		return ctx -> new GenericHordeRenderer<>(ctx,
				new SilverfishModel<>(ctx.bakeLayer(ModelLayers.SILVERFISH)), tex, shadow, scale);
	}
}
