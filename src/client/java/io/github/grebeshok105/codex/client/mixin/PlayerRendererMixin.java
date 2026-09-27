package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.ClientNanoSuitUpState;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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

	@Unique
	private static ResourceLocation superheroes$heroHandTexture(AbstractClientPlayer player) {
		if (player != Minecraft.getInstance().player) {
			return null;
		}
		// Во время нано-сборки рука от первого лица остаётся «голой» — броня ещё материализуется.
		if (ClientNanoSuitUpState.suppressHeroSkin(player.getUUID())) {
			return null;
		}
		SkinResolver.ResolvedSkin skin = SkinResolver.resolve(player);
		return skin == null ? null : skin.handTexture();
	}
}
