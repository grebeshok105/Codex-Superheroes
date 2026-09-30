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
 * <p>Stage 1 registers the variables but never drives the weights, so every
 * weight reads 0 and owned players render the vanilla pose. Clip clocks
 * advance freely so they hold real values once Stage 2 starts animating.
 *
 * <p>Stage 8 drives the milk one-shot: {@link #startMilk} arms it on the
 * MILK_DRINK event, {@link #cancelMilk} on MILK_CANCEL, and the weight
 * returns to 0 the tick the 6.3 s clip finishes.
 */
final class HomelanderPoseState {

	private static final int MAX_ENTITIES = 64;
	private static final float TICK_SECONDS = 1f / 20f;

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
		boolean milkPlaying;
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
			entry.clap.advance(TICK_SECONDS);
			entry.milk.advance(TICK_SECONDS);
			entry.milkWeight = HomelanderPoseMath.oneShotWeight(
					entry.milkPlaying, entry.milk.time(), HomelanderPoseMath.MILK_CLIP_SECONDS);
		}
	}

	/**
	 * MILK_DRINK on an owned player: restart the milk clip clock and arm the
	 * one-shot so {@code superheroes_hl_milk_*} drives the authored sequence.
	 */
	static void startMilk(UUID uuid) {
		Entry entry = STATES.computeIfAbsent(uuid, id -> new Entry());
		entry.milk.reset();
		entry.milkPlaying = true;
	}

	/** MILK_CANCEL (early release): the weight returns to 0 next tick. */
	static void cancelMilk(UUID uuid) {
		Entry entry = STATES.get(uuid);
		if (entry != null) {
			entry.milkPlaying = false;
		}
	}

	/** Master presentation weight (0 = vanilla pose): flight-active or any one-shot up. */
	static float weight(UUID uuid) {
		Entry entry = STATES.get(uuid);
		if (entry == null) {
			return 0f;
		}
		return Math.max(entry.active,
				Math.max(entry.takeoffWeight, Math.max(entry.clapWeight, entry.milkWeight)));
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
