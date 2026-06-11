package com.example.superheroes.client.render;

import com.example.superheroes.client.ClientNanoSuitUpState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/**
 * Нано-одевание: во время сборки части костюма проявляются поверх игрока
 * в порядке грудь → руки → ноги → шлем (шлем — последним), при снятии — наоборот.
 */
public final class NanoSuitUpLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public NanoSuitUpLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		ClientNanoSuitUpState.Anim anim = ClientNanoSuitUpState.animFor(player.getUUID());
		if (anim == null) {
			return;
		}
		float f = ClientNanoSuitUpState.assembledFraction(anim, Util.getMillis());
		if (f <= 0f) {
			return;
		}

		PlayerModel<AbstractClientPlayer> model = getParentModel();
		boolean[] saved = saveVisibility(model);
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(anim.texture));

		poseStack.pushPose();
		poseStack.scale(1.02f, 1.02f, 1.02f);

		// грудь → руки → ноги → шлем (последним)
		renderGroup(poseStack, buffer, packedLight, model, stageAlpha(f, 0.00f, 0.30f),
				model.body, model.jacket);
		renderGroup(poseStack, buffer, packedLight, model, stageAlpha(f, 0.25f, 0.60f),
				model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve);
		renderGroup(poseStack, buffer, packedLight, model, stageAlpha(f, 0.50f, 0.85f),
				model.rightLeg, model.leftLeg, model.rightPants, model.leftPants);
		renderGroup(poseStack, buffer, packedLight, model, stageAlpha(f, 0.80f, 1.00f),
				model.head, model.hat);

		poseStack.popPose();
		restoreVisibility(model, saved);
	}

	private static float stageAlpha(float f, float start, float end) {
		if (f <= start) {
			return 0f;
		}
		return Mth.clamp((f - start) / (end - start), 0f, 1f);
	}

	private static void renderGroup(PoseStack poseStack, VertexConsumer buffer, int packedLight,
			PlayerModel<AbstractClientPlayer> model, float alpha, ModelPart... parts) {
		if (alpha <= 0.02f) {
			return;
		}
		model.setAllVisible(false);
		for (ModelPart part : parts) {
			part.visible = true;
		}
		int a = (int) (alpha * 255f) & 0xFF;
		int color = (a << 24) | 0x00FFFFFF;
		model.renderToBuffer(poseStack, buffer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
	}

	private static boolean[] saveVisibility(PlayerModel<AbstractClientPlayer> model) {
		return new boolean[] {
				model.head.visible, model.hat.visible, model.body.visible, model.jacket.visible,
				model.rightArm.visible, model.leftArm.visible, model.rightSleeve.visible, model.leftSleeve.visible,
				model.rightLeg.visible, model.leftLeg.visible, model.rightPants.visible, model.leftPants.visible
		};
	}

	private static void restoreVisibility(PlayerModel<AbstractClientPlayer> model, boolean[] saved) {
		model.head.visible = saved[0];
		model.hat.visible = saved[1];
		model.body.visible = saved[2];
		model.jacket.visible = saved[3];
		model.rightArm.visible = saved[4];
		model.leftArm.visible = saved[5];
		model.rightSleeve.visible = saved[6];
		model.leftSleeve.visible = saved[7];
		model.rightLeg.visible = saved[8];
		model.leftLeg.visible = saved[9];
		model.rightPants.visible = saved[10];
		model.leftPants.visible = saved[11];
	}
}
