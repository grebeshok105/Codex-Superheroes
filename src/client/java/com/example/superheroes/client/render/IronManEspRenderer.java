package com.example.superheroes.client.render;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientHeroState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class IronManEspRenderer {
	private static final double RANGE = 64.0;
	private static final float COLOR_GOLD_R = 1.00f;
	private static final float COLOR_GOLD_G = 0.82f;
	private static final float COLOR_GOLD_B = 0.20f;
	private static final float COLOR_RED_R = 1.00f;
	private static final float COLOR_RED_G = 0.18f;
	private static final float COLOR_RED_B = 0.18f;
	private static final int LABEL_GOLD = 0xFFFFD24A;
	private static final int LABEL_RED = 0xFFFF4040;
	private static final int LABEL_BG = 0xB0140204;

	private IronManEspRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(IronManEspRenderer::onRender);
	}

	private static void onRender(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
		if (!ClientHeroState.data().isActive(AbilityIds.BOX_ESP)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		Camera camera = ctx.camera();
		Vec3 cam = camera.getPosition();
		PoseStack ps = ctx.matrixStack();

		ps.pushPose();
		ps.translate(-cam.x, -cam.y, -cam.z);

		drawBoxes(ps, level, mc, cam);
		drawLabels(ps, level, mc, camera);

		ps.popPose();
	}

	private static void drawBoxes(PoseStack ps, ClientLevel level, Minecraft mc, Vec3 cam) {
		double rangeSq = RANGE * RANGE;
		Entity self = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;

		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.lineWidth(2.0F);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Tesselator tess = Tesselator.getInstance();
		BufferBuilder bb = tess.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f m = ps.last().pose();

		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity living)) {
				continue;
			}
			if (!living.isAlive() || living == self) {
				continue;
			}
			if (living.distanceToSqr(cam) > rangeSq) {
				continue;
			}
			AABB box = living.getBoundingBox();
			float lifeFrac = Math.max(0f, Math.min(1f, living.getHealth() / Math.max(1f, living.getMaxHealth())));
			float r = COLOR_RED_R * (1.0f - lifeFrac) + COLOR_GOLD_R * lifeFrac;
			float g = COLOR_RED_G * (1.0f - lifeFrac) + COLOR_GOLD_G * lifeFrac;
			float b = COLOR_RED_B * (1.0f - lifeFrac) + COLOR_GOLD_B * lifeFrac;
			addBoxLines(bb, m, box, r, g, b, 0.95f);
		}

		MeshData mesh = bb.build();
		if (mesh != null) {
			BufferUploader.drawWithShader(mesh);
		}

		RenderSystem.lineWidth(1.0F);
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
	}

	private static void drawLabels(PoseStack ps, ClientLevel level, Minecraft mc, Camera camera) {
		double rangeSq = RANGE * RANGE;
		Entity self = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
		Font font = mc.font;
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		Vec3 cam = camera.getPosition();

		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity living)) {
				continue;
			}
			if (!living.isAlive() || living == self) {
				continue;
			}
			if (living.distanceToSqr(cam) > rangeSq) {
				continue;
			}

			ps.pushPose();
			AABB box = living.getBoundingBox();
			double headX = (box.minX + box.maxX) * 0.5;
			double headY = box.maxY + 0.45;
			double headZ = (box.minZ + box.maxZ) * 0.5;
			ps.translate(headX, headY, headZ);
			ps.mulPose(camera.rotation());
			ps.scale(-0.025f, -0.025f, 0.025f);
			Matrix4f m = ps.last().pose();

			Component name = living.getDisplayName();
			String hpText = String.format("%.1f / %.1f", living.getHealth(), living.getMaxHealth());

			float nameWidth = font.width(name);
			float hpWidth = font.width(hpText);
			float halfName = nameWidth * 0.5f;
			float halfHp = hpWidth * 0.5f;

			int bgPad = 2;
			drawTextBg(buffers, m, -halfName - bgPad, -10 - bgPad, halfName + bgPad, -10 + 9 + bgPad, LABEL_BG);
			drawTextBg(buffers, m, -halfHp - bgPad, 1 - bgPad, halfHp + bgPad, 1 + 9 + bgPad, LABEL_BG);

			font.drawInBatch(name, -halfName, -10, LABEL_GOLD, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
			font.drawInBatch(hpText, -halfHp, 1, LABEL_RED, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

			ps.popPose();
		}
		buffers.endBatch();
	}

	private static void drawTextBg(MultiBufferSource buffers, Matrix4f m, float x0, float y0, float x1, float y1, int argb) {
		var consumer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.textBackgroundSeeThrough());
		float a = ((argb >> 24) & 0xFF) / 255f;
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		consumer.addVertex(m, x0, y1, 0).setColor(r, g, b, a).setLight(0xF000F0);
		consumer.addVertex(m, x1, y1, 0).setColor(r, g, b, a).setLight(0xF000F0);
		consumer.addVertex(m, x1, y0, 0).setColor(r, g, b, a).setLight(0xF000F0);
		consumer.addVertex(m, x0, y0, 0).setColor(r, g, b, a).setLight(0xF000F0);
	}

	private static void addBoxLines(BufferBuilder bb, Matrix4f m, AABB box, float r, float g, float b, float a) {
		float x0 = (float) box.minX;
		float y0 = (float) box.minY;
		float z0 = (float) box.minZ;
		float x1 = (float) box.maxX;
		float y1 = (float) box.maxY;
		float z1 = (float) box.maxZ;
		// 12 edges
		line(bb, m, x0, y0, z0, x1, y0, z0, r, g, b, a);
		line(bb, m, x1, y0, z0, x1, y0, z1, r, g, b, a);
		line(bb, m, x1, y0, z1, x0, y0, z1, r, g, b, a);
		line(bb, m, x0, y0, z1, x0, y0, z0, r, g, b, a);

		line(bb, m, x0, y1, z0, x1, y1, z0, r, g, b, a);
		line(bb, m, x1, y1, z0, x1, y1, z1, r, g, b, a);
		line(bb, m, x1, y1, z1, x0, y1, z1, r, g, b, a);
		line(bb, m, x0, y1, z1, x0, y1, z0, r, g, b, a);

		line(bb, m, x0, y0, z0, x0, y1, z0, r, g, b, a);
		line(bb, m, x1, y0, z0, x1, y1, z0, r, g, b, a);
		line(bb, m, x1, y0, z1, x1, y1, z1, r, g, b, a);
		line(bb, m, x0, y0, z1, x0, y1, z1, r, g, b, a);
	}

	private static void line(BufferBuilder bb, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
		bb.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
		bb.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
	}
}
