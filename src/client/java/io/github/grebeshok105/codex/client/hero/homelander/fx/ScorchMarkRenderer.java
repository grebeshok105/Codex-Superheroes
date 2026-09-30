package io.github.grebeshok105.codex.client.hero.homelander.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.net.ScorchMarkStore;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Homelander's laser-scorch decal renderer: draws every mark in
 * {@link ScorchMarkStore} as one textured quad on its block face, offset
 * {@value #NORMAL_OFFSET} along the face normal plus a polygon-offset bias so
 * it wins depth against the surface without fighting it. Marks farther than
 * render distance or painted on a face that is no longer solid are skipped —
 * the server prunes those lazily on its side.
 *
 * <p>All visible marks are emitted into a single quad buffer per frame; the
 * store is read as raw columns, so nothing is allocated per mark.
 */
public final class ScorchMarkRenderer {
	private static final ResourceLocation TEXTURE =
			ModId.of("textures/effect/homelander/laser_scorch.png");
	private static final double NORMAL_OFFSET = 0.002;
	/** Extra slack past render distance so a mark's edge doesn't pop at the boundary. */
	private static final double CULL_SLACK = 2.0;

	private ScorchMarkRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ScorchMarkRenderer::render);
	}

	private static void render(WorldRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		Vec3 cam = ctx.camera().getPosition();
		double maxDist = mc.options.getEffectiveRenderDistance() * 16.0 + CULL_SLACK;
		double maxDistSqr = maxDist * maxDist;

		// Emit into one buffer; bail before touching GL if nothing is in range.
		BufferBuilder bb = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		PoseStack ps = ctx.matrixStack();
		ps.pushPose();
		ps.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = ps.last().pose();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

		int emitted = 0;
		for (int i = 0; i < ScorchMarkStore.CAPACITY; i++) {
			if (!ScorchMarkStore.live(i)) {
				continue;
			}
			pos.set(ScorchMarkStore.posLong(i));
			Direction dir = Direction.from3DDataValue(ScorchMarkStore.face(i));
			if (!mc.level.getBlockState(pos).isFaceSturdy(mc.level, pos, dir)) {
				continue;
			}
			double dx = pos.getX() + 0.5 - cam.x;
			double dy = pos.getY() + 0.5 - cam.y;
			double dz = pos.getZ() + 0.5 - cam.z;
			if (dx * dx + dy * dy + dz * dz > maxDistSqr) {
				continue;
			}
			lightPos.set(pos).move(dir);
			int light = LevelRenderer.getLightColor(mc.level, lightPos);
			emit(bb, matrix, pos, dir, ScorchMarkStore.u(i), ScorchMarkStore.v(i),
					ScorchMarkStore.size(i), ScorchMarkStore.rot(i), light);
			emitted++;
		}
		ps.popPose();
		if (emitted == 0) {
			return;
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enablePolygonOffset();
		RenderSystem.polygonOffset(-1f, -10f);
		RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.setShaderTexture(0, TEXTURE);
		mc.gameRenderer.lightTexture().turnOnLightLayer();
		mc.gameRenderer.overlayTexture().setupOverlayColor();

		BufferUploader.drawWithShader(bb.buildOrThrow());

		mc.gameRenderer.overlayTexture().teardownOverlayColor();
		RenderSystem.polygonOffset(0f, 0f);
		RenderSystem.disablePolygonOffset();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	/**
	 * Emits one quad centred on the face at ({@code u}, {@code v}), rotated
	 * in-plane by {@code rot} degrees. {@code u}/{@code v} map onto the same
	 * per-axis in-plane bases the server used to build the mark: X faces use
	 * (+z, +y), Y faces (+x, +z), Z faces (+x, +y).
	 */
	private static void emit(BufferBuilder bb, Matrix4f matrix, BlockPos pos, Direction dir,
			float u, float v, float size, int rot, int light) {
		float uAx, uAy, uAz, vAx, vAy, vAz;
		switch (dir.getAxis()) {
			case X -> { uAx = 0; uAy = 0; uAz = 1; vAx = 0; vAy = 1; vAz = 0; }
			case Y -> { uAx = 1; uAy = 0; uAz = 0; vAx = 0; vAy = 0; vAz = 1; }
			default -> { uAx = 1; uAy = 0; uAz = 0; vAx = 0; vAy = 1; vAz = 0; }
		}
		int step = dir.getAxisDirection().getStep();
		float off = (step > 0 ? 1f : 0f) + (float) (step * NORMAL_OFFSET);
		float nx = dir.getStepX();
		float ny = dir.getStepY();
		float nz = dir.getStepZ();
		float cx = pos.getX() + uAx * u + vAx * v + nx * nx * off;
		float cy = pos.getY() + uAy * u + vAy * v + ny * ny * off;
		float cz = pos.getZ() + uAz * u + vAz * v + nz * nz * off;

		double rad = Math.toRadians(rot);
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);
		float h = size * 0.5f;
		// corners (du,dv) — rotated in the face plane.
		float[][] corners = {{-h, -h}, {h, -h}, {h, h}, {-h, h}};
		float[][] texUv = {{0f, 1f}, {1f, 1f}, {1f, 0f}, {0f, 0f}};
		for (int c = 0; c < 4; c++) {
			float du = corners[c][0] * cos - corners[c][1] * sin;
			float dv = corners[c][0] * sin + corners[c][1] * cos;
			bb.addVertex(matrix,
					cx + uAx * du + vAx * dv,
					cy + uAy * du + vAy * dv,
					cz + uAz * du + vAz * dv)
				.setColor(1f, 1f, 1f, 1f)
				.setUv(texUv[c][0], texUv[c][1])
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(nx, ny, nz);
		}
	}
}
