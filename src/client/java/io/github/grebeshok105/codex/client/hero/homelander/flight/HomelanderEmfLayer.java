package io.github.grebeshok105.codex.client.hero.homelander.flight;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderEmfEngine;
import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderEmfRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/**
 * The {@link PlayerRenderer} feature layer that draws the EMF Homelander
 * model in place of the vanilla player: every rendered frame it advances the
 * entity's {@link HomelanderEmfRuntime} (weights, clip times, lean) and lets
 * the compiled {@link HomelanderEmfEngine} evaluate the generated expressions
 * and draw the part tree. Skipped entirely while {@code !presenting()} — the
 * vanilla model stays authoritative then.
 */
public final class HomelanderEmfLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public HomelanderEmfLayer(PlayerRenderer renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
			float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(player.getId());
		HomelanderEmfEngine engine = HomelanderEmfRuntime.engine();
		if (runtime == null || engine == null) {
			return;
		}
		float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		runtime.advanceAndPush(partial);
		if (!runtime.presenting()) {
			return;
		}
		// The stack is already in vanilla model space (y-down, feet plane at
		// y=24px — the same space the converted parts are built in), so the
		// root part renders directly.
		engine.render(poseStack, bufferSource, packedLight, player, partialTicks);
	}
}
