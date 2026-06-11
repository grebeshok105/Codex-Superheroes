package com.example.superheroes.client.render;

import com.example.superheroes.client.ClientNanoFormState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;

/**
 * Нано-оружие Mark 85 на модели игрока (Endgame-стиль, оранжевая энергия):
 *  1 — клинок из правой руки, 2 — супермолот, 3 — энергощит на левом предплечье.
 */
public final class IronManNanoFormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	// Тёплая «нано-энергия» Старка.
	private static final float CORE_R = 1.0f;
	private static final float CORE_G = 0.78f;
	private static final float CORE_B = 0.35f;
	private static final float EDGE_R = 1.0f;
	private static final float EDGE_G = 0.36f;
	private static final float EDGE_B = 0.16f;

	public IronManNanoFormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		int form = ClientNanoFormState.formFor(player.getUUID());
		if (form == 0) {
			return;
		}
		float pulse = 0.85f + 0.15f * (float) Math.sin((ageInTicks + partialTick) * 0.32f);
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());

		switch (form) {
			case 1 -> renderBlade(poseStack, buffer, pulse);
			case 2 -> renderHammer(poseStack, buffer, pulse);
			case 3 -> renderShield(poseStack, buffer, pulse);
			default -> {
			}
		}
	}

	/** Клинок: длинное сужающееся лезвие из правого запястья. */
	private void renderBlade(PoseStack poseStack, VertexConsumer buffer, float pulse) {
		poseStack.pushPose();
		getParentModel().rightArm.translateAndRotate(poseStack);
		Matrix4f m = poseStack.last().pose();
		// Рука в модели направлена вниз: лезвие продолжает её от запястья (y ≈ 0.55) вниз.
		float baseY = 0.55f;
		float tipY = baseY + 0.95f;
		float w = 0.055f;
		float d = 0.10f;
		// Две перекрещённые плоскости — объёмное «энергетическое» лезвие.
		bladeFin(buffer, m, baseY, tipY, w, 0f, pulse);
		bladeFin(buffer, m, baseY, tipY, 0f, d, pulse);
		poseStack.popPose();
	}

	private static void bladeFin(VertexConsumer buffer, Matrix4f m, float baseY, float tipY,
			float halfX, float halfZ, float pulse) {
		float a = 0.85f * pulse;
		// четырёхугольник: широкое основание у запястья, остриё внизу
		quad(buffer, m,
				-halfX, baseY, -halfZ,
				halfX, baseY, halfZ,
				halfX * 0.25f, tipY, halfZ * 0.25f,
				-halfX * 0.25f, tipY, -halfZ * 0.25f,
				CORE_R, CORE_G, CORE_B, a);
		// внешняя кромка с лёгким расширением — оранжевое свечение
		float gx = halfX * 1.7f + 0.012f;
		float gz = halfZ * 1.7f + 0.012f;
		quad(buffer, m,
				-gx, baseY, -gz,
				gx, baseY, gz,
				gx * 0.25f, tipY + 0.04f, gz * 0.25f,
				-gx * 0.25f, tipY + 0.04f, -gz * 0.25f,
				EDGE_R, EDGE_G, EDGE_B, a * 0.35f);
	}

	/** Супермолот: массивная глыба энергии на правой руке. */
	private void renderHammer(PoseStack poseStack, VertexConsumer buffer, float pulse) {
		poseStack.pushPose();
		getParentModel().rightArm.translateAndRotate(poseStack);
		Matrix4f m = poseStack.last().pose();
		// Голова молота вокруг кулака (y ≈ 0.62), перпендикулярно руке.
		float cy = 0.68f;
		box(buffer, m, -0.16f, cy - 0.14f, -0.30f, 0.16f, cy + 0.14f, 0.30f,
				0.55f, 0.42f, 0.36f, 0.92f * pulse);
		// светящееся ядро
		box(buffer, m, -0.10f, cy - 0.08f, -0.24f, 0.10f, cy + 0.08f, 0.24f,
				CORE_R, CORE_G, CORE_B, 0.8f * pulse);
		// рукоять-перемычка от кулака к голове
		box(buffer, m, -0.045f, 0.40f, -0.045f, 0.045f, cy, 0.045f,
				EDGE_R, EDGE_G, EDGE_B, 0.7f * pulse);
		poseStack.popPose();
	}

	/** Энергощит: диск на левом предплечье. */
	private void renderShield(PoseStack poseStack, VertexConsumer buffer, float pulse) {
		poseStack.pushPose();
		getParentModel().leftArm.translateAndRotate(poseStack);
		Matrix4f m = poseStack.last().pose();
		float cy = 0.42f;
		float cx = 0.16f; // наружная сторона левой руки
		int segments = 24;
		float radius = 0.46f;
		float inner = 0.30f;
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * 2.0 * i / segments;
			double a1 = Math.PI * 2.0 * (i + 1) / segments;
			float y0 = cy + (float) (Math.cos(a0) * radius);
			float z0 = (float) (Math.sin(a0) * radius);
			float y1 = cy + (float) (Math.cos(a1) * radius);
			float z1 = (float) (Math.sin(a1) * radius);
			float iy0 = cy + (float) (Math.cos(a0) * inner);
			float iz0 = (float) (Math.sin(a0) * inner);
			float iy1 = cy + (float) (Math.cos(a1) * inner);
			float iz1 = (float) (Math.sin(a1) * inner);
			// внутренний диск (центр → внутреннее кольцо), полупрозрачное ядро
			quad(buffer, m,
					cx, cy, 0f,
					cx, cy, 0f,
					cx, iy0, iz0,
					cx, iy1, iz1,
					CORE_R, CORE_G, CORE_B, 0.30f * pulse);
			// внешнее кольцо — яркий оранжевый обод
			quad(buffer, m,
					cx, iy0, iz0,
					cx, iy1, iz1,
					cx, y1, z1,
					cx, y0, z0,
					EDGE_R, EDGE_G, EDGE_B, 0.55f * pulse);
		}
		poseStack.popPose();
	}

	/** Квад двусторонний: RenderType.lightning() рисует группами по 4 вершины. */
	private static void quad(VertexConsumer buffer, Matrix4f m,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			float r, float g, float b, float a) {
		vertex(buffer, m, x0, y0, z0, r, g, b, a);
		vertex(buffer, m, x1, y1, z1, r, g, b, a);
		vertex(buffer, m, x2, y2, z2, r, g, b, a);
		vertex(buffer, m, x3, y3, z3, r, g, b, a);
		// обратная сторона
		vertex(buffer, m, x3, y3, z3, r, g, b, a);
		vertex(buffer, m, x2, y2, z2, r, g, b, a);
		vertex(buffer, m, x1, y1, z1, r, g, b, a);
		vertex(buffer, m, x0, y0, z0, r, g, b, a);
	}

	private static void box(VertexConsumer buffer, Matrix4f m,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float r, float g, float b, float a) {
		quad(buffer, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a); // перед
		quad(buffer, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a); // зад
		quad(buffer, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a); // лево
		quad(buffer, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a); // право
		quad(buffer, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a); // низ
		quad(buffer, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a); // верх
	}

	private static void vertex(VertexConsumer buffer, Matrix4f m, float x, float y, float z,
			float r, float g, float b, float a) {
		buffer.addVertex(m, x, y, z).setColor(r, g, b, a);
	}
}
