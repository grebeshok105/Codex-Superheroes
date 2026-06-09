package com.example.superheroes.client.render;

import com.example.superheroes.client.ClientRemDemonismState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;

public final class RemOniHornFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public RemOniHornFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		if (!ClientRemDemonismState.isActive(player.getUUID())) {
			return;
		}
		poseStack.pushPose();
		getParentModel().head.translateAndRotate(poseStack);
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();
		float pulse = 0.86f + 0.14f * (float) Math.sin((ageInTicks + partialTick) * 0.38f);
		drawHorn(buffer, matrix, 0.10f, 0.62f, 0.92f * pulse);
		drawHorn(buffer, matrix, 0.055f, 0.92f, 1.0f);
		poseStack.popPose();
	}

	private static void drawHorn(VertexConsumer buffer, Matrix4f matrix, float width, float alpha, float glow) {
		float half = width * 0.5f;
		float baseY = -0.33f;
		float baseZ = -0.255f;
		float tipY = -0.47f;
		float tipZ = -0.52f;
		vertex(buffer, matrix, -half, baseY - half, baseZ, glow, 0.22f, 0.72f, alpha);
		vertex(buffer, matrix, half, baseY - half, baseZ, glow, 0.35f, 0.86f, alpha);
		vertex(buffer, matrix, 0.0f, tipY, tipZ, 1.0f, 0.85f, 1.0f, alpha);
		vertex(buffer, matrix, half, baseY - half, baseZ, glow, 0.35f, 0.86f, alpha);
		vertex(buffer, matrix, half, baseY + half, baseZ, glow, 0.22f, 0.72f, alpha);
		vertex(buffer, matrix, 0.0f, tipY, tipZ, 1.0f, 0.85f, 1.0f, alpha);
		vertex(buffer, matrix, half, baseY + half, baseZ, glow, 0.22f, 0.72f, alpha);
		vertex(buffer, matrix, -half, baseY + half, baseZ, glow, 0.30f, 0.82f, alpha);
		vertex(buffer, matrix, 0.0f, tipY, tipZ, 1.0f, 0.85f, 1.0f, alpha);
		vertex(buffer, matrix, -half, baseY + half, baseZ, glow, 0.30f, 0.82f, alpha);
		vertex(buffer, matrix, -half, baseY - half, baseZ, glow, 0.22f, 0.72f, alpha);
		vertex(buffer, matrix, 0.0f, tipY, tipZ, 1.0f, 0.85f, 1.0f, alpha);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z,
			float red, float green, float blue, float alpha) {
		buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
	}
}
