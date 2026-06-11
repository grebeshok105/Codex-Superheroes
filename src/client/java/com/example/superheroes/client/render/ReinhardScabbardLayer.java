package com.example.superheroes.client.render;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.RemoteHeroSkins;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Reinhard's royal scabbard on the left hip. The scabbard itself NEVER
 * disappears while Reinhard is active; the sword (the real Royal Icicle item)
 * sits inside it while sheathed and leaves it when drawn (the actual item then
 * appears in the player's hand, so we simply stop rendering the hip copy).
 * Pure client visual — no gameplay impact.
 */
public final class ReinhardScabbardLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

	public ReinhardScabbardLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		if (!isReinhard(player)) {
			return;
		}
		boolean drawn = isSwordDrawn(player);

		poseStack.pushPose();
		getParentModel().body.translateAndRotate(poseStack);
		// left hip, slightly behind the leg, sloped backwards like a real sheath
		poseStack.translate(0.30f, 0.66f, 0.04f);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-18f));
		poseStack.mulPose(Axis.XP.rotationDegrees(8f));

		if (!drawn) {
			// the very same Royal Icicle, resting in the scabbard: hilt sticks out the top
			poseStack.pushPose();
			poseStack.translate(0f, 0.02f, 0f);
			poseStack.mulPose(Axis.YP.rotationDegrees(90f));
			// -45 (not 135): hilt out the top, blade pointing DOWN inside the sheath
			poseStack.mulPose(Axis.ZP.rotationDegrees(-45f));
			poseStack.scale(0.85f, 0.85f, 0.85f);
			ItemStack sword = new ItemStack(ModItems.ROYAL_ICICLE);
			Minecraft.getInstance().getItemRenderer().renderStatic(sword, ItemDisplayContext.FIXED,
					packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, player.level(), player.getId());
			poseStack.popPose();
		}

		// scabbard shell drawn on top so the blade reads as "inside"
		drawScabbard(poseStack, bufferSource, drawn);
		poseStack.popPose();
	}

	private static boolean isReinhard(AbstractClientPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
			return ReinhardHero.ID.equals(ClientHeroState.heroId());
		}
		ResourceLocation remote = RemoteHeroSkins.get(player.getUUID());
		return ReinhardHero.ID.equals(remote);
	}

	/** Drawn = the real Royal Icicle is in either hand (equipment is synced to all clients). */
	private static boolean isSwordDrawn(LivingEntity player) {
		return player.getMainHandItem().is(ModItems.ROYAL_ICICLE)
				|| player.getOffhandItem().is(ModItems.ROYAL_ICICLE);
	}

	/**
	 * Procedural scabbard: a slim dark-navy case with gold throat, mid band and
	 * chape, built from flat color quads (opaque color quads, debugQuads render type).
	 */
	private static void drawScabbard(PoseStack poseStack, MultiBufferSource bufferSource, boolean drawn) {
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugQuads());
		Matrix4f m = poseStack.last().pose();

		float topY = 0.02f;
		float botY = 0.46f;
		float w = 0.055f; // half width (along z in local space)
		float t = 0.030f; // half thickness (along x)

		// body: dark navy, slightly lighter when empty so the opening reads
		float r = 0.10f, g = 0.11f, b = 0.20f;
		box(buffer, m, -t, topY, -w, t, botY, w, r, g, b, 1f);

		// gold throat (top), mid band and chape (tip)
		float gr = 0.86f, gg = 0.70f, gb = 0.28f;
		box(buffer, m, -t - 0.006f, topY, -w - 0.006f, t + 0.006f, topY + 0.045f, w + 0.006f, gr, gg, gb, 1f);
		box(buffer, m, -t - 0.004f, topY + 0.22f, -w - 0.004f, t + 0.004f, topY + 0.255f, w + 0.004f, gr, gg, gb, 1f);
		box(buffer, m, -t - 0.006f, botY - 0.045f, -w - 0.006f, t + 0.006f, botY, w + 0.006f, gr, gg, gb, 1f);

		// dark opening slot on top when the sword is drawn (empty scabbard)
		if (drawn) {
			box(buffer, m, -t * 0.5f, topY - 0.004f, -w * 0.6f, t * 0.5f, topY + 0.004f, w * 0.6f,
					0.03f, 0.03f, 0.06f, 1f);
		}
	}

	/** Axis-aligned box from colored quads (local space, units = blocks). */
	private static void box(VertexConsumer buf, Matrix4f m, float x0, float y0, float z0,
			float x1, float y1, float z1, float r, float g, float b, float a) {
		// -X / +X
		quad(buf, m, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r * 0.85f, g * 0.85f, b * 0.85f, a);
		quad(buf, m, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, r, g, b, a);
		// -Z / +Z
		quad(buf, m, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, r * 0.92f, g * 0.92f, b * 0.92f, a);
		quad(buf, m, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r * 0.92f, g * 0.92f, b * 0.92f, a);
		// top / bottom
		quad(buf, m, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r * 1.08f, g * 1.08f, b * 1.08f, a);
		quad(buf, m, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1, r * 0.7f, g * 0.7f, b * 0.7f, a);
	}

	private static void quad(VertexConsumer buf, Matrix4f m,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz,
			float r, float g, float b, float a) {
		buf.addVertex(m, ax, ay, az).setColor(r, g, b, a);
		buf.addVertex(m, bx, by, bz).setColor(r, g, b, a);
		buf.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
		buf.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
	}
}
