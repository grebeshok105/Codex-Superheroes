package io.github.grebeshok105.codex.client.core.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Per-frame render inputs handed to every {@link VfxEffect#render}. Carries
 * the raw {@code WorldRenderContext} because {@code CrossBeamRenderer.draw}
 * and other {@code WorldRenderEvents} consumers need it directly.
 */
public record VfxRenderContext(WorldRenderContext world, PoseStack pose, MultiBufferSource buffers,
		Camera camera, float partialTick) {
}
