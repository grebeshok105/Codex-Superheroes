package io.github.grebeshok105.codex.client.hero.regulus.emf;

import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player clip clocks and blend weights for Regulus's authored EMF
 * presentation — the client-side twin of the wire {@code regulus/anim/*}
 * events. Entries are created lazily per EMF-owned player and LRU-bounded.
 * Everything here is client-tick driven (the {@code tick(Minecraft)}
 * registered on {@code END_CLIENT_TICK}); render reads add the partial so
 * clip time interpolates smoothly — a client-side clock that never trusts
 * wall time.
 *
 * <p>Clip selection: one-shot cast clips are event-driven
 * ({@code start*}) and take priority; between casts the pose is the
 * combat idle, swapped for the evangelium idle while the synced madness
 * window is open ({@link RegulusMadnessState} is synced to tracking
 * clients, so remote Regulus players get the same selection).
 */
final class RegulusPoseState {
	private static final int MAX_ENTRIES = 64;

	/** Loop clip blend windows in ticks (authored blend_in/out seconds × 20). */
	private static final float COMBAT_IN = 3.6f, COMBAT_OUT = 2.4f;
	private static final float EV_IDLE_IN = 4.4f, EV_IDLE_OUT = 3.2f;
	private static final float LOOP_SECONDS = 4f;

	private static final Map<UUID, Entry> STATES = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<UUID, Entry> eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	private static @Nullable UUID lastUuid;
	private static @Nullable Entry lastEntry;

	static {
		ClientSessionState.register(RegulusPoseState::clearAll);
	}

	private RegulusPoseState() {
	}

	/** One-shot clip metadata: authored duration and blend windows in ticks. */
	enum Clip {
		LION_HEART_ACTIVATION(1.6f, 2.0f, 3.2f),
		LION_ROAR(2.6f, 2.4f, 3.6f),
		MANIA_OF_GREED_CAST(2.1f, 2.4f, 3.6f),
		GREEDS_EMBRACE_CAST(2.2f, 2.4f, 3.6f),
		COUNTER_ATTACK(0.9f, 1.2f, 2.0f),
		EVANGELIUM_ACTIVATION(3.4f, 3.6f, 4.4f),
		EVANGELIUM_DEACTIVATION(2.0f, 3.2f, 3.6f),
		DEBRIS_KICK(2.2f, 2.2f, 3.4f);

		final float duration;
		final float blendInTicks;
		final float blendOutTicks;

		Clip(float duration, float blendInTicks, float blendOutTicks) {
			this.duration = duration;
			this.blendInTicks = blendInTicks;
			this.blendOutTicks = blendOutTicks;
		}
	}

	/** The per-clip runtime: its clock plus the blend weight driving the jem gate. */
	private static final class OneShot {
		private final Clip clip;
		private final RegulusPoseMath.ClipClock clock = new RegulusPoseMath.ClipClock();
		private float weight;

		private OneShot(Clip clip) {
			this.clip = clip;
		}

		private void start() {
			clock.start();
		}

		private void tick() {
			clock.advance(RegulusPoseMath.TICK_SECONDS, clip.duration);
			weight = RegulusPoseMath.blend(weight,
					clock.playing() ? 1f : 0f, clip.blendInTicks, clip.blendOutTicks);
		}
	}

	static final class Entry {
		private float masterWeight;
		private float combatWeight;
		private float evangeliumIdleWeight;
		private final RegulusPoseMath.ClipClock combatIdle = RegulusPoseMath.ClipClock.running();
		private final RegulusPoseMath.ClipClock evangeliumIdle = RegulusPoseMath.ClipClock.running();
		private final EnumMap<Clip, OneShot> oneShots = new EnumMap<>(Clip.class);

		Entry() {
			for (Clip clip : Clip.values()) {
				oneShots.put(clip, new OneShot(clip));
			}
		}

		void tick(boolean madness) {
			masterWeight = RegulusPoseMath.approach(masterWeight, 1f,
					RegulusPoseMath.ACTIVE_HALF_LIFE_TICKS, 1f);
			combatIdle.advance(RegulusPoseMath.TICK_SECONDS, LOOP_SECONDS);
			evangeliumIdle.advance(RegulusPoseMath.TICK_SECONDS, LOOP_SECONDS);
			combatWeight = RegulusPoseMath.blend(combatWeight, madness ? 0f : 1f,
					COMBAT_IN, COMBAT_OUT);
			evangeliumIdleWeight = RegulusPoseMath.blend(evangeliumIdleWeight, madness ? 1f : 0f,
					EV_IDLE_IN, EV_IDLE_OUT);
			for (OneShot oneShot : oneShots.values()) {
				oneShot.tick();
			}
		}
	}

	// -- tick ---------------------------------------------------------------

	static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		for (Player player : level.players()) {
			if (!(player instanceof net.minecraft.client.player.AbstractClientPlayer p)
					|| !EmfPresentationOwnership.isOwned(p)) {
				continue;
			}
			boolean madness = p.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT).madness();
			entryOf(p.getUUID(), true).tick(madness);
		}
	}

	// -- clip start events ---------------------------------------------------

	static void startLionHeartActivation(UUID uuid) {
		start(uuid, Clip.LION_HEART_ACTIVATION);
	}

	static void startLionRoar(UUID uuid) {
		start(uuid, Clip.LION_ROAR);
	}

	static void startManiaCast(UUID uuid) {
		start(uuid, Clip.MANIA_OF_GREED_CAST);
	}

	static void startEmbraceCast(UUID uuid) {
		start(uuid, Clip.GREEDS_EMBRACE_CAST);
	}

	static void startCounterAttack(UUID uuid) {
		start(uuid, Clip.COUNTER_ATTACK);
	}

	static void startEvangeliumActivation(UUID uuid) {
		start(uuid, Clip.EVANGELIUM_ACTIVATION);
	}

	static void startEvangeliumDeactivation(UUID uuid) {
		start(uuid, Clip.EVANGELIUM_DEACTIVATION);
	}

	static void startDebrisKick(UUID uuid) {
		start(uuid, Clip.DEBRIS_KICK);
	}

	private static void start(@Nullable UUID uuid, Clip clip) {
		if (uuid == null) {
			return;
		}
		entryOf(uuid, true).oneShots.get(clip).start();
	}

	// -- variable readers ----------------------------------------------------

	/** Master weight — the {@code superheroes_rg_w} jem gate. */
	static float weight(@Nullable UUID uuid, float partial) {
		Entry entry = entryOf(uuid, false);
		return entry == null ? 0f : entry.masterWeight;
	}

	static float combatIdleWeight(@Nullable UUID uuid, float partial) {
		Entry entry = entryOf(uuid, false);
		return entry == null ? 0f : entry.combatWeight;
	}

	static float evangeliumIdleWeight(@Nullable UUID uuid, float partial) {
		Entry entry = entryOf(uuid, false);
		return entry == null ? 0f : entry.evangeliumIdleWeight;
	}

	static float combatIdleTime(@Nullable UUID uuid, float partial) {
		Entry entry = entryOf(uuid, false);
		return entry == null ? 0f
				: RegulusPoseMath.loopTime(entry.combatIdle.time()
				+ partial * RegulusPoseMath.TICK_SECONDS, LOOP_SECONDS);
	}

	static float evangeliumIdleTime(@Nullable UUID uuid, float partial) {
		Entry entry = entryOf(uuid, false);
		return entry == null ? 0f
				: RegulusPoseMath.loopTime(entry.evangeliumIdle.time()
				+ partial * RegulusPoseMath.TICK_SECONDS, LOOP_SECONDS);
	}

	static float oneShotTime(@Nullable UUID uuid, Clip clip, float partial) {
		Entry entry = entryOf(uuid, false);
		if (entry == null) {
			return 0f;
		}
		return entry.oneShots.get(clip).clock.time()
				+ (entry.oneShots.get(clip).clock.playing()
				? partial * RegulusPoseMath.TICK_SECONDS : 0f);
	}

	static float oneShotWeight(@Nullable UUID uuid, Clip clip, float partial) {
		Entry entry = entryOf(uuid, false);
		return entry == null ? 0f : entry.oneShots.get(clip).weight;
	}

	// -- bookkeeping ---------------------------------------------------------

	static Entry entryOf(@Nullable UUID uuid, boolean create) {
		if (uuid == null) {
			return null;
		}
		if (uuid.equals(lastUuid) && lastEntry != null) {
			return lastEntry;
		}
		Entry entry = STATES.get(uuid);
		if (entry == null && create) {
			entry = new Entry();
			STATES.put(uuid, entry);
		}
		lastUuid = uuid;
		lastEntry = entry;
		return entry;
	}

	static void clearAll() {
		STATES.clear();
		lastUuid = null;
		lastEntry = null;
	}
}
