package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.core.emf.EmfHeldItemSuppression;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the third-person held item while a registered EMF presentation
 * supplies its own authored prop (Homelander's milk bottle while the milk
 * weight is up). First-person hands render through {@code ItemInHandRenderer}
 * so they are unaffected; entities no predicate claims render vanilla items.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

	@Inject(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void superheroes$hideHeldItemDuringEmfProp(PoseStack poseStack, MultiBufferSource buffer,
			int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount,
			float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (EmfHeldItemSuppression.hides(entity)) {
			ci.cancel();
		}
	}
}
