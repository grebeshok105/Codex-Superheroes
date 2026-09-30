package io.github.grebeshok105.codex.client.hero.homelander.emf;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.utils.EMFEntity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-entity runtime for the Homelander EMF model: holds the
 * {@link EmfPlaybackState} (weights + clip times), the smoothed lean values,
 * and pushes them into the entity's EMF {@code var.*} map — the single write
 * point for the whole var contract:
 *
 * <ul>
 *   <li>{@code var.<clip>_w} — blend weight ∈ [0,1]</li>
 *   <li>{@code var.<clip>_t} — clip time in 60-fps frames (frozen while w=0)</li>
 *   <li>{@code var.lean_pitch}, {@code var.lean_roll} — radians on root rx/rz</li>
 *   <li>{@code var.lean_y} — model-pixel offset on root ty</li>
 * </ul>
 *
 * <p>Advance happens per rendered frame via {@link #advanceAndPush} (partial
 * ticks give true 60-fps playback); the flight driver only changes targets.
 * Agents B and D consume this contract through {@code HomelanderPoseApi}.
 */
public final class HomelanderEmfRuntime {
	private static final Map<Integer, HomelanderEmfRuntime> BY_ENTITY = new ConcurrentHashMap<>();
	private static volatile EmfAssets assets = EmfAssets.EMPTY;

	public static void install(EmfAssets assets) {
		HomelanderEmfRuntime.assets = assets == null ? EmfAssets.EMPTY : assets;
		BY_ENTITY.clear();
	}

	public static void clearAll() {
		BY_ENTITY.clear();
	}

	public static void release(int entityId) {
		BY_ENTITY.remove(entityId);
	}

	public static @Nullable HomelanderEmfRuntime peek(int entityId) {
		return BY_ENTITY.get(entityId);
	}

	public static HomelanderEmfRuntime of(Entity entity) {
		return BY_ENTITY.computeIfAbsent(entity.getId(), id -> {
			HomelanderEmfRuntime runtime = new HomelanderEmfRuntime(entity);
			for (EmfClip clip : assets.clips()) {
				runtime.playback.registerClip(clip.varName(), clip.frameCount(), clip.loop());
			}
			return runtime;
		});
	}

	public static @Nullable HomelanderEmfEngine engine() {
		return assets.engine();
	}

	public static Set<String> clipNames() {
		return assets.clipNames();
	}

	/** Whether a compiled EMF engine exists to render. */
	public static boolean available() {
		return assets.engine() != null;
	}

	private final Entity entity;
	private final EmfPlaybackState playback = new EmfPlaybackState();
	/** Same lean half-life as {@code HomelanderFlightLean.Params.defaults()}. */
	private static final double LEAN_HALF_LIFE_TICKS = 3.0;
	private double leanPitchTarget, leanRollTarget, leanYTarget;
	private double leanPitch, leanRoll, leanY;
	private double prevLeanPitch, prevLeanRoll, prevLeanY;
	private float lastPartial = -1f;
	private boolean presenting;

	private HomelanderEmfRuntime(Entity entity) {
		this.entity = entity;
	}

	public EmfPlaybackState playback() {
		return playback;
	}

	/** Driver-side lean targets; smoothed toward in {@link #advanceAndPush}. */
	public void setLeanTargets(double pitchRad, double rollRad, double yPx) {
		this.leanPitchTarget = pitchRad;
		this.leanRollTarget = rollRad;
		this.leanYTarget = yPx;
	}

	/** Total active weight — ~0 means the entity renders as vanilla again. */
	public boolean presenting() {
		return presenting;
	}

	public double lerpLeanPitch(float partial) {
		return prevLeanPitch + (leanPitch - prevLeanPitch) * partial;
	}

	public double lerpLeanRoll(float partial) {
		return prevLeanRoll + (leanRoll - prevLeanRoll) * partial;
	}

	public double lerpLeanY(float partial) {
		return prevLeanY + (leanY - prevLeanY) * partial;
	}

	/**
	 * Advances smoothing + clip times by the elapsed fraction of a game tick
	 * and mirrors everything into the entity var map. Call once per rendered
	 * frame with {@code getGameTimeDeltaPartialTick(true)}.
	 */
	public void advanceAndPush(float partial) {
		float dt = lastPartial < 0f ? 0f : (partial - lastPartial + 1f) % 1f;
		lastPartial = partial;
		playback.advance(dt);
		prevLeanPitch = leanPitch;
		prevLeanRoll = leanRoll;
		prevLeanY = leanY;
		leanPitch = EmfPlaybackState.smoothApproach(leanPitch, leanPitchTarget, dt, LEAN_HALF_LIFE_TICKS);
		leanRoll = EmfPlaybackState.smoothApproach(leanRoll, leanRollTarget, dt, LEAN_HALF_LIFE_TICKS);
		leanY = EmfPlaybackState.smoothApproach(leanY, leanYTarget, dt, LEAN_HALF_LIFE_TICKS);
		pushVars();
	}

	/** Writes the current values without advancing (target changes, start). */
	public void pushVars() {
		if (!(entity instanceof EMFEntity emfEntity)) {
			return;
		}
		Map<String, Float> vars = emfEntity.emf$getVariableMap();
		boolean active = false;
		for (String clip : assets.clipNames()) {
			float w = playback.weight(clip);
			vars.put(EmfExpressionBuilder.weightVarName(clip), w);
			vars.put(EmfExpressionBuilder.timeVarName(clip), playback.time(clip));
			active |= w > 1e-3f;
		}
		vars.put(EmfExpressionBuilder.LEAN_PITCH_VAR, (float) leanPitch);
		vars.put(EmfExpressionBuilder.LEAN_ROLL_VAR, (float) leanRoll);
		vars.put(EmfExpressionBuilder.LEAN_Y_VAR, (float) leanY);
		this.presenting = active;
	}
}
