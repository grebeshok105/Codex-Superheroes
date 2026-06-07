package com.example.superheroes.client.render.lightning;

import com.example.superheroes.mixin.LightningBoltAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LightningBolt;
import org.joml.Matrix4f;

/**
 * Replaces the vanilla lightning bolt renderer with one of two custom
 * styles, chosen deterministically from the entity's seed:
 *
 *   FRACTAL — recursive midpoint-displacement fractal lightning
 *             (port of Fractal Lightning by Builderb0y, MIT).
 *   BEAM    — thick glowing column with side branches, evoking the
 *             look of high-end particle-engine lightning.
 */
public class SuperheroLightningRenderer extends EntityRenderer<LightningBolt> {
	public SuperheroLightningRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(LightningBolt entity, float yaw, float tickDelta,
			PoseStack poseStack, MultiBufferSource buffers, int light) {
		long seed = ((LightningBoltAccessor) (Object) entity).superheroes$getSeed();
		float age = entity.tickCount + tickDelta;
		LightningStyle style = pickStyle(seed);
		switch (style) {
			case FRACTAL -> renderFractal(seed, age, poseStack, buffers);
			case BEAM -> renderBeam(seed, age, poseStack, buffers);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(LightningBolt entity) {
		return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
	}

	private static LightningStyle pickStyle(long seed) {
		long h = LightningRng.stafford(seed ^ 0x5752_4E47L);
		return (h & 1L) == 0L ? LightningStyle.FRACTAL : LightningStyle.BEAM;
	}

	private void renderFractal(long seed, float age, PoseStack poseStack, MultiBufferSource buffers) {
		VertexConsumer buf = buffers.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
		Matrix4f matrix = new Matrix4f(poseStack.last().pose());
		new FractalLightningRenderer(matrix, buf, age).render(seed);
	}

	private void renderBeam(long seed, float age, PoseStack poseStack, MultiBufferSource buffers) {
		new BeamLightningRenderer(poseStack, buffers, seed, age).render();
	}

	private enum LightningStyle {
		FRACTAL,
		BEAM
	}
}
