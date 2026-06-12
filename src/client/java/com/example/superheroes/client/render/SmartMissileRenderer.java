package com.example.superheroes.client.render;

import com.example.superheroes.entity.SmartMissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Рендер нано-ракеты: маленькое огненное тело (fire charge), ориентированное по
 * вектору полёта; основной визуал — дымно-огненный след из частиц самой
 * сущности. Полный свет, чтобы ракета светилась в темноте.
 */
public class SmartMissileRenderer extends EntityRenderer<SmartMissileEntity> {
	public SmartMissileRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0f;
	}

	@Override
	public ResourceLocation getTextureLocation(SmartMissileEntity entity) {
		return ResourceLocation.withDefaultNamespace("textures/item/fire_charge.png");
	}

	@Override
	public void render(SmartMissileEntity entity, float yaw, float partialTick,
			PoseStack pose, MultiBufferSource buffers, int packedLight) {
		pose.pushPose();
		float entYaw = entity.getYRot();
		float entPitch = entity.getXRot();
		pose.mulPose(Axis.YP.rotationDegrees(-entYaw));
		pose.mulPose(Axis.XP.rotationDegrees(entPitch));
		pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 40f));
		pose.scale(0.55f, 0.55f, 0.55f);

		Minecraft mc = Minecraft.getInstance();
		ItemRenderer itemRenderer = mc.getItemRenderer();
		ItemStack stack = new ItemStack(Items.FIRE_CHARGE);
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY,
				pose, buffers, entity.level(), 0);
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffers, packedLight);
	}
}
