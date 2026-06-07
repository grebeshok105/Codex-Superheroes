package com.example.superheroes.client.render;

import com.example.superheroes.entity.ShieldProjectileEntity;
import com.example.superheroes.item.ModItems;
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

public class ShieldProjectileRenderer extends EntityRenderer<ShieldProjectileEntity> {
	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/item/shield.png");

	public ShieldProjectileRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(ShieldProjectileEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(ShieldProjectileEntity entity, float yaw, float partialTick,
			PoseStack pose, MultiBufferSource buffers, int packedLight) {
		pose.pushPose();
		float rot = entity.getRotation() + partialTick * 30f;
		pose.mulPose(Axis.YP.rotationDegrees(rot));
		pose.scale(1.4f, 1.4f, 1.4f);

		Minecraft mc = Minecraft.getInstance();
		ItemRenderer itemRenderer = mc.getItemRenderer();
		ItemStack stack = entity.getShieldStack().isEmpty() ? new ItemStack(ModItems.VIBRANIUM_SHIELD) : entity.getShieldStack();
		itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
				OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());

		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffers, packedLight);
	}
}
