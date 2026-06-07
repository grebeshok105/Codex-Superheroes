package com.example.superheroes.client.render.lightning;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Fractal lightning bolt renderer using recursive midpoint displacement.
 *
 * Port of the rendering algorithm from Fractal Lightning by Builderb0y (MIT).
 * https://github.com/Builderb0y/FractalLightning
 *
 * Vertices are emitted in view space (already pre-transformed by the
 * model-view matrix) so the consumer should be obtained from
 * {@link RenderType#lightning()} using the standard pose pipeline.
 */
public final class FractalLightningRenderer {
	public static final float BASE_WIDTH = 0.015625F;
	public static final float EIGHTH_WIDTH = 0.001953125F;

	private static final int INNER_ARGB = Integer.MAX_VALUE;
	private static final int OUTER_ARGB = 16255;

	private final Matrix4f modelViewMatrix;
	private final VertexConsumer buffer;
	private final Vector4f scratch = new Vector4f();
	private final float age;

	public FractalLightningRenderer(Matrix4f modelViewMatrix, VertexConsumer buffer, float age) {
		this.modelViewMatrix = modelViewMatrix;
		this.buffer = buffer;
		this.age = age;
	}

	public void render(long seed) {
		long s1 = seed + LightningRng.PHI64;
		long s2 = s1 + LightningRng.PHI64;
		long s3 = s2 + LightningRng.PHI64;
		float startX = LightningRng.nextUniformFloat(s1) * 32.0F;
		float startY = LightningRng.nextPositiveFloat(s2) * 64.0F + 128.0F;
		float startZ = LightningRng.nextUniformFloat(s3) * 32.0F;
		generatePoints(seed, startX, startY, startZ, 0.0F, 0.0F, 0.0F, BASE_WIDTH);
	}

	private void generatePoints(long seed,
			float startX, float startY, float startZ,
			float endX, float endY, float endZ,
			float width) {
		modelViewMatrix.transform(scratch.set(startX, startY, startZ, 1.0F));
		float tStartX = scratch.x;
		float tStartY = scratch.y;
		float tStartZ = scratch.z;
		modelViewMatrix.transform(scratch.set(endX, endY, endZ, 1.0F));
		float tEndX = scratch.x;
		float tEndY = scratch.y;
		float tEndZ = scratch.z;
		generatePointsRecursive(seed,
				startX, startY, startZ, 0.0F,
				endX, endY, endZ, 1.0F,
				tStartX, tStartY, tStartZ,
				tEndX, tEndY, tEndZ,
				width);
	}

	private float adjustWidth(float width, float startFrac, float endFrac) {
		if (age >= 8.0F) {
			return width - (age * EIGHTH_WIDTH - BASE_WIDTH);
		}
		return startFrac >= age * 0.125F ? 0.0F : width;
	}

	private void generatePointsRecursive(long seed,
			float startX, float startY, float startZ, float startFrac,
			float endX, float endY, float endZ, float endFrac,
			float tStartX, float tStartY, float tStartZ,
			float tEndX, float tEndY, float tEndZ,
			float width) {
		float adjusted = adjustWidth(width, startFrac, endFrac);
		if (adjusted < 0.0F) {
			return;
		}
		float cameraDistSq = Math.min(
				tStartX * tStartX + tStartY * tStartY + tStartZ * tStartZ,
				tEndX * tEndX + tEndY * tEndY + tEndZ * tEndZ);
		float dx = endX - startX;
		float dy = endY - startY;
		float dz = endZ - startZ;
		float segLenSq = dx * dx + dy * dy + dz * dz;
		if (segLenSq <= 2.4414062E-4F || segLenSq <= cameraDistSq * 2.4414062E-4F) {
			addQuads(tStartX, tStartY, tStartZ, tEndX, tEndY, tEndZ, cameraDistSq, adjusted);
			return;
		}

		long offsetSeed = LightningRng.stafford(seed ^ -8929738082300989633L);
		float midX = (startX + endX) * 0.5F;
		float midY = (startY + endY) * 0.5F;
		float midZ = (startZ + endZ) * 0.5F;
		float midFrac = (startFrac + endFrac) * 0.5F;

		offsetSeed += LightningRng.PHI64;
		float ox1 = LightningRng.nextUniformFloat(offsetSeed);
		offsetSeed += LightningRng.PHI64;
		float oy1 = LightningRng.nextUniformFloat(offsetSeed);
		offsetSeed += LightningRng.PHI64;
		float oz1 = LightningRng.nextUniformFloat(offsetSeed);
		float dot1 = (ox1 * dx + oy1 * dy + oz1 * dz) / segLenSq;
		ox1 -= dx * dot1;
		oy1 -= dy * dot1;
		oz1 -= dz * dot1;
		float lenSq1 = ox1 * ox1 + oy1 * oy1 + oz1 * oz1;
		float scalar1 = 0.1875F * (float) Math.sqrt(segLenSq / lenSq1);
		ox1 *= scalar1;
		oy1 *= scalar1;
		oz1 *= scalar1;
		midX += ox1;
		midY += oy1;
		midZ += oz1;

		offsetSeed += LightningRng.PHI64;
		float ox2 = LightningRng.nextUniformFloat(offsetSeed);
		offsetSeed += LightningRng.PHI64;
		float oy2 = LightningRng.nextUniformFloat(offsetSeed);
		offsetSeed += LightningRng.PHI64;
		float oz2 = LightningRng.nextUniformFloat(offsetSeed);
		float dot2 = (ox2 * dx + oy2 * dy + oz2 * dz) / segLenSq;
		ox2 -= dx * dot2;
		oy2 -= dy * dot2;
		oz2 -= dz * dot2;
		float lenSq2 = ox2 * ox2 + oy2 * oy2 + oz2 * oz2;
		float scalar2 = 0.375F * (float) Math.sqrt(segLenSq / lenSq2);
		ox2 *= scalar2;
		oy2 *= scalar2;
		oz2 *= scalar2;
		float branchX = midX + ox2;
		float branchY = midY + oy2;
		float branchZ = midZ + oz2;

		modelViewMatrix.transform(scratch.set(midX, midY, midZ, 1.0F));
		float tMidX = scratch.x;
		float tMidY = scratch.y;
		float tMidZ = scratch.z;
		modelViewMatrix.transform(scratch.set(branchX, branchY, branchZ, 1.0F));
		float tBranchX = scratch.x;
		float tBranchY = scratch.y;
		float tBranchZ = scratch.z;

		long splitSeed = LightningRng.stafford(seed ^ -5104707024794420520L);
		generatePointsRecursive(LightningRng.permute(splitSeed, 1),
				startX, startY, startZ, startFrac,
				midX, midY, midZ, midFrac,
				tStartX, tStartY, tStartZ,
				tMidX, tMidY, tMidZ,
				width);
		generatePointsRecursive(LightningRng.permute(splitSeed, 2),
				midX, midY, midZ, midFrac,
				endX, endY, endZ, endFrac,
				tMidX, tMidY, tMidZ,
				tEndX, tEndY, tEndZ,
				width);
		generatePointsRecursive(LightningRng.permute(splitSeed, 3),
				midX, midY, midZ, midFrac,
				branchX, branchY, branchZ, endFrac,
				tMidX, tMidY, tMidZ,
				tBranchX, tBranchY, tBranchZ,
				width * 0.5F);
	}

	private void addQuads(float tStartX, float tStartY, float tStartZ,
			float tEndX, float tEndY, float tEndZ,
			float cameraDistSq, float width) {
		float dx = tEndX - tStartX;
		float dy = tEndY - tStartY;
		float dz = tEndZ - tStartZ;
		float crossX = dz * tStartY - dy * tStartZ;
		float crossY = dx * tStartZ - dz * tStartX;
		float crossZ = dy * tStartX - dx * tStartY;
		float crossLenSq = crossX * crossX + crossY * crossY + crossZ * crossZ;
		if (crossLenSq <= 0.0F) {
			return;
		}
		float scalar = width * (float) Math.sqrt(cameraDistSq / crossLenSq);
		crossX *= scalar;
		crossY *= scalar;
		crossZ *= scalar;
		vertex(tStartX + crossX, tStartY + crossY, tStartZ + crossZ, OUTER_ARGB);
		vertex(tEndX + crossX, tEndY + crossY, tEndZ + crossZ, OUTER_ARGB);
		vertex(tEndX, tEndY, tEndZ, INNER_ARGB);
		vertex(tStartX, tStartY, tStartZ, INNER_ARGB);
		vertex(tStartX, tStartY, tStartZ, INNER_ARGB);
		vertex(tEndX, tEndY, tEndZ, INNER_ARGB);
		vertex(tEndX - crossX, tEndY - crossY, tEndZ - crossZ, OUTER_ARGB);
		vertex(tStartX - crossX, tStartY - crossY, tStartZ - crossZ, OUTER_ARGB);
	}

	private void vertex(float x, float y, float z, int argb) {
		buffer.addVertex(x, y, z).setColor(argb);
	}

	@SuppressWarnings("unused")
	private static int colorOf(int a, int r, int g, int b) {
		return FastColor.ARGB32.color(a, r, g, b);
	}
}
