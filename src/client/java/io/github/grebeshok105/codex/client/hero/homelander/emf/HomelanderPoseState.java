package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player state feeding the {@code superheroes_hl_*} EMF variables: five
 * clip clocks plus the authored-motion weights. Bounded (64 entries, LRU) and
 * zero-allocation on the read path — suppliers look the entity up by uuid and
 * copy floats out.
 *
 * <p>Stage 7 drives the clap: a {@code CLAP} event resets the clip clock and
 * plays it once over 1.92 s while {@code clap_w} and the master {@code hl_w}
 * ease in/out (the jem blends it over the hover/boost group, so it works in
 * flight too); {@code CLAP_CANCEL} releases the weights early. The other clips'
 * weights still read 0 until their stages drive them; their clocks free-run.
 */
final class HomelanderPoseState {

	private static final int MAX_ENTITIES = 64;
	private static final float TICK_SECONDS = 1f / 20f;
	/** Authored HAND CLAP length (windup + hold). */
	private static final float CLAP_SECONDS = 1.92f;
	/** Same ease-in/out as the master weight, so ramp-in and release look symmetric. */
	private static final float CLAP_HALF_LIFE_TICKS = 3f;

	private static final Map<UUID, Entry> STATES = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<UUID, Entry> eldest) {
			return size() > MAX_ENTITIES;
		}
	};

	static {
		ClientSessionState.register(HomelanderPoseState::clearAll);
	}

	private HomelanderPoseState() {
	}

	static final class Entry {
		final HomelanderPoseMath.ClipClock hover = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock takeoff = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock boost = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock clap = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock milk = new HomelanderPoseMath.ClipClock();
		float active;
		float boostWeight;
		float takeoffWeight;
		float clapWeight;
		float milkWeight;
		boolean clapPlaying;
		float forward;
		float strafe;
		float pitchDeg;
		float bankDeg;
	}

	static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			if (!STATES.isEmpty()) {
				STATES.clear();
			}
			return;
		}
		List<AbstractClientPlayer> players = level.players();
		for (int i = 0; i < players.size(); i++) {
			AbstractClientPlayer player = players.get(i);
			if (!EmfPresentationOwnership.isOwned(player)) {
				continue;
			}
			Entry entry = STATES.computeIfAbsent(player.getUUID(), uuid -> new Entry());
			entry.hover.advance(TICK_SECONDS);
			entry.takeoff.advance(TICK_SECONDS);
			entry.boost.advance(TICK_SECONDS);
			if (entry.clapPlaying) {
				entry.clap.advance(TICK_SECONDS);
				if (entry.clap.finished(CLAP_SECONDS)) {
					entry.clapPlaying = false;
				}
			}
			entry.milk.advance(TICK_SECONDS);
			entry.clapWeight = HomelanderPoseMath.approach(entry.clapWeight,
					entry.clapPlaying ? 1f : 0f, CLAP_HALF_LIFE_TICKS, 1f);
			entry.active = HomelanderPoseMath.activeWeight(entry.active, entry.clapPlaying, 1f);
		}
	}

	/** {@code CLAP}: restart the authored clip. Not gated on ownership — a late skin resolve still lands. */
	static void startClap(UUID uuid) {
		Entry entry = STATES.computeIfAbsent(uuid, key -> new Entry());
		entry.clap.reset();
		entry.clapPlaying = true;
	}

	/** {@code CLAP_CANCEL}: the hit was dropped server-side; release the weights early. */
	static void cancelClap(UUID uuid) {
		Entry entry = STATES.get(uuid);
		if (entry != null) {
			entry.clapPlaying = false;
		}
	}

	/** Master presentation weight (0 = vanilla pose). */
	static float weight(UUID uuid) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.active;
	}

	/** Clip-local times, interpolated forward by the render partial tick. */
	static float hoverTime(UUID uuid, float partial) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.hover.time() + partial * TICK_SECONDS;
	}

	static float takeoffTime(UUID uuid, float partial) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.takeoff.time() + partial * TICK_SECONDS;
	}

	static float boostTime(UUID uuid, float partial) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.boost.time() + partial * TICK_SECONDS;
	}

	static float clapTime(UUID uuid, float partial) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.clap.time() + partial * TICK_SECONDS;
	}

	static float milkTime(UUID uuid, float partial) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.milk.time() + partial * TICK_SECONDS;
	}

	static float boostWeight(UUID uuid) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.boostWeight;
	}

	static float takeoffWeight(UUID uuid) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.takeoffWeight;
	}

	static float clapWeight(UUID uuid) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.clapWeight;
	}

	static float milkWeight(UUID uuid) {
		Entry entry = STATES.get(uuid);
		return entry == null ? 0f : entry.milkWeight;
	}

	static void clearAll() {
		STATES.clear();
	}
}
