package io.github.grebeshok105.codex.client.core.vfx.anchor;

import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.client.core.emf.RenderedPoseCache;
import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * World-space render anchors for humanoid bodies. Anchors follow the pose the
 * player is actually rendered in — interpolated body/head rotations plus the
 * {@link FlightBodyTransform} tilt — so beams and trails attached to them
 * leave the rendered eyes rather than the feet or the camera.
 *
 * <p>Eye geometry: the head pivot sits {@value #HEAD_PIVOT} blocks above the
 * feet (unscaled); the eyes sit {@value #EYE_FORWARD} forward,
 * ±{@value #EYE_LATERAL} lateral and {@value #EYE_UP} up from the pivot along
 * the head's own axes — the three eye offsets multiplied by {@code scale}.
 * In first person the local player uses the camera frame instead:
 * +{@value #FIRST_PERSON_FORWARD} forward, ±{@value #FIRST_PERSON_LATERAL}
 * lateral, −{@value #FIRST_PERSON_DOWN} down.
 */
public final class HumanoidAnchors {
	private static final Vec3 UP = new Vec3(0, 1, 0);

	private static final double HEAD_PIVOT = 1.5;
	private static final double EYE_FORWARD = 0.25;
	// 2px eye centers on the 8px-wide head face (1px = 0.0625 blocks).
	private static final double EYE_LATERAL = 0.125;
	private static final double EYE_UP = 0.0625;

	private static final double FIRST_PERSON_FORWARD = 0.35;
	private static final double FIRST_PERSON_LATERAL = 0.11;
	private static final double FIRST_PERSON_DOWN = 0.08;

	/** Model-space eye centers (pixels): +x is the left eye, -z the face front. */
	private static final Vector3f EYE_LEFT_PX = new Vector3f(2f, -1f, -4f);
	private static final Vector3f EYE_RIGHT_PX = new Vector3f(-2f, -1f, -4f);
	/** Render-thread scratch for {@link RenderedPoseCache#headPose}. */
	private static final float[] HEAD_SCRATCH = new float[6];

	private HumanoidAnchors() {
	}

	/**
	 * Pure anchor math. {@code tilt} rotates the whole feet-relative eye
	 * offset — the neck pivot is body-fixed and the head basis rotates with
	 * the body exactly as the renderer applies tilt before the head's own
	 * look rotation.
	 */
	public static EyePair eyesFrom(Vec3 feet, float bodyYawDeg, float headYawDeg, float headPitchDeg,
			FlightBodyTransform tilt, float scale) {
		return computeEyes(feet, bodyYawDeg, headYawDeg, headPitchDeg, 0f, tilt, scale);
	}

	/**
	 * Anchors for a live player: interpolated position and rotations.
	 * {@code headAnimDeg} adds the head channel of the current animation
	 * sample already in rendered/entity terms — x = pitch, y = yaw, z = roll
	 * about the forward axis (see {@code PlayerPoseApplier#renderedRotationDeg});
	 * pass a zero vector while no animation runtime feeds it.
	 */
	public static EyePair eyes(AbstractClientPlayer player, float partial, FlightBodyTransform tilt,
			Vector3f headAnimDeg) {
		Minecraft client = Minecraft.getInstance();
		if (client.getCameraEntity() == player && client.options.getCameraType().isFirstPerson()) {
			Camera camera = client.gameRenderer.getMainCamera();
			Vec3 forward = new Vec3(camera.getLookVector());
			Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
			Vec3 base = camera.getPosition()
					.add(forward.scale(FIRST_PERSON_FORWARD))
					.add(0, -FIRST_PERSON_DOWN, 0);
			return new EyePair(
					base.add(right.scale(-FIRST_PERSON_LATERAL)),
					base.add(right.scale(FIRST_PERSON_LATERAL)));
		}
		Vec3 feet = player.getPosition(partial);
		float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
		if (EmfPresentationOwnership.isOwned(player)
				&& RenderedPoseCache.headPose(player.getId(), HEAD_SCRATCH)) {
			return ownedEyes(feet, bodyYaw, tilt, player.getScale());
		}
		float headYaw = Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot) + headAnimDeg.y;
		float headPitch = Mth.lerp(partial, player.xRotO, player.getXRot()) + headAnimDeg.x;
		return computeEyes(feet, bodyYaw, headYaw, headPitch, headAnimDeg.z, tilt, player.getScale());
	}

	/**
	 * A generic body anchor: {@code lateral}/{@code up}/{@code forward} blocks
	 * off the feet along the body axes (right/up/forward), then tilted by the
	 * flight transform exactly like the eye anchors — used by trail effects
	 * that leave from hands and feet.
	 */
	public static Vec3 tiltedPoint(Vec3 feet, float bodyYawDeg, double lateral, double up,
			double forward, FlightBodyTransform tilt) {
		Vec3 bodyForward = Vec3.directionFromRotation(0f, bodyYawDeg);
		Vec3 bodyRight = rightOf(bodyForward, new Vec3(1, 0, 0));
		Vec3 offset = bodyRight.scale(lateral).add(UP.scale(up)).add(bodyForward.scale(forward));
		return feet.add(applyTilt(offset, bodyRight, bodyForward, tilt));
	}

	private static EyePair computeEyes(Vec3 feet, float bodyYawDeg, float headYawDeg, float headPitchDeg,
			float headRollDeg, FlightBodyTransform tilt, float scale) {
		Vec3 bodyForward = Vec3.directionFromRotation(0f, bodyYawDeg);
		Vec3 bodyRight = rightOf(bodyForward, new Vec3(1, 0, 0));
		Vec3 headForward = Vec3.directionFromRotation(headPitchDeg, headYawDeg);
		Vec3 headRight = rightOf(headForward, bodyRight);
		Vec3 headUp = headRight.cross(headForward).normalize();
		if (headRollDeg != 0f) {
			headRight = rotate(headRight, headForward, headRollDeg);
			headUp = headRight.cross(headForward).normalize();
		}
		Vec3 eyeCenter = new Vec3(0, HEAD_PIVOT, 0)
				.add(headForward.scale(EYE_FORWARD * scale))
				.add(headUp.scale(EYE_UP * scale));
		Vec3 lateral = headRight.scale(EYE_LATERAL * scale);
		return new EyePair(
				feet.add(applyTilt(eyeCenter.subtract(lateral), bodyRight, bodyForward, tilt)),
				feet.add(applyTilt(eyeCenter.add(lateral), bodyRight, bodyForward, tilt)));
	}

	/**
	 * Eye anchors from the cached EMF-posed head transform (HEAD_SCRATCH =
	 * x,y,z px + xRot,yRot,zRot rad). Model-space eye = pivot + Rz·Ry·Rx·local,
	 * mapped to entity space ({@code -x/16, 1.5-y/16, z/16}), then the
	 * renderer's body yaw and flight tilt — identical to the vanilla path when
	 * the cached pose equals the vanilla pose.
	 */
	private static EyePair ownedEyes(Vec3 feet, float bodyYawDeg, FlightBodyTransform tilt, float scale) {
		Vec3 bodyForward = Vec3.directionFromRotation(0f, bodyYawDeg);
		Vec3 bodyRight = rightOf(bodyForward, new Vec3(1, 0, 0));
		Quaternionf headRot = new Quaternionf()
				.rotationZYX(HEAD_SCRATCH[5], HEAD_SCRATCH[4], HEAD_SCRATCH[3]);
		Vector3f pivot = new Vector3f(HEAD_SCRATCH[0], HEAD_SCRATCH[1], HEAD_SCRATCH[2]);
		return new EyePair(
				ownedEye(feet, bodyYawDeg, bodyRight, bodyForward, tilt, scale, pivot, headRot, EYE_LEFT_PX),
				ownedEye(feet, bodyYawDeg, bodyRight, bodyForward, tilt, scale, pivot, headRot, EYE_RIGHT_PX));
	}

	private static Vec3 ownedEye(Vec3 feet, float bodyYawDeg, Vec3 bodyRight, Vec3 bodyForward,
			FlightBodyTransform tilt, float scale, Vector3f pivotPx, Quaternionf headRot, Vector3f eyePx) {
		// Only the eye offset scales — the head pivot stays unscaled (computeEyes parity).
		Vector3f model = new Vector3f(eyePx).mul(scale).rotate(headRot).add(pivotPx);
		Vec3 offset = new Vec3(-model.x / 16.0, HEAD_PIVOT - model.y / 16.0, model.z / 16.0)
				.yRot((float) Math.toRadians(180.0 - bodyYawDeg));
		return feet.add(applyTilt(offset, bodyRight, bodyForward, tilt));
	}

	/** Minecraft's right-hand axis: {@code forward × up} (matches the beam overlays). */
	private static Vec3 rightOf(Vec3 forward, Vec3 fallback) {
		Vec3 right = forward.cross(UP);
		if (right.lengthSqr() < 1e-6) {
			return fallback;
		}
		return right.normalize();
	}

	private static Vec3 applyTilt(Vec3 offset, Vec3 bodyRight, Vec3 bodyForward, FlightBodyTransform tilt) {
		// Positive pitch leans head-first toward body forward; positive roll
		// banks toward the player's right. Roll runs first — the renderer's
		// mulPose(XP·(-pitch))·mulPose(ZP·(-roll)) applies roll to the vector
		// before pitch, so the same order keeps anchors glued to the render.
		if (tilt.rollDeg() != 0f) {
			offset = rotate(offset, bodyForward, tilt.rollDeg());
		}
		if (tilt.pitchDeg() != 0f) {
			offset = rotate(offset, bodyRight, -tilt.pitchDeg());
		}
		return offset;
	}

	/** Rodrigues rotation of {@code v} around a unit {@code axis} by {@code deg} degrees. */
	private static Vec3 rotate(Vec3 v, Vec3 axis, float deg) {
		double rad = Math.toRadians(deg);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double along = axis.dot(v) * (1.0 - cos);
		return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(along));
	}
}
