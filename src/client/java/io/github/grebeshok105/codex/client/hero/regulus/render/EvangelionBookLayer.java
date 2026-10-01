package io.github.grebeshok105.codex.client.hero.regulus.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The Evangelion book floating open in front of the reader while the ritual
 * channel runs. It reads the SYNCED {@code ritualUntilTick} deadline off the
 * rendered player's attachment, so tracking clients see the prop on every
 * reader at the same game-tick progress — no wall-clock anywhere.
 */
public final class EvangelionBookLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public EvangelionBookLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		long now = player.level().getGameTime();
		RegulusMadnessState state = player.getAttached(RegulusMadnessState.ATTACHMENT);
		if (state == null || !state.isReading(now)) {
			return;
		}
		// 0.0 just opened → 1.0 about to complete — drives the slow open-tilt.
		float progress = 1f - (state.ritualUntilTick() - now - partialTick)
				/ (float) RegulusMadnessController.RITUAL_TICKS;

		poseStack.pushPose();
		getParentModel().body.translateAndRotate(poseStack);
		// Hovering just under face height, out in front of the chest — the arms
		// are already aimed forward by the BOW use-pose.
		poseStack.translate(0f, -0.30f, -0.55f);
		// gentle bob + a slow left→right yaw wander
		poseStack.translate(0f, Math.sin(ageInTicks * 0.25f) * 0.015f, 0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(65f + progress * 10f));
		poseStack.scale(0.9f, 0.9f, 0.9f);

		ItemStack book = new ItemStack(evangelion());
		Minecraft.getInstance().getItemRenderer().renderStatic(book, ItemDisplayContext.FIXED,
				packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, player.level(), player.getId());
		poseStack.popPose();
	}

	/** The item object lives inside {@code hero.regulus}; client.render resolves it by id. */
	private static Item evangelion() {
		return BuiltInRegistries.ITEM.get(ModId.of("evangelion"));
	}
}
