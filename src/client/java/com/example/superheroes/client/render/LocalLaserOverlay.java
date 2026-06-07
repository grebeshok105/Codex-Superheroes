package com.example.superheroes.client.render;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.effect.ModEffects;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LocalLaserOverlay {
	private static final double RANGE = 64.0;

	private LocalLaserOverlay() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(LocalLaserOverlay::render);
	}

	private static void render(WorldRenderContext context) {
		if (!ClientHeroState.data().isActive(AbilityIds.EYE_LASERS)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null) {
			return;
		}
		float partial = context.tickCounter().getGameTimeDeltaPartialTick(true);
		Vec3 eye = player.getEyePosition(partial);
		Vec3 dir = player.getViewVector(partial);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 entitySearchEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				level, player, eye, entitySearchEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		Vec3 actualEnd;
		if (hit != null) {
			LivingEntity target = (LivingEntity) hit.getEntity();
			actualEnd = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ());
		} else {
			actualEnd = entitySearchEnd;
		}
		Vec3 right = dir.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1e-6) {
			right = new Vec3(1, 0, 0);
		}
		right = right.normalize();
		double eyeSep = 0.11;
		Vec3 forward = dir.scale(0.35);
		Vec3 leftEye = eye.add(forward).add(right.scale(-eyeSep));
		Vec3 rightEye = eye.add(forward).add(right.scale(eyeSep));
		float widthMul = ModEffects.isMadness(player) ? 1.1f : 0.45f;
		float intensity = ModEffects.isMadness(player) ? 1.3f : 1.0f;
		BeamRenderer.draw(context, leftEye, actualEnd, intensity, widthMul);
		BeamRenderer.draw(context, rightEye, actualEnd, intensity, widthMul);
	}
}
