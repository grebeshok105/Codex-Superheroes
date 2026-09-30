package io.github.grebeshok105.codex.client.core.camera;

import io.github.grebeshok105.codex.client.ClientSessionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Hero-agnostic third-person framing seam (plan §7 stage 6): a presentation
 * registers an {@code offsetProvider} that supplies the world-space offset a
 * detached camera should add to its vanilla eye-position base (Homelander:
 * {@code weight * (bodyCentre - eyePos)}). The offset is smoothed with
 * half-life 3 ticks in world space — one exponential step per client tick,
 * then interpolated by the render partial tick — so framing transitions stay
 * smooth and frame-rate independent while a moving provider target keeps its
 * slight cinematic trail.
 *
 * <p>{@link #offset} is consulted once per frame from {@code Camera.setup}
 * (only inside the detached third-person branch: first-person never reaches
 * the {@code getMaxZoom} invoke it is injected at). {@code null} — no
 * provider, no camera entity, or the provider's weight at 0 — leaves the
 * vanilla base untouched, so standing third-person stays byte-identical and
 * non-focused entities can never be framed. The smoothed state keys on the
 * camera entity's uuid and reseeds when it changes; session reset drops it
 * through {@link ClientSessionState}.
 */
public final class ThirdPersonFraming {
	/** World-space smoothing half-life in ticks (plan §7 stage 6). */
	private static final float HALF_LIFE_TICKS = 3f;

	private static final List<Function<Entity, Vec3>> PROVIDERS = new CopyOnWriteArrayList<>();

	private static boolean tickRegistered;
	private static UUID focusedUuid;
	private static Vec3 smoothedPrev;
	private static Vec3 smoothed;

	static {
		ClientSessionState.register(ThirdPersonFraming::reset);
	}

	private ThirdPersonFraming() {
	}

	/**
	 * Registers a framing provider. The first provider returning a non-null
	 * offset for the camera entity wins; a {@code null} return means
	 * "no framing for this entity" and leaves the camera untouched.
	 */
	public static void register(Function<Entity, Vec3> offsetProvider) {
		PROVIDERS.add(offsetProvider);
		if (!tickRegistered) {
			tickRegistered = true;
			ClientTickEvents.END_CLIENT_TICK.register(ThirdPersonFraming::tick);
		}
	}

	/**
	 * The smoothed framing offset for {@code entity}, or {@code null} when
	 * nothing frames it — the caller keeps the vanilla camera base. Called
	 * at most once per frame for the camera entity.
	 */
	@Nullable
	public static Vec3 offset(Entity entity, float partial) {
		if (entity == null || smoothed == null || !entity.getUUID().equals(focusedUuid)) {
			return null;
		}
		return smoothedPrev.lerp(smoothed, partial);
	}

	private static void tick(Minecraft client) {
		Entity entity = client.getCameraEntity();
		advance(entity == null ? null : entity.getUUID(), target(entity));
	}

	@Nullable
	private static Vec3 target(@Nullable Entity entity) {
		if (entity == null) {
			return null;
		}
		for (Function<Entity, Vec3> provider : PROVIDERS) {
			Vec3 target = provider.apply(entity);
			if (target != null) {
				return target;
			}
		}
		return null;
	}

	/**
	 * One client tick of smoothing: a {@code null} target (no provider, or
	 * the presentation's weight at 0) drops the framing state so the next
	 * sample is identity again; a fresh entity or re-engage seeds straight
	 * to the target so engaging never jumps from a stale value.
	 */
	static void advance(@Nullable UUID uuid, @Nullable Vec3 target) {
		if (uuid == null || target == null) {
			reset();
			return;
		}
		if (!uuid.equals(focusedUuid) || smoothed == null) {
			focusedUuid = uuid;
			smoothedPrev = smoothed = target;
			return;
		}
		smoothedPrev = smoothed;
		float f = 1f - (float) Math.pow(0.5, 1.0 / HALF_LIFE_TICKS);
		smoothed = smoothed.lerp(target, f);
	}

	/**
	 * Smoothed offset interpolated to the render partial tick; {@code null}
	 * while the framing state is empty or belongs to another entity.
	 */
	@Nullable
	static Vec3 sample(UUID uuid, float partial) {
		if (smoothed == null || !uuid.equals(focusedUuid)) {
			return null;
		}
		return smoothedPrev.lerp(smoothed, partial);
	}

	static void reset() {
		focusedUuid = null;
		smoothedPrev = null;
		smoothed = null;
	}
}
