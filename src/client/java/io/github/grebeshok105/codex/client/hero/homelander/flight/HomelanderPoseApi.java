package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderEmfEngine;
import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderEmfRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

/**
 * The seam Agent B (clap/milk), Agent C (laser VFX) and Agent D (trails/F5
 * camera) consume: one-shot + loop clip control on the EMF runtime, the
 * presentation flag that hides the vanilla model, and the interpolated
 * whole-body transform.
 *
 * <p>Clip weights: {@link #playClip} fades the named clip in and every other
 * action clip out — a single action slot, same as the old
 * {@code PlayerAnimator.Layer.ACTION} — so the per-channel weight sum never
 * exceeds 1. Flight clips ({@code takeoff/hover/boost}) are owned by
 * {@link HomelanderFlightDriver} and cannot be played through this API.
 */
public final class HomelanderPoseApi {
	private HomelanderPoseApi() {
	}

	/** Flight clip var names — owned by the driver, not the action slot. */
	static final Set<String> FLIGHT_CLIPS = Set.of("takeoff", "hover", "boost");

	/** Whether the EMF model currently presents this player (any weight on). */
	public static boolean isPresenting(int entityId) {
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(entityId);
		return runtime != null && runtime.presenting();
	}

	/** {@code PlayerModelSuppressions} predicate — hides the vanilla model. */
	public static boolean suppressesVanillaModel(AbstractClientPlayer player) {
		return isPresenting(player.getId());
	}

	/**
	 * Fades {@code clipId} in (weight → 1) and every other action clip out;
	 * restarts the clip's local time at frame 0. No-op if the clip is unknown
	 * or the entity has no EMF runtime (not rendered as Homelander).
	 */
	public static void playClip(int entityId, ResourceLocation clipId) {
		String name = varName(clipId);
		if (name == null || FLIGHT_CLIPS.contains(name)) {
			return;
		}
		HomelanderEmfRuntime runtime = runtimeFor(entityId);
		if (runtime == null) {
			return;
		}
		for (String clip : HomelanderEmfRuntime.clipNames()) {
			if (!FLIGHT_CLIPS.contains(clip)) {
				runtime.playback().setTargetWeight(clip, clip.equals(name) ? 1f : 0f);
			}
		}
		runtime.playback().restart(name);
		runtime.pushVars();
	}

	/** Fades {@code clipId} out (weight → 0); the clip time freezes. */
	public static void stopClip(int entityId, ResourceLocation clipId) {
		String name = varName(clipId);
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(entityId);
		if (name == null || runtime == null) {
			return;
		}
		runtime.playback().setTargetWeight(name, 0f);
	}

	/** {@link #stopClip} for several clips at once (channel teardown). */
	public static void stopClips(int entityId, ResourceLocation... clipIds) {
		for (ResourceLocation clipId : clipIds) {
			stopClip(entityId, clipId);
		}
	}

	/**
	 * The head bone's last-rendered rotation in degrees (x=pitch, y=yaw,
	 * z=roll, same semantics as the old
	 * {@code PlayerPoseApplier.renderedRotationDeg}). Read back from the EMF
	 * part after the expression pass — one rendered frame stale, fine for
	 * VFX anchors. Zero vector when not presenting.
	 */
	public static Vector3f currentHeadAnglesDeg(int entityId) {
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(entityId);
		HomelanderEmfEngine engine = HomelanderEmfRuntime.engine();
		if (runtime == null || engine == null || !runtime.presenting()) {
			return new Vector3f();
		}
		ModelPart head = engine.model().part("head");
		if (head == null) {
			return new Vector3f();
		}
		return new Vector3f(
				(float) Math.toDegrees(head.xRot),
				(float) Math.toDegrees(head.yRot),
				(float) Math.toDegrees(head.zRot));
	}

	/**
	 * Interpolated whole-body pitch/roll + vertical center offset — the
	 * {@code var.lean_*} values in degrees/pixels, sampled across the last
	 * render tick. {@link HomelanderBodyTransform#IDENTITY} for anything not
	 * presenting through the EMF runtime.
	 */
	public static HomelanderBodyTransform currentBodyTransform(int entityId, float partialTick) {
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(entityId);
		if (runtime == null || !runtime.presenting()) {
			return HomelanderBodyTransform.IDENTITY;
		}
		return new HomelanderBodyTransform(
				Math.toDegrees(runtime.lerpLeanPitch(partialTick)),
				Math.toDegrees(runtime.lerpLeanRoll(partialTick)),
				runtime.lerpLeanY(partialTick));
	}

	/**
	 * {@code HeroClientContext.flightCameraFocus} provider: the third-person
	 * camera offset that keeps the tilted EMF body centered, or {@code null}
	 * when the entity is not presenting.
	 */
	public static @Nullable Vec3 cameraFocusOffset(LivingEntity entity, float tickDelta) {
		HomelanderBodyTransform body = currentBodyTransform(entity.getId(), tickDelta);
		if (body.equals(HomelanderBodyTransform.IDENTITY)) {
			return null;
		}
		float bodyYaw = Mth.rotLerp(tickDelta, entity.yBodyRotO, entity.yBodyRot);
		return body.cameraCenterOffsetBlocks(bodyYaw);
	}

	static @Nullable HomelanderEmfRuntime runtimeFor(int entityId) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}
		Entity entity = level.getEntity(entityId);
		if (entity == null) {
			return null;
		}
		return HomelanderEmfRuntime.of(entity);
	}

	private static @Nullable String varName(ResourceLocation clipId) {
		String path = clipId.getPath();
		String name = path.substring(path.lastIndexOf('/') + 1);
		return name.isEmpty() ? null : name;
	}
}
