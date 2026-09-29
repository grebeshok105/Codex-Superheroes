package io.github.grebeshok105.codex.client.core.anim;

import io.github.grebeshok105.codex.client.ClientSessionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-entity player-animation state: two layers — {@link Layer#BASE} for sustained poses
 * (loops, held stances) and {@link Layer#ACTION} for one-shot moves on top. ACTION
 * overrides BASE per bone by the ACTION fade weight, so a partially faded action blends
 * between the base pose and its own. Playing a second clip on a layer fades the previous
 * one out while the new one fades in (crossfade over {@code fadeTicks}).
 *
 * <p>Time comes from a {@link TickClock} counting client ticks (20 per second); the
 * default clock advances in {@link #tick()} once per client tick and tests inject their
 * own via {@link #useClock}. Non-loop clips release to BASE when they end (fading out
 * over their {@code fadeTicks}); {@code hold_on_last_frame} clips keep their final pose
 * until {@link #stop}ped. Tick/reset self-register on class load — the same convention
 * {@code ScreenFlash} uses, so no bootstrap wiring is needed.
 */
public final class PlayerAnimator {
	/** Animation layers; ACTION samples on top of BASE. */
	public enum Layer {
		BASE, ACTION
	}

	/** Current client time in ticks (20 ticks = 1 second). */
	@FunctionalInterface
	public interface TickClock {
		float nowTicks();
	}

	private static final float TICKS_PER_SECOND = 20f;
	private static final int MAX_LANE_DEPTH = 8;
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-anim");
	private static final Map<Integer, EnumMap<Layer, Deque<Playing>>> PLAYERS = new ConcurrentHashMap<>();
	private static final Set<ResourceLocation> UNKNOWN_LOGGED = ConcurrentHashMap.newKeySet();

	private static float clientTicks;
	private static final TickClock DEFAULT_CLOCK = () -> clientTicks;
	private static TickClock clock = DEFAULT_CLOCK;

	static {
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		ClientSessionState.register(PlayerAnimator::reset);
	}

	private PlayerAnimator() {
	}

	/** Test seam: replaces the tick clock; {@link #reset()} restores the default. */
	static void useClock(TickClock injected) {
		clock = injected;
	}

	/** Starts {@code clipId} on the entity's layer; unknown ids are logged once and dropped. */
	public static void play(int entityId, ResourceLocation clipId, Layer layer, int fadeTicks) {
		Optional<AnimationClip> clip = AnimationLibrary.get(clipId);
		if (clip.isEmpty()) {
			if (UNKNOWN_LOGGED.add(clipId)) {
				LOGGER.debug("ignoring play for unknown clip {}", clipId);
			}
			return;
		}
		playClip(entityId, clip.get(), layer, fadeTicks);
	}

	/** Starts a resolved clip on the layer, fading the previous entry out over {@code fadeTicks}. */
	static void playClip(int entityId, AnimationClip clip, Layer layer, int fadeTicks) {
		Deque<Playing> lane = PLAYERS
				.computeIfAbsent(entityId, id -> new EnumMap<>(Layer.class))
				.computeIfAbsent(layer, l -> new ArrayDeque<>());
		lane.addLast(new Playing(clip, clock.nowTicks(), Math.max(0, fadeTicks)));
		while (lane.size() > MAX_LANE_DEPTH) {
			lane.removeFirst();
		}
	}

	/**
	 * Fades out only the lane entries playing one of {@code clipIds} over
	 * {@code fadeTicks}; {@code 0} removes them immediately. Sibling clips the
	 * caller does not own keep playing — a lane is shared, so a stop is always
	 * scoped to the clips the caller played.
	 */
	public static void stop(int entityId, Layer layer, int fadeTicks, ResourceLocation... clipIds) {
		EnumMap<Layer, Deque<Playing>> lanes = PLAYERS.get(entityId);
		if (lanes == null) {
			return;
		}
		Deque<Playing> lane = lanes.get(layer);
		if (lane == null || lane.isEmpty()) {
			return;
		}
		Set<ResourceLocation> ids = Set.of(clipIds);
		if (fadeTicks <= 0) {
			lane.removeIf(playing -> ids.contains(playing.clip.id()));
			return;
		}
		float now = clock.nowTicks();
		for (Playing playing : lane) {
			if (ids.contains(playing.clip.id())) {
				playing.beginFadeOut(now, fadeTicks);
			}
		}
	}

	/** The merged BASE + ACTION pose for the entity at {@code nowTicks + partialTick}. */
	public static PoseSample sample(int entityId, float partialTick) {
		EnumMap<Layer, Deque<Playing>> lanes = PLAYERS.get(entityId);
		if (lanes == null) {
			return PoseSample.EMPTY;
		}
		float now = clock.nowTicks() + partialTick;
		PoseSample merged = PoseSample.EMPTY;
		for (Layer layer : Layer.values()) {
			merged = override(merged, sampleLane(lanes.get(layer), now));
		}
		return merged;
	}

	/** Advances the default clock one client tick and releases finished playings. */
	public static void tick() {
		clientTicks += 1f;
		float now = clock.nowTicks();
		for (var it = PLAYERS.entrySet().iterator(); it.hasNext();) {
			EnumMap<Layer, Deque<Playing>> lanes = it.next().getValue();
			lanes.values().forEach(lane -> lane.removeIf(playing -> playing.finished(now)));
			lanes.values().removeIf(Deque::isEmpty);
			if (lanes.isEmpty()) {
				it.remove();
			}
		}
	}

	/** Session reset (disconnect / world leave): drops every playing clip. */
	public static void reset() {
		PLAYERS.clear();
		UNKNOWN_LOGGED.clear();
		clock = DEFAULT_CLOCK;
	}

	private static PoseSample sampleLane(Deque<Playing> lane, float now) {
		if (lane == null) {
			return PoseSample.EMPTY;
		}
		PoseSample merged = PoseSample.EMPTY;
		for (Playing playing : lane) {
			merged = override(merged, playing.sample(now));
		}
		return merged;
	}

	/**
	 * Layers {@code top} over {@code base}: bones the top sample animates blend from the
	 * base contribution toward the top value by the top's fade weight; bones it does not
	 * keep their base pose. The merged weight is the larger of the two, and stored vectors
	 * are divided back down by it so {@link PlayerPoseApplier}'s {@code * weight} lands on
	 * the true per-bone blend.
	 */
	private static PoseSample override(PoseSample base, PoseSample top) {
		if (top.isEmpty() || top.weight() <= 0f) {
			return base;
		}
		float weight = Math.max(base.weight(), top.weight());
		return new PoseSample(
				blend(base.rotationDeg(), base.weight(), top.rotationDeg(), top.weight(), weight),
				blend(base.offsetPx(), base.weight(), top.offsetPx(), top.weight(), weight),
				weight);
	}

	private static Map<String, Vector3f> blend(Map<String, Vector3f> base, float baseWeight,
			Map<String, Vector3f> top, float topWeight, float weight) {
		Map<String, Vector3f> out = new HashMap<>();
		for (Map.Entry<String, Vector3f> entry : base.entrySet()) {
			if (!top.containsKey(entry.getKey())) {
				out.put(entry.getKey(), new Vector3f(entry.getValue()).mul(baseWeight / weight));
			}
		}
		for (Map.Entry<String, Vector3f> entry : top.entrySet()) {
			Vector3f baseContribution = base.containsKey(entry.getKey())
					? new Vector3f(base.get(entry.getKey())).mul(baseWeight)
					: new Vector3f();
			Vector3f v = new Vector3f(entry.getValue())
					.sub(baseContribution)
					.mul(topWeight)
					.add(baseContribution)
					.div(weight);
			out.put(entry.getKey(), v);
		}
		return out;
	}

	/** One clip on a lane: start tick, fade-in ticks, and a pending fade-out. */
	private static final class Playing {
		private final AnimationClip clip;
		private final float startTick;
		private final int fadeTicks;
		private float fadeOutStart = -1f;
		private int fadeOutTicks;

		private Playing(AnimationClip clip, float startTick, int fadeTicks) {
			this.clip = clip;
			this.startTick = startTick;
			this.fadeTicks = fadeTicks;
		}

		private float endTick() {
			return startTick + clip.lengthSeconds() * TICKS_PER_SECOND;
		}

		/** Fade-out begins from the current weight so the transition stays continuous. */
		private void beginFadeOut(float now, int ticks) {
			float weight = weight(now);
			fadeOutTicks = ticks;
			fadeOutStart = now - (1f - weight) * ticks;
		}

		private float weight(float now) {
			if (fadeOutStart >= 0f) {
				return fadeOutTicks > 0
						? Math.max(0f, 1f - (now - fadeOutStart) / fadeOutTicks)
						: 0f;
			}
			if (clip.loop() == AnimationClip.Loop.OFF && now >= endTick()) {
				return fadeTicks > 0
						? Math.max(0f, 1f - (now - endTick()) / fadeTicks)
						: 0f;
			}
			return fadeTicks > 0 ? Math.min(1f, (now - startTick) / fadeTicks) : 1f;
		}

		private boolean finished(float now) {
			if (fadeOutStart >= 0f) {
				return now >= fadeOutStart + fadeOutTicks;
			}
			return clip.loop() == AnimationClip.Loop.OFF && now >= endTick() + fadeTicks;
		}

		private float seconds(float now) {
			float elapsed = Math.max(0f, (now - startTick) / TICKS_PER_SECOND);
			if (clip.loop() == AnimationClip.Loop.WRAP && clip.lengthSeconds() > 0f) {
				return elapsed % clip.lengthSeconds();
			}
			return elapsed;
		}

		private PoseSample sample(float now) {
			float weight = weight(now);
			if (weight <= 0f) {
				return PoseSample.EMPTY;
			}
			float seconds = seconds(now);
			Map<String, Vector3f> rotation = new HashMap<>();
			Map<String, Vector3f> offset = new HashMap<>();
			for (Map.Entry<String, BoneTrack> bone : clip.bones().entrySet()) {
				if (!bone.getValue().rotation().isEmpty()) {
					rotation.put(bone.getKey(), bone.getValue().rotation().sample(seconds));
				}
				if (!bone.getValue().position().isEmpty()) {
					offset.put(bone.getKey(), bone.getValue().position().sample(seconds));
				}
			}
			return new PoseSample(rotation, offset, weight);
		}
	}
}
