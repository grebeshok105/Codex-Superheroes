package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import io.github.grebeshok105.codex.client.core.flight.FlightPoseTracker;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.client.core.render.SkinSuppressions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	@ModifyVariable(
			method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
			at = @At("STORE"),
			ordinal = 0
	)
	private ResourceLocation superheroes$useHeroHandTexture(ResourceLocation original, PoseStack poseStack, MultiBufferSource multiBufferSource, int light,
			AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
		ResourceLocation texture = superheroes$heroHandTexture(player);
		return texture == null ? original : texture;
	}

	/**
	 * Whole-body flight tilt: applies the tracker's pitch (about the lateral X
	 * axis) and roll (about the forward Z axis) around the entity origin —
	 * the same feet pivot {@code HumanoidAnchors} tilts its eye/body anchors
	 * around, so rendered limbs and anchor math agree.
	 */
	@Inject(
			method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
			at = @At("TAIL")
	)
	private void superheroes$flightBodyTilt(AbstractClientPlayer entity, PoseStack poseStack, float bob,
			float bodyRot, float partialTick, float scale, CallbackInfo ci) {
		FlightBodyTransform tilt = FlightPoseTracker.transform(entity.getId(), partialTick);
		if (tilt.equals(FlightBodyTransform.IDENTITY)) {
			return;
		}
		poseStack.mulPose(Axis.XP.rotationDegrees(-tilt.pitchDeg()));
		poseStack.mulPose(Axis.ZP.rotationDegrees(-tilt.rollDeg()));
	}

	@Unique
	private static ResourceLocation superheroes$heroHandTexture(AbstractClientPlayer player) {
		if (player != Minecraft.getInstance().player) {
			return null;
		}
		// Во время нано-сборки рука от первого лица остаётся «голой» — броня ещё материализуется.
		if (SkinSuppressions.suppresses(player.getUUID())) {
			return null;
		}
		SkinResolver.ResolvedSkin skin = SkinResolver.resolve(player);
		return skin == null ? null : skin.handTexture();
	}
}
