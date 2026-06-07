package com.example.superheroes.client.render.lightning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Thick glowing column lightning, evoking the look of high-end particle
 * engines (Effekseer-style "lightning column"). Rendered as a stack of
 * jagged jittered cross-billboard quads with random side branches, in
 * additive blending via {@link RenderType#lightning()}.
 *
 * Vertices are emitted in world space, offset by negative camera so the
 * caller is responsible for translating the pose by the camera position.
 */
public final class BeamLightningRenderer {
	private static final int COLUMN_SEGMENTS = 18;
	private static final float COLUMN_HEIGHT = 96.0F;
	private static final float COLUMN_JITTER = 0.35F;
	private static final int BRANCH_COUNT = 5;
	private static final int BRANCH_SEGMENTS = 5;
	private static final float BRANCH_LENGTH = 6.5F;
	private static final float BRANCH_JITTER = 0.6F;

	private final PoseStack poseStack;
	private final MultiBufferSource buffers;
	private final long seed;
	private final float age;

	public BeamLightningRenderer(PoseStack poseStack, MultiBufferSource buffers, long seed, float age) {
		this.poseStack = poseStack;
		this.buffers = buffers;
		this.seed = seed;
		this.age = age;
	}

	public void render() {
		float intensity = computeIntensity();
		if (intensity <= 0.0F) {
			return;
		}
		VertexConsumer buf = buffers.getBuffer(RenderType.lightning());
		Matrix4f pose = poseStack.last().pose();

		float[] xs = new float[COLUMN_SEGMENTS + 1];
		float[] zs = new float[COLUMN_SEGMENTS + 1];
		long s = LightningRng.stafford(seed);
		for (int i = 0; i <= COLUMN_SEGMENTS; i++) {
			float taper = 1.0F - (float) i / COLUMN_SEGMENTS;
			s = LightningRng.permute(s, i + 1);
			xs[i] = LightningRng.toUniformFloat(s) * COLUMN_JITTER * (0.5F + 0.5F * taper);
			s = LightningRng.permute(s, i + 7919);
			zs[i] = LightningRng.toUniformFloat(s) * COLUMN_JITTER * (0.5F + 0.5F * taper);
		}
		xs[0] = 0.0F;
		zs[0] = 0.0F;

		for (int i = 0; i < COLUMN_SEGMENTS; i++) {
			float y0 = (float) i / COLUMN_SEGMENTS * COLUMN_HEIGHT;
			float y1 = (float) (i + 1) / COLUMN_SEGMENTS * COLUMN_HEIGHT;
			float widthCore = (1.0F - (float) i / COLUMN_SEGMENTS) * 0.18F + 0.12F;
			drawJaggedSegment(buf, pose, xs[i], y0, zs[i], xs[i + 1], y1, zs[i + 1], widthCore, intensity);
		}

		long branchSeed = LightningRng.permute(seed, 31337);
		for (int b = 0; b < BRANCH_COUNT; b++) {
			branchSeed = LightningRng.permute(branchSeed, b + 1);
			float t = LightningRng.toPositiveFloat(branchSeed) * 0.7F + 0.05F;
			int idx = Math.min(COLUMN_SEGMENTS, Math.max(0, (int) (t * COLUMN_SEGMENTS)));
			float bx0 = xs[idx];
			float by0 = (float) idx / COLUMN_SEGMENTS * COLUMN_HEIGHT;
			float bz0 = zs[idx];
			branchSeed = LightningRng.permute(branchSeed, 13);
			float dirX = LightningRng.toUniformFloat(branchSeed);
			branchSeed = LightningRng.permute(branchSeed, 19);
			float dirZ = LightningRng.toUniformFloat(branchSeed);
			float dirLen = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
			if (dirLen < 1e-3F) {
				continue;
			}
			dirX /= dirLen;
			dirZ /= dirLen;
			drawBranch(buf, pose, bx0, by0, bz0, dirX, dirZ, branchSeed, intensity);
		}
	}

	private void drawBranch(VertexConsumer buf, Matrix4f pose,
			float x0, float y0, float z0, float dirX, float dirZ, long branchSeed, float intensity) {
		float px = x0;
		float py = y0;
		float pz = z0;
		long s = branchSeed;
		for (int i = 0; i < BRANCH_SEGMENTS; i++) {
			float t0 = (float) i / BRANCH_SEGMENTS;
			float t1 = (float) (i + 1) / BRANCH_SEGMENTS;
			s = LightningRng.permute(s, i + 1);
			float jx = LightningRng.toUniformFloat(s) * BRANCH_JITTER;
			s = LightningRng.permute(s, i + 101);
			float jy = LightningRng.toUniformFloat(s) * BRANCH_JITTER;
			s = LightningRng.permute(s, i + 211);
			float jz = LightningRng.toUniformFloat(s) * BRANCH_JITTER;
			float nx = x0 + dirX * BRANCH_LENGTH * t1 + jx;
			float ny = y0 - BRANCH_LENGTH * 0.35F * t1 + jy;
			float nz = z0 + dirZ * BRANCH_LENGTH * t1 + jz;
			float w = (1.0F - t0) * 0.06F + 0.025F;
			drawCrossSegment(buf, pose, px, py, pz, nx, ny, nz, w, intensity * (1.0F - t0 * 0.6F));
			px = nx;
			py = ny;
			pz = nz;
		}
	}

	private void drawJaggedSegment(VertexConsumer buf, Matrix4f pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float width, float intensity) {
		drawCrossSegment(buf, pose, x0, y0, z0, x1, y1, z1, width, intensity);
	}

	private void drawCrossSegment(VertexConsumer buf, Matrix4f pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float width, float intensity) {
		float a = Math.max(0.0F, Math.min(1.0F, intensity));
		float outerR = 0.45F;
		float outerG = 0.65F;
		float outerB = 1.00F;
		float coreR = 0.95F;
		float coreG = 0.97F;
		float coreB = 1.00F;

		drawQuad(buf, pose, x0, y0, z0, x1, y1, z1, width, 1.0F, 0.0F, 0.0F, outerR, outerG, outerB, 0.35F * a);
		drawQuad(buf, pose, x0, y0, z0, x1, y1, z1, width * 0.55F, 1.0F, 0.0F, 0.0F, coreR, coreG, coreB, 0.85F * a);
		drawQuad(buf, pose, x0, y0, z0, x1, y1, z1, width, 0.0F, 0.0F, 1.0F, outerR, outerG, outerB, 0.35F * a);
		drawQuad(buf, pose, x0, y0, z0, x1, y1, z1, width * 0.55F, 0.0F, 0.0F, 1.0F, coreR, coreG, coreB, 0.85F * a);
		drawQuad(buf, pose, x0, y0, z0, x1, y1, z1, width * 0.22F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, a);
		drawQuad(buf, pose, x0, y0, z0, x1, y1, z1, width * 0.22F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, a);
	}

	private void drawQuad(VertexConsumer buf, Matrix4f pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float width, float sx, float sy, float sz,
			float r, float g, float b, float a) {
		float wx = sx * width;
		float wy = sy * width;
		float wz = sz * width;
		addV(buf, pose, x0 + wx, y0 + wy, z0 + wz, r, g, b, a);
		addV(buf, pose, x0 - wx, y0 - wy, z0 - wz, r, g, b, a);
		addV(buf, pose, x1 - wx, y1 - wy, z1 - wz, r, g, b, a);
		addV(buf, pose, x1 + wx, y1 + wy, z1 + wz, r, g, b, a);
		addV(buf, pose, x0 + wx, y0 + wy, z0 + wz, r, g, b, a);
		addV(buf, pose, x1 + wx, y1 + wy, z1 + wz, r, g, b, a);
		addV(buf, pose, x1 - wx, y1 - wy, z1 - wz, r, g, b, a);
		addV(buf, pose, x0 - wx, y0 - wy, z0 - wz, r, g, b, a);
	}

	private static void addV(VertexConsumer buf, Matrix4f pose, float x, float y, float z,
			float r, float g, float b, float a) {
		buf.addVertex(pose, x, y, z).setColor(r, g, b, a);
	}

	private float computeIntensity() {
		if (age < 0.0F) {
			return 0.0F;
		}
		if (age <= 4.0F) {
			float flicker = 0.85F + 0.15F * (float) Math.sin(age * 6.0);
			return flicker;
		}
		if (age >= 14.0F) {
			return 0.0F;
		}
		return 1.0F - (age - 4.0F) / 10.0F;
	}
}
